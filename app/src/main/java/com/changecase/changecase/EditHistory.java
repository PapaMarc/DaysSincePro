package com.merware.dayssincepro;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.MerWare.DaysSincePro.SimpleDate.DateStyle;

public class EditHistory extends Activity {

    SharedPreferences preferences;
    static final int DATE_DIALOG_ID = 0;
    private int mYear;
    private int mMonth;
    private int mDay;
    TextView dateText;
    CheckBox onTimeCheckbox;
    TextView explainText;
    long historyId;
    EditText editTextNotes;

    String mode;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String sTheme = preferences.getString("theme", "0");
        int theme = Integer.parseInt(sTheme);

        if (theme == 1) { // dark
            setTheme(R.style.AppDialogTheme2);
        } else {// 0 light
            setTheme(R.style.AppDialogTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_history);

        Intent intent = getIntent();

        historyId = intent.getLongExtra("historyId", 0);

        mode = intent.getStringExtra("mode");
        String event = intent.getStringExtra("event");

        dateText = (TextView) findViewById(R.id.dateText);
        explainText = (TextView) findViewById(R.id.ago_future);
        onTimeCheckbox = (CheckBox) findViewById(R.id.checkBox1);
        editTextNotes = (EditText) findViewById(R.id.editText1);
        Button cancelButton = (Button) findViewById(R.id.eventCancel);
        Button btnPickDate = (Button) findViewById(R.id.buttonPickDate);

        int onTime = intent.getIntExtra("onTime", 1);
        if (onTime == 1) {
            onTimeCheckbox.setChecked(true);
        }

        if (mode.equals("Add")) {
            setTitle(getString(R.string.add_occur) + " " + event);
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

        } else {
            setTitle(getString(R.string.edit) + " " + event);

            String date = intent.getStringExtra("date");
            SimpleDate sd = new SimpleDate(date);

            mMonth = sd.getMonth() - 1;
            mDay = sd.getDay();
            mYear = sd.getYear();

            editTextNotes.setText(intent.getStringExtra("note"));
        }

        cancelButton.setOnClickListener(historyCancel);

        btnPickDate.setOnClickListener(dateDialogListener);

        onTimeCheckbox.setOnClickListener(checkListener);

        Button okButton = (Button) findViewById(R.id.eventOK);
        okButton.setOnClickListener(eventOK);

        updateDisplay();

    }

    private OnClickListener historyCancel = new OnClickListener() {
        public void onClick(View v) {

            setResult(RESULT_CANCELED, null);
            finish();
        }
    };

    private OnClickListener dateDialogListener = new OnClickListener() {
        public void onClick(View v) {
            showDialog(DATE_DIALOG_ID);
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this, R.style.AccentDialogTheme, mDateSetListener,
                        mYear, mMonth, mDay);
        }
        return null;
    }

    // the call-back received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            updateDisplay();
        }
    };

    SimpleDate chosenDate;

    public void updateDisplay() {

        String systemDateFormat = DateFormat.GetSystemDateFormat(this);

        int month = mMonth + 1;
        String usDate = mYear + "-" + month + "-" + mDay;
        SimpleDate sd = new SimpleDate(usDate, SimpleDate.DateStyle.US);

        chosenDate = sd;

        if (systemDateFormat.equals(getString(R.string.uk_date_style))) {
            dateText.setText(sd.getDate2(DateStyle.UK));
        }
        else if (systemDateFormat.equals("Year-Month-Day"))
        {
            dateText.setText(sd.getDate2(DateStyle.US));
        }
        else
            dateText.setText(sd.getDate2(DateStyle.MYD));

        DaysSinceCalculations dsc = new DaysSinceCalculations(usDate);
        explainText.setText(dsc.getExplain(true, 0));

    }

    boolean isOnTime = true;
    private OnClickListener checkListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            isOnTime = onTimeCheckbox.isChecked();
        }
    };

    private OnClickListener eventOK = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent();

            String dateText = mYear + "-";
            int month = mMonth + 1;
            int day = mDay;

            // pad 0
            if (month < 10)
                dateText = dateText + "0";

            dateText = dateText + month + "-";

            if (day < 10)
                dateText = dateText + "0";

            dateText = dateText + day;

            intent.putExtra("date", dateText);
            intent.putExtra("onTime", isOnTime);
            intent.putExtra("historyId", historyId);
            intent.putExtra("onTime", isOnTime);

            //Log.wtf("edit", editTextNotes.getText().toString());
            //Log.wtf("edit", "on time ma? " + isOnTime);

            intent.putExtra("note", editTextNotes.getText().toString());

            setResult(RESULT_OK, intent);
            finish();
        }
    };

    // don't restart when phone change orientation.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

}
