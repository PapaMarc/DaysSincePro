package com.merware.dayssincepro;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.merware.dayssincepro.SimpleDate.DateStyle;

public class EditEventActivity extends AppCompatActivity {

    private static final int MAX_DETAILS_LENGTH = 256;

    EditText eventText;
    EditText detailsText;

    TextView dateText;
    TextView endDateText;
    TextView recurTextView;
    TextView notifyAtView;
    Spinner catSpinner;
    CheckBox checkbox;
    CheckBox cbEndDay;
    TextView explainText;
    SelectAgainSpinner recurSpinner;
    Button btnPickEndDate;

    private long categoryID;
    private long eventID;
    protected SQLiteDatabase db;

    private String mode;
    static final int TIME_DIALOG_ID = 1;

    private int mYear;
    private int mMonth;
    private int mDay;

    private int mEndYear;
    private int mEndMonth;
    private int mEndDay;

    private long nRecur = 0; // database
    private String sRecur = "0";
    int iRecur = 0; // one time event - default
    private int selectCount = 0;

    private int notifyHour;
    private int notifyMinute;

    SharedPreferences preferences;

    int theme = 0;

    ArrayList<Long> listCatId = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String sTheme = preferences.getString("theme", "0");
        theme = Integer.parseInt(sTheme);


        if (theme == 1) { // dark
            setTheme(R.style.DatePickerHostThemeDark);
        } else {// 0 light
            setTheme(R.style.DatePickerHostThemeLight);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_event);

        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        eventText = (EditText) findViewById(R.id.editEvent);
        detailsText = (EditText) findViewById(R.id.editDetails);
        Button btnPickDate = (Button) findViewById(R.id.buttonPickDate);
        btnPickDate.setOnClickListener(dateDialogListener);

        btnPickEndDate = (Button) findViewById(R.id.buttonPickEndDate);
        btnPickEndDate.setOnClickListener(endDateDialogListener);

        dateText = (TextView) findViewById(R.id.dateText);
        recurTextView = (TextView) findViewById(R.id.recur);
        endDateText = (TextView) findViewById(R.id.endDateText);

        notifyAtView = (TextView) findViewById(R.id.notify_at);
        Button btnPickNotify = (Button) findViewById(R.id.buttonPickRecur);
        btnPickNotify.setOnClickListener(timeDialogListener);

        // if notify not specified, don't even show option.

        boolean optionNotify = preferences.getBoolean("noti", false);

        if (!optionNotify) {
            notifyAtView.setVisibility(View.GONE);
            btnPickNotify.setVisibility(View.GONE);
        }

        Button okButton = (Button) findViewById(R.id.eventOK);
        okButton.setOnClickListener(eventOK);

        Button cancelButton = (Button) findViewById(R.id.eventCancel);
        cancelButton.setOnClickListener(eventCancel);

        catSpinner = (Spinner) findViewById(R.id.catSpinner);
        recurSpinner = (SelectAgainSpinner) findViewById(R.id.recur_spinner);
        checkbox = (CheckBox) findViewById(R.id.checkBox1);
        checkbox.setOnClickListener(checkListener);

        cbEndDay = (CheckBox) findViewById(R.id.checkBoxEndDate);
        cbEndDay.setOnClickListener(cbEndDayListener);

        explainText = (TextView) findViewById(R.id.ago_future);

        Intent intent = getIntent();
        categoryID = intent.getLongExtra("catId", 0);

        // showToast("gotten cat id is " + categoryID);

        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                this, R.array.recur, android.R.layout.simple_spinner_item);


        if (theme == 1) { // dark - use a custom view so that the options are visible
            adapter1.setDropDownViewResource(R.layout.spinner_dropdown_item);
        }
        else {
            adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        recurSpinner.setAdapter(adapter1);
        recurSpinner.setOnItemSelectedListener(new RecurListener());

        mode = intent.getStringExtra("mode");
        if (mode.equals("Add")) {
            setTitle(R.string.add_event);
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            notifyHour = 8;
            notifyMinute = 0;

            showEndDateFields(false);

        } else {
            String event = intent.getStringExtra("event");
            eventText.setText(event);
            String details = intent.getStringExtra("details");
            if (details != null) {
                detailsText.setText(details);
            }
            String date = intent.getStringExtra("date");
            SimpleDate sd = new SimpleDate(date);

            mMonth = sd.getMonth() - 1;
            mDay = sd.getDay();
            mYear = sd.getYear();

            sRecur = intent.getStringExtra("recur");
            iRecur = Integer.parseInt(sRecur);

            // showToast("nRecur is " + nRecur);

            switch (iRecur) {
                case 0: // one time
                    recurSpinner.setSelection(0);
                    break;
                case 7: // weekly
                    recurSpinner.setSelection(1);
                    break;
                case 14: // biweekly
                    recurSpinner.setSelection(2);
                    break;
                case 30:
                    recurSpinner.setSelection(3);
                    break;
                case 90: // quarter
                    recurSpinner.setSelection(4);
                    break;
                case 180: // semi annually
                    recurSpinner.setSelection(5);
                    break;
                case 365:
                    recurSpinner.setSelection(6);
                    break;
                default:
                    selectCount--; // anti effect
                    recurSpinner.setSelection(7);
            }

            eventID = intent.getLongExtra("id", 0);

            String endDate= intent.getStringExtra("end_date");
            if (endDate == null)
            {
                showEndDateFields(false);
            }
            else
            {
                SimpleDate sdEnd = new SimpleDate(endDate);
                mEndMonth = sdEnd.getMonth() - 1;
                mEndDay = sdEnd.getDay();
                mEndYear = sdEnd.getYear();

                cbEndDay.setVisibility(View.VISIBLE);

            }

        }

        String end_date = intent.getStringExtra("end_date");

        if (end_date == null) {
            cbEndDay.setChecked(false);
        }
        else {
            cbEndDay.setChecked(true);
        }

        listCategories();
        updateDisplay();

        eventText.setHint(R.string.enter_text);

        if (mode.equals("Add")) {
            eventText.requestFocus();
            eventText.post(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(eventText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }
    }



    private OnClickListener eventOK = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent();
            ContentValues values = new ContentValues();

            String sEvent = eventText.getText().toString();
            String sDetails = normalizeDetails(detailsText.getText().toString());

            values.put("event", sEvent);
            values.put("details", sDetails);
            values.put("recur", nRecur);

            categoryID = 0;

            String dateText = DatePickerSupport.isoDateString(mYear, mMonth, mDay);
            values.put("date", dateText);

            String endDateText = null;

            if (!cbEndDay.isChecked()) {
                mEndYear = 0;
                mEndMonth = 0;
                mEndDay = 0;
            }

            // ------------------------------------
            if ( mEndMonth == 0 && mEndDay == 0 && mEndYear == 0)
            {
                // no end date is specified
            }
            else {

                endDateText = DatePickerSupport.isoDateString(mEndYear, mEndMonth, mEndDay);
                values.put("end_date", endDateText);
            }

            // showToast("old categoryID is " + categoryID);

            if (!checkbox.isChecked()) {
                categoryID = 0;
            } else {

                if (listCatId.size() > 0)
                    categoryID = listCatId.get(catSpinner.getSelectedItemPosition());
            }

            //showToast("categoryID is " + categoryID);

            values.put("catId", categoryID); // -------------

           // Log.wtf("add", "event " + values.get("event"));
           // Log.wtf("add", "recur " + values.get("recur"));
          //  Log.wtf("add", "date  " + values.get("date"));
          //  Log.wtf("add", "catId" + values.get("catId"));


            if (mode.equals("Add")) {
                eventID = db.insert("event", "event", values);

            //    Log.wtf("add", "after insert eventId is " + eventID);

                String APP_NAME = getString(R.string.app_name);
                Preferences.storePreferenceInt(EditEventActivity.this,
                        APP_NAME, "notify_hour_" + eventID, notifyHour);
                Preferences.storePreferenceInt(EditEventActivity.this,
                        APP_NAME, "notify_minute_" + eventID, notifyMinute);
            }

            // determine if date is future

            Calendar eventDate = Calendar.getInstance();
            Calendar nowDate = Calendar.getInstance();

            eventDate.set(mYear, mMonth, mDay);

            if (eventDate.after(nowDate)) {
                intent.putExtra("future", true);
            } else {
                intent.putExtra("future", false);
            }

            // put field data back.
            intent.putExtra("event", sEvent);
            intent.putExtra("details", sDetails);
            intent.putExtra("id", eventID);
            intent.putExtra("date", dateText);
            intent.putExtra("nRecur", nRecur);
            // showToast("putting back " + categoryID);

            // showToast("putting back " + notifyMinute);

            intent.putExtra("catId", categoryID);
            intent.putExtra("notifyHour", notifyHour);
            intent.putExtra("notifyMinute", notifyMinute);

            intent.putExtra("end_date", endDateText);

            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private OnClickListener eventCancel = new OnClickListener() {
        public void onClick(View v) {

            setResult(RESULT_CANCELED, null);
            finish();
        }
    };

    private OnClickListener dateDialogListener = new OnClickListener() {
        public void onClick(View v) {
            long initial = DatePickerSupport.utcMillis(mYear, mMonth, mDay);
            MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(initial);
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = DatePickerSupport.toUtcCalendar(selection);
                mYear = cal.get(Calendar.YEAR);
                mMonth = cal.get(Calendar.MONTH);
                mDay = cal.get(Calendar.DAY_OF_MONTH);
                updateDisplay();
            });
            picker.show(getSupportFragmentManager(), "startDatePicker");
        }
    };

    private OnClickListener endDateDialogListener = new OnClickListener() {
        public void onClick(View v) {
            openEndDatePicker(false);
        }
    };

    private void openEndDatePicker(boolean fromEndDayCheckbox) {
        long initial;

        if (mEndMonth == 0 && mEndDay == 0 && mEndYear == 0) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, mDay);
            cal.set(Calendar.MONTH, mMonth);
            cal.set(Calendar.YEAR, mYear);

            switch (iRecur) {
                case 0: // one time; use today
                    cal = Calendar.getInstance();
                    break;
                case 7: // weekly
                    cal.add(Calendar.DAY_OF_MONTH, 7);
                    break;
                case 14: // biweekly
                    cal.add(Calendar.DAY_OF_MONTH, 14);
                    break;
                case 30:
                    cal.add(Calendar.DAY_OF_MONTH, 30);
                    break;
                case 90: // quarter
                    cal.add(Calendar.MONTH, 3);
                    break;
                case 180: // semi annually
                    cal.add(Calendar.MONTH, 6);
                    break;
                case 365:
                    cal.add(Calendar.YEAR, 1);
                    break;
                default:
            }

            initial = DatePickerSupport.utcMillis(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        } else {
            initial = DatePickerSupport.utcMillis(mEndYear, mEndMonth, mEndDay);
        }

        MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(initial);
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = DatePickerSupport.toUtcCalendar(selection);
            mEndYear = cal.get(Calendar.YEAR);
            mEndMonth = cal.get(Calendar.MONTH);
            mEndDay = cal.get(Calendar.DAY_OF_MONTH);

            SimpleDate eventDate = new SimpleDate(mYear, mMonth, mDay);
            SimpleDate endDate = new SimpleDate(mEndYear, mEndMonth, mEndDay);

            if (endDate.getDate().before(eventDate.getDate())) {
                showToast("End date should not be earlier than the Date");
                if (fromEndDayCheckbox) {
                    cbEndDay.setChecked(false);
                    mEndYear = 0;
                    mEndMonth = 0;
                    mEndDay = 0;
                    showEndDateFields(false);
                }
            } else {
                cbEndDay.setChecked(true);
                updateDisplay();
                showEndDateFields(true);
            }
        });

        if (fromEndDayCheckbox) {
            picker.addOnNegativeButtonClickListener(view -> {
                cbEndDay.setChecked(false);
                mEndYear = 0;
                mEndMonth = 0;
                mEndDay = 0;
                showEndDateFields(false);
            });
            picker.addOnCancelListener(dialog -> {
                cbEndDay.setChecked(false);
                mEndYear = 0;
                mEndMonth = 0;
                mEndDay = 0;
                showEndDateFields(false);
            });
        }

        picker.show(getSupportFragmentManager(), "endDatePicker");
    }

    private OnClickListener timeDialogListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            showDialog(TIME_DIALOG_ID);
        }
    };

    public void setRecurText(String value) {

        try {
            iRecur = Integer.valueOf(value);
        }
        catch (NumberFormatException nfe)
        {
            iRecur = 0;
        }
        sRecur = value;

        if (value.equals("0") || value.equals("")) {
            recurTextView.setText("");
            nRecur = 0;
        } else {

            String text = getString(R.string.recurs_in) + " " + value + " " + getString(R.string.days) + ".";

            recurTextView.setText(text);
            nRecur = Long.parseLong(value);
        }
    }

    void recurCallback() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.recurrence);
        alert.setMessage(R.string.days_recur);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        alert.setView(input);

        input.setText(String.valueOf(iRecur));
        input.setSelection(sRecur.length());

        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();

                if (value.toString().equals("0")) {
                    recurSpinner.setSelection(0);
                } else {
                    setRecurText(value.toString());
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

        alert.show();
    }

    public void updateDisplay() {

        String systemDateFormat = DateFormat.GetSystemDateFormat(this);

        String usDate = DatePickerSupport.isoDateString(mYear, mMonth, mDay);
        SimpleDate sd = new SimpleDate(usDate, SimpleDate.DateStyle.US);

        if (systemDateFormat.equals(getString(R.string.uk_date_style))) {
            dateText.setText(sd.getDate2(DateStyle.UK));
        }
        else {
            dateText.setText(sd.getDate2(DateStyle.US));
        }

        if (mEndYear == 0 && mEndMonth == 0 && mEndDay == 0) {
            endDateText.setText( getString(R.string.no_end_date_specified));
        }
        else {
            String usEndDate = DatePickerSupport.isoDateString(mEndYear, mEndMonth, mEndDay);
            sd = new SimpleDate(usEndDate, SimpleDate.DateStyle.US);

            if (systemDateFormat.equals(getString(R.string.uk_date_style))) {
                endDateText.setText(sd.getDate2(DateStyle.UK));
            } else {
                endDateText.setText(sd.getDate2(DateStyle.US));
            }
        }

        DaysSinceCalculations dsc = new DaysSinceCalculations(usDate);
        explainText.setText(dsc.getExplain(true, 0));

        // notify time
        if (eventID != 0) {

            notifyHour = Preferences.getPreferenceInt(EditEventActivity.this,
                    getString(R.string.app_name), "notify_hour_" + eventID);

            notifyMinute = Preferences.getPreferenceInt(EditEventActivity.this,
                    getString(R.string.app_name), "notify_minute_" + eventID);

            // showToast("got from preference" + notifyHour + ":" + notifyMinute);
        }

        // showToast("updateDisplay: from pref hour " + notifyHour + " minute " + notifyMinute + " for ID " + eventID);

        String text = "Notify at " + formatHourMinute(notifyHour, notifyMinute);
        notifyAtView.setText(text);

    }

    // the call-back received when the user "sets" the date in the dialog

    private String formatHourMinute(int hour, int minute) {
        // format display
        String am_pm = "AM";
        String min = "" + minute;

        if (hour == 0)
            hour = 12;
        else if (hour == 12) {
            am_pm = "PM";
        }
        else if (hour >= 13) {
            am_pm = "PM";
            hour -= 12;
        }

        if (minute < 10) {
            min = "0" + minute;
        }

        //showToast("OK, look:" + hour + ":" + min + " " + am_pm);

        return hour + ":" + min + " " + am_pm;
    }

    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            // showToast(" on time set " + hourOfDay + ":" + minute);
            // store eventID with time in preference

            String APP_NAME = getString(R.string.app_name);
            Preferences.storePreferenceInt(EditEventActivity.this, APP_NAME,
                    "notify_hour_" + eventID, hourOfDay);
            Preferences.storePreferenceInt(EditEventActivity.this, APP_NAME,
                    "notify_minute_" + eventID, minute);

            String text = "Notify at " + formatHourMinute(hourOfDay, minute);
            notifyAtView.setText(text);

            notifyHour = hourOfDay;
            notifyMinute = minute;

            // updateDisplay();
        }
    };

    static String normalizeDetails(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.length() == 0) {
            return null;
        }
        if (normalized.length() > MAX_DETAILS_LENGTH) {
            normalized = normalized.substring(0, MAX_DETAILS_LENGTH);
        }
        return normalized;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this, mTimeSetListener, notifyHour,
                        notifyMinute, false);
        }
        return null;
    }

    // don't restart when phone change orientation.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private long getUncategorizedEventCount() {
        Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM event WHERE catId = ?",
                new String[]{String.valueOf(CategorySelectionPolicy.UNCATEGORIZED_CAT_ID)});
        try {
            if (countCursor.moveToFirst()) {
                return countCursor.getLong(0);
            }
            return 0;
        } finally {
            countCursor.close();
        }
    }

    private void listCategories() {

        String option = preferences.getString("category_sort_order", "0");
        int iOption = Integer.parseInt(option);
        String orderBy = null;

        switch (iOption) {
            case 0:
                orderBy = null;
                break;
            case 1:
                orderBy = "category ASC";
                break;
            case 2:
                orderBy = "category DESC";
                break;
        }

        // _id is required for SimpleCursorAdapter
        Cursor categoryCursor = db.query("category",
                new String[] { "_id", "category" }, null, null, null, null,
                orderBy);

        Cursor cursor = categoryCursor;
        if (CategorySelectionPolicy.shouldIncludeSyntheticUncategorized(getUncategorizedEventCount())) {
            MatrixCursor synthetic = new MatrixCursor(new String[]{"_id", "category"});
            synthetic.addRow(new Object[]{
                CategorySelectionPolicy.UNCATEGORIZED_CAT_ID,
                CategorySelectionPolicy.getUncategorizedDisplayLabel()
            });
            cursor = new MergeCursor(new Cursor[]{synthetic, categoryCursor});
        }

        String[] from = new String[] { "category" };
        int[] to = new int[] { android.R.id.text1 };

        catSpinner.setEnabled(false);

        int totalCategories = cursor.getCount();

        // showToast("count is " + cursor.getCount());

        if (totalCategories == 0) {
            checkbox.setEnabled(false);
            checkbox.setVisibility(View.GONE);
            catSpinner.setVisibility(View.GONE);
        }

        SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, cursor, from, to);

        // set layout for activated adapter

        if (theme == 1) { // dark - use a custom view so that the options are visible
            sca.setDropDownViewResource(R.layout.spinner_dropdown_item);
        }
        else {
            sca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        catSpinner.setAdapter(sca);

        // set spinner
        cursor.moveToFirst();
        listCatId.clear();

        int setPosition = 0;
        boolean gotPosition = false;

        for (int i = 0; i < totalCategories; i++) {

            long catId = Long.parseLong(cursor.getString(0));

            listCatId.add(catId);

            if (catId == categoryID) {
                setPosition = i;
                gotPosition = true;

            }
            cursor.moveToNext();
        }

        // if there are categories, add a new one will pick the first category
        // listed.
        // if add to uncategorized don't bother to pick category even if there
        // are some.
        if (mode.equals("Add")) {

            // showToast("total Categories : " + totalCategories + " catID " + categoryID);

            if (totalCategories > 0 && categoryID != 0) {
                gotPosition = true;
                // setPosition = 0; // need to find position given categoryID
            }
        }

        if (gotPosition) {
            catSpinner.setSelection(setPosition);
            catSpinner.setEnabled(true);
            checkbox.setEnabled(true);
            checkbox.setChecked(true);
        }

        startManagingCursor(cursor);
    }

    private OnClickListener checkListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (catSpinner.isEnabled())
                catSpinner.setEnabled(false);
            else
                catSpinner.setEnabled(true);
        }
    };


    private OnClickListener cbEndDayListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (cbEndDay.isChecked()) {
                showEndDateFields(false);
                openEndDatePicker(true);
            }
            else {
                mEndYear = 0;
                mEndMonth = 0;
                mEndDay = 0;
                showEndDateFields(false);
            }
        }
    };
    private void showEndDateFields(boolean bShow)
    {
        if (bShow)
        {
            btnPickEndDate.setVisibility(View.VISIBLE);
            endDateText.setVisibility(View.VISIBLE);
        }
        else {
            btnPickEndDate.setVisibility(View.GONE);
            endDateText.setVisibility(View.GONE);
        }
    }

    public class RecurListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

            // showToast("on recur selected " + pos + " select count " + selectCount);
            selectCount++;

            cbEndDay.setVisibility(View.VISIBLE);

            switch (pos) {
                case 0:
                    setRecurText("0");
                    break;
                case 1:
                    setRecurText("7");
                    break;
                case 2:
                    setRecurText("14");
                    break;
                case 3:
                    setRecurText("30");
                    break;
                case 4:
                    setRecurText("90");
                    break;
                case 5:
                    setRecurText("180");
                    break;
                case 6:
                    setRecurText("365");
                    break;
                case 7:
                    if (selectCount > 0)
                        recurCallback();

                    setRecurText(Integer.toString(iRecur));
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }

    }
}
