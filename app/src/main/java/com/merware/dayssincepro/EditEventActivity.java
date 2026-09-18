package com.merware.dayssincepro;

import java.util.ArrayList;
import java.util.Calendar;

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
import android.view.MotionEvent;
import android.view.LayoutInflater;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.merware.dayssincepro.SimpleDate.DateStyle;

public class EditEventActivity extends AppCompatActivity {

    private static final int MAX_DETAILS_LENGTH = 256;
    private static final int REQUEST_ADD_EVENT_CATEGORY = 201;
    public static final String EXTRA_CATEGORY_CREATED_INLINE_DURING_ADD_FLOW =
            "extra_category_created_inline_during_add_flow";

    EditText eventText;
    EditText detailsText;

    TextView dateText;
    TextView endDateText;
    TextView recurTextView;
    TextView notifyAtView;
    TextView leadDaysEffectiveView;
    TextView leadDaysSourceView;
    TextView globalReminderDisabledHintView;
    View reminderSettingsGroup;
    SelectAgainSpinner catSpinner;
    CheckBox cbEndDay;
    CheckBox eventNotifyEnabledCheckbox;
    TextView explainText;
    TextView categoryNudgeText;
    TextView reminderSummaryValueView;
    SelectAgainSpinner recurSpinner;
    Button btnPickEndDate;
    Button btnPickNotify;
    Button buttonEditNotifyLeadDays;
    Button buttonAddCategory;
    ScrollView formScrollView;

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
    private Integer customNotifyLeadDays;
    private boolean eventNotifyEnabled = true;
    private boolean globalNotificationsEnabled = false;
    private AlertDialog reminderEditorDialog;

    SharedPreferences preferences;

    int theme = 0;

    ArrayList<Long> listCatId = new ArrayList<>();
    private boolean isBindingCategorySpinner = false;
    private int lastPersistableSpinnerPosition = -1;
    private long inlineCreatedCategoryIdDuringAddFlow = -1L;
    private boolean isAddFlowCategorySelectionRequired = false;
    private boolean hasExplicitCategorySelectionAction = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String themeValue = ThemeMode.getThemeValue(this);
        theme = ThemeMode.isDark(themeValue) ? 1 : 0;
        setTheme(ThemeMode.miniAEventThemeResId(themeValue));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_event);
        EdgeToEdgeUtil.applyContentInsets(this);
        TopBarHelper.setupCenteredBackToolbar(this, R.id.mini_a_toolbar, 0);

        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        eventText = (EditText) findViewById(R.id.editEvent);
        detailsText = (EditText) findViewById(R.id.editDetails);
        formScrollView = (ScrollView) findViewById(R.id.scrollView1);
        Button btnPickDate = (Button) findViewById(R.id.buttonPickDate);
        btnPickDate.setOnClickListener(dateDialogListener);

        btnPickEndDate = (Button) findViewById(R.id.buttonPickEndDate);
        btnPickEndDate.setOnClickListener(endDateDialogListener);

        dateText = (TextView) findViewById(R.id.dateText);
        recurTextView = (TextView) findViewById(R.id.recur);
        endDateText = (TextView) findViewById(R.id.endDateText);

        reminderSummaryValueView = (TextView) findViewById(R.id.reminderSummaryValue);
        View reminderSummaryRow = findViewById(R.id.reminderSummaryRow);
        reminderSummaryRow.setOnClickListener(v -> {
            dismissKeyboardAndClearFocus();
            showReminderEditor();
        });

        // if notify not specified, don't even show option.

        boolean optionNotify = preferences.getBoolean("noti", false);
        globalNotificationsEnabled = optionNotify;

        Button okButton = (Button) findViewById(R.id.eventOK);
        okButton.setOnClickListener(eventOK);

        Button cancelButton = (Button) findViewById(R.id.eventCancel);
        cancelButton.setOnClickListener(eventCancel);

        catSpinner = (SelectAgainSpinner) findViewById(R.id.catSpinner);
        catSpinner.setOnItemSelectedListener(categorySelectionListener);
        buttonAddCategory = (Button) findViewById(R.id.buttonAddCategory);
        buttonAddCategory.setOnClickListener(addCategoryClickListener);
        recurSpinner = (SelectAgainSpinner) findViewById(R.id.recur_spinner);
        View.OnTouchListener hideKeyboardTouchListener = (view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                dismissKeyboardAndClearFocus();
            }
            return false;
        };
        catSpinner.setOnTouchListener(hideKeyboardTouchListener);
        recurSpinner.setOnTouchListener(hideKeyboardTouchListener);
        categoryNudgeText = (TextView) findViewById(R.id.categoryNudgeText);

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
        isAddFlowCategorySelectionRequired = "Add".equals(mode)
            && CategorySelectionPolicy.shouldDefaultToAddNewCategoryAction(true, categoryID);
        hasExplicitCategorySelectionAction = !isAddFlowCategorySelectionRequired;
        if (mode.equals("Add")) {
            setTitle(R.string.add_event);
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            customNotifyLeadDays = null;
            eventNotifyEnabled = true;

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
            if (intent.hasExtra("notify_lead_days")) {
                int extraLeadDays = intent.getIntExtra("notify_lead_days", -1);
                customNotifyLeadDays = extraLeadDays >= 0 ? extraLeadDays : null;
            } else {
                customNotifyLeadDays = null;
            }
            eventNotifyEnabled = intent.getBooleanExtra("notify_enabled", true);

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
        updateCategoryNudgeVisibility();
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

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED, null);
        finish();
        return true;
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
            values.put("notify_enabled", eventNotifyEnabled ? 1 : 0);

            if (customNotifyLeadDays == null) {
                values.putNull("notify_lead_days");
            } else {
                values.put("notify_lead_days", customNotifyLeadDays);
            }

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

            if ("Add".equals(mode)) {
                if (listCatId.isEmpty()) {
                    showToast(getString(R.string.choose_or_create_category));
                    return;
                }
                if (isAddFlowCategorySelectionRequired && !hasExplicitCategorySelectionAction) {
                    showToast(getString(R.string.choose_or_create_category));
                    return;
                }
            }

            if (listCatId.size() > 0) {
                long selectedCategoryId = listCatId.get(catSpinner.getSelectedItemPosition());
                categoryID = selectedCategoryId;
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
            intent.putExtra("notify_enabled", eventNotifyEnabled);
            intent.putExtra("notify_lead_days",
                    customNotifyLeadDays == null ? -1 : customNotifyLeadDays);
                boolean usedInlineCreatedCategory = "Add".equals(mode)
                    && inlineCreatedCategoryIdDuringAddFlow > 0
                    && inlineCreatedCategoryIdDuringAddFlow == categoryID;
                intent.putExtra(EXTRA_CATEGORY_CREATED_INLINE_DURING_ADD_FLOW, usedInlineCreatedCategory);

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
            dismissKeyboardAndClearFocus();
            long initial = DatePickerSupport.utcMillis(mYear, mMonth, mDay);
            MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(EditEventActivity.this, initial);
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
            dismissKeyboardAndClearFocus();
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

        MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(EditEventActivity.this, initial);
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = DatePickerSupport.toUtcCalendar(selection);
            mEndYear = cal.get(Calendar.YEAR);
            mEndMonth = cal.get(Calendar.MONTH);
            mEndDay = cal.get(Calendar.DAY_OF_MONTH);

            SimpleDate eventDate = new SimpleDate(mYear, mMonth, mDay);
            SimpleDate endDate = new SimpleDate(mEndYear, mEndMonth, mEndDay);

            if (endDate.getDate().before(eventDate.getDate())) {
                showToast(getString(R.string.end_date_before_start_date));
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
            dismissKeyboardAndClearFocus();
            if (!globalNotificationsEnabled) {
                return;
            }
            showDialog(TIME_DIALOG_ID);
        }
    };

    private OnClickListener notifyLeadDaysDialogListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissKeyboardAndClearFocus();
            showNotifyLeadDaysDialog();
        }
    };

    private void showNotifyLeadDaysDialog() {
        if (!globalNotificationsEnabled) {
            return;
        }

        AlertDialog.Builder alert = DialogThemeHelper.themedBuilder(this);
        alert.setTitle(R.string.notify_lead_days_title);
        alert.setMessage(getString(R.string.notify_lead_days_prompt));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (customNotifyLeadDays != null) {
            input.setText(String.valueOf(customNotifyLeadDays));
            input.setSelection(input.getText().length());
        }
        alert.setView(input);

        alert.setPositiveButton(R.string.notify_lead_days_set_custom, (dialog, whichButton) -> {
            Integer parsed = parseNotifyLeadDaysOrNull(input.getText().toString());
            if (parsed == null) {
                showToast(getString(R.string.notify_lead_days_invalid));
                return;
            }
            customNotifyLeadDays = parsed;
            updateReminderStateViews();
        });

        alert.setNeutralButton(R.string.notify_lead_days_use_default, (dialog, whichButton) -> {
            customNotifyLeadDays = null;
            updateReminderStateViews();
        });

        alert.setNegativeButton(R.string.Cancel, (dialog, whichButton) -> {
            // no-op; dialog open/cancel must not mutate persisted or in-memory lead days
        });

        alert.show();
    }

    private void showReminderEditor() {
        if (reminderEditorDialog != null && reminderEditorDialog.isShowing()) {
            return;
        }

        View editorView = LayoutInflater.from(this).inflate(
                R.layout.reminder_editor_dialog, null, false);
        notifyAtView = (TextView) editorView.findViewById(R.id.notify_at);
        btnPickNotify = (Button) editorView.findViewById(R.id.buttonPickRecur);
        btnPickNotify.setOnClickListener(timeDialogListener);
        leadDaysEffectiveView = (TextView) editorView.findViewById(R.id.notify_lead_days_effective);
        leadDaysSourceView = (TextView) editorView.findViewById(R.id.notify_lead_days_source);
        globalReminderDisabledHintView = (TextView) editorView.findViewById(R.id.notify_global_disabled_hint);
        reminderSettingsGroup = editorView.findViewById(R.id.reminder_editor_content);
        eventNotifyEnabledCheckbox = (CheckBox) editorView.findViewById(R.id.eventNotifyEnabledCheckbox);
        eventNotifyEnabledCheckbox.setOnClickListener(v -> {
            eventNotifyEnabled = eventNotifyEnabledCheckbox.isChecked();
            updateReminderStateViews();
        });
        buttonEditNotifyLeadDays = (Button) editorView.findViewById(R.id.buttonEditNotifyLeadDays);
        buttonEditNotifyLeadDays.setOnClickListener(notifyLeadDaysDialogListener);

        reminderEditorDialog = DialogThemeHelper.themedBuilder(this)
                .setTitle(R.string.reminders)
                .setView(editorView)
                .setNegativeButton(R.string.Cancel, null)
                .create();
        reminderEditorDialog.setOnDismissListener(dialog -> reminderEditorDialog = null);
        updateReminderStateViews();
        reminderEditorDialog.show();
    }

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

        AlertDialog.Builder alert = DialogThemeHelper.themedBuilder(this);

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

        DaysSinceCalculations dsc = new DaysSinceCalculations(this, usDate);
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

        String text = formatNotifyAtText(notifyHour, notifyMinute);
        if (notifyAtView != null) {
            notifyAtView.setText(text);
        }

        updateReminderStateViews();

    }

    private void updateReminderStateViews() {
        if (eventNotifyEnabledCheckbox != null) {
            eventNotifyEnabledCheckbox.setChecked(eventNotifyEnabled);
        }

        ReminderLeadDaysResolver.Resolution resolution =
                ReminderLeadDaysResolver.resolve(nRecur, customNotifyLeadDays);

        if (leadDaysEffectiveView != null) {
            leadDaysEffectiveView.setText(getResources().getQuantityString(
                R.plurals.notify_lead_days_prior_summary,
                resolution.effectiveLeadDays,
                resolution.effectiveLeadDays));
        }

        if (leadDaysSourceView != null) {
            leadDaysSourceView.setText(resolution.custom
                    ? getString(R.string.lead_days_source_custom)
                    : getString(R.string.lead_days_source_default_from_recurrence));
            leadDaysSourceView.setEnabled(globalNotificationsEnabled);
            leadDaysSourceView.setAlpha(globalNotificationsEnabled ? 1.0f : 0.60f);
        }

        if (eventNotifyEnabledCheckbox != null) {
            eventNotifyEnabledCheckbox.setText(eventNotifyEnabled
                    ? getString(R.string.event_notifications_enabled)
                    : getString(R.string.event_notifications_disabled));
            eventNotifyEnabledCheckbox.setEnabled(globalNotificationsEnabled);
            eventNotifyEnabledCheckbox.setAlpha(globalNotificationsEnabled ? 1.0f : 0.60f);
        }

        if (buttonEditNotifyLeadDays != null) {
            buttonEditNotifyLeadDays.setEnabled(globalNotificationsEnabled);
            buttonEditNotifyLeadDays.setAlpha(globalNotificationsEnabled ? 1.0f : 0.60f);
        }

        if (notifyAtView != null) {
            notifyAtView.setEnabled(globalNotificationsEnabled);
            notifyAtView.setAlpha(globalNotificationsEnabled ? 1.0f : 0.60f);
        }

        if (btnPickNotify != null) {
            btnPickNotify.setEnabled(globalNotificationsEnabled);
            btnPickNotify.setAlpha(globalNotificationsEnabled ? 1.0f : 0.60f);
        }

        if (leadDaysEffectiveView != null) {
            leadDaysEffectiveView.setEnabled(globalNotificationsEnabled);
            leadDaysEffectiveView.setAlpha(globalNotificationsEnabled ? 1.0f : 0.60f);
        }

        if (globalReminderDisabledHintView != null) {
            globalReminderDisabledHintView.setVisibility(globalNotificationsEnabled
                    ? View.GONE
                    : View.VISIBLE);
            globalReminderDisabledHintView.setAlpha(0.85f);
        }

        if (reminderSettingsGroup != null) {
            reminderSettingsGroup.setAlpha(globalNotificationsEnabled ? 1.0f : 0.92f);
        }

        if (reminderSummaryValueView != null) {
            ReminderLeadDaysResolver.Resolution summaryResolution =
                    ReminderLeadDaysResolver.resolve(nRecur, customNotifyLeadDays);
            if (!eventNotifyEnabled) {
                reminderSummaryValueView.setText(R.string.reminders_off);
            } else {
                reminderSummaryValueView.setText(getResources().getQuantityString(
                        R.plurals.reminders_on_summary,
                        summaryResolution.effectiveLeadDays,
                        formatHourMinute(notifyHour, notifyMinute),
                        summaryResolution.effectiveLeadDays));
            }
        }
    }

    static Integer parseNotifyLeadDaysOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() == 0) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(trimmed);
            if (parsed < 0 || parsed > 30) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
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

    private String formatNotifyAtText(int hour, int minute) {
        return getString(R.string.notify_at_time_format, formatHourMinute(hour, minute));
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

            String text = formatNotifyAtText(hourOfDay, minute);
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

    private long getRealCategoryCount() {
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM category", null);
        try {
            if (countCursor.moveToFirst()) {
                return countCursor.getLong(0);
            }
            return 0;
        } finally {
            countCursor.close();
        }
    }

    private void updateCategoryNudgeVisibility() {
        if (categoryNudgeText == null) {
            return;
        }

        boolean show = "Add".equals(mode)
                && CategorySelectionPolicy.shouldShowCategoryCreationNudge(getRealCategoryCount());
        categoryNudgeText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private int findFirstPersistableSpinnerPosition() {
        for (int i = 0; i < listCatId.size(); i++) {
            long id = listCatId.get(i);
            if (CategorySelectionPolicy.isPersistableCategoryId(id)) {
                return i;
            }
        }
        return -1;
    }

    private int findSpinnerPositionByCategoryId(long targetCategoryId) {
        for (int i = 0; i < listCatId.size(); i++) {
            if (listCatId.get(i) == targetCategoryId) {
                return i;
            }
        }
        return -1;
    }

    private void launchAddCategoryFromPicker() {
        hasExplicitCategorySelectionAction = true;
        Intent intent = new Intent(this, CreateCategoryActivity.class);
        startActivityForResult(intent, REQUEST_ADD_EVENT_CATEGORY);
    }

    private OnClickListener addCategoryClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissKeyboardAndClearFocus();
            launchAddCategoryFromPicker();
        }
    };

    private AdapterView.OnItemSelectedListener categorySelectionListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isBindingCategorySpinner) {
                        return;
                    }

                    if (position < 0 || position >= listCatId.size()) {
                        return;
                    }

                    long selectedId = listCatId.get(position);
                    if ("Add".equals(mode) && isAddFlowCategorySelectionRequired) {
                        hasExplicitCategorySelectionAction = true;
                    }

                    if (CategorySelectionPolicy.isPersistableCategoryId(selectedId)) {
                        lastPersistableSpinnerPosition = position;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };

    private void listCategories() {
        isBindingCategorySpinner = true;

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
                CategorySelectionPolicy.getUncategorizedDisplayLabel(this)
            });
            cursor = new MergeCursor(new Cursor[]{synthetic, cursor});
        }

        String[] from = new String[] { "category" };
        int[] to = new int[] { android.R.id.text1 };

        catSpinner.setEnabled(false);

        int totalCategories = cursor.getCount();

        // showToast("count is " + cursor.getCount());

        if (totalCategories == 0) {
            catSpinner.setEnabled(false);
            if (categoryNudgeText != null) {
                categoryNudgeText.setVisibility(View.GONE);
            }
        } else {
            catSpinner.setVisibility(View.VISIBLE);
            catSpinner.setEnabled(true);
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

        int fallbackPersistable = findFirstPersistableSpinnerPosition();
        if (fallbackPersistable >= 0) {
            lastPersistableSpinnerPosition = fallbackPersistable;
        }

        if (!gotPosition && totalCategories > 0) {
            if (fallbackPersistable >= 0) {
                setPosition = fallbackPersistable;
            } else {
                setPosition = 0;
            }
            gotPosition = true;
        }

        if (gotPosition) {
            catSpinner.setSelection(setPosition);
            catSpinner.setEnabled(true);
            if (CategorySelectionPolicy.isPersistableCategoryId(listCatId.get(setPosition))) {
                lastPersistableSpinnerPosition = setPosition;
            }
        }

        startManagingCursor(cursor);
        isBindingCategorySpinner = false;
    }

    private OnClickListener cbEndDayListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            dismissKeyboardAndClearFocus();
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

            updateReminderStateViews();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_ADD_EVENT_CATEGORY) {
            return;
        }

        isBindingCategorySpinner = false;
        dismissKeyboardAndClearFocus();

        long createdCategoryId = -1L;
        if (data != null) {
            createdCategoryId = data.getLongExtra(CreateCategoryActivity.EXTRA_CREATED_CATEGORY_ID, -1L);
        }

        if (createdCategoryId > 0) {
            categoryID = createdCategoryId;
            inlineCreatedCategoryIdDuringAddFlow = createdCategoryId;
            isAddFlowCategorySelectionRequired = false;
            hasExplicitCategorySelectionAction = true;
        }

        listCategories();
        updateCategoryNudgeVisibility();
        ensureTopOfFormVisible();
    }

    private void dismissKeyboardAndClearFocus() {
        View focused = getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
            focused.clearFocus();
        }
    }

    private void ensureTopOfFormVisible() {
        if (formScrollView == null) {
            return;
        }
        formScrollView.post(new Runnable() {
            @Override
            public void run() {
                formScrollView.smoothScrollTo(0, 0);
            }
        });
    }
}
