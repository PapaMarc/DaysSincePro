package com.merware.dayssincepro;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

public class DaysDiffActivity extends Activity {

    SharedPreferences preferences;
    static final int DATE_DIALOG_ID1 = 1;
    static final int DATE_DIALOG_ID2 = 2;

    private int mYear;
    private int mMonth;
    private int mDay;
    TextView dateText;
    TextView dateText2;
    TextView daysText;
    TextView explainText;

    String click_choose;
    int theme = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String stheme = preferences.getString("theme", "0");
        theme = Integer.parseInt(stheme);

        if (theme == 1) { // dark
            setTheme(R.style.AppDialogTheme2);
        } else {// 0 light
            setTheme(R.style.AppDialogTheme);
        }
        super.onCreate(savedInstanceState);
        click_choose = getString(R.string.click_choose);

        setContentView(R.layout.days_diff);

        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        dateText = (TextView) findViewById(R.id.dateText);
        dateText2 = (TextView) findViewById(R.id.dateText2);
        daysText = (TextView) findViewById(R.id.daysDiffText);
        explainText = (TextView) findViewById(R.id.daysExplainText);

        Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(doneDiff);

        Button buttonA = (Button) findViewById(R.id.buttonPickDateA);
        Button buttonB = (Button) findViewById(R.id.buttonPickDateB);
        buttonA.setOnClickListener(buttonACallback);
        buttonB.setOnClickListener(buttonBCallback);

        Button buttonEventA = (Button) findViewById(R.id.buttonEventA);
        Button buttonEventB = (Button) findViewById(R.id.buttonEventB);

        buttonEventA.setOnClickListener(eventACallback);
        buttonEventB.setOnClickListener(eventBCallback);

    }

    private OnClickListener doneDiff = new OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    private OnClickListener buttonACallback = new OnClickListener() {
        public void onClick(View v) {
            chooseDialog(DATE_DIALOG_ID1);
        }
    };

    private OnClickListener buttonBCallback = new OnClickListener() {
        public void onClick(View v) {
            chooseDialog(DATE_DIALOG_ID2);
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID1:
                return new DatePickerDialog(this, R.style.AccentDialogTheme, mDateSetListener1,
                        mYear, mMonth, mDay);
            case DATE_DIALOG_ID2:
                return new DatePickerDialog(this, R.style.AccentDialogTheme, mDateSetListener2,
                        mYear, mMonth, mDay);
        }
        return null;
    }

    private void chooseDialog(int key) {
        showDialog(key);
    }

    String usDate1 = click_choose;
    private DatePickerDialog.OnDateSetListener mDateSetListener1 = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;

            int month = mMonth + 1;
            usDate1 = mYear + "-" + month + "-" + mDay;
            updateDisplay(usDate1, dateText);
        }
    };

    String usDate2 = click_choose;
    private DatePickerDialog.OnDateSetListener mDateSetListener2 = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;

            int month = mMonth + 1;
            usDate2 = mYear + "-" + month + "-" + mDay;

            updateDisplay(usDate2, dateText2);
        }
    };

    private static final int EVENT_A_ACTIVITY = 0;
    private static final int EVENT_B_ACTIVITY = 1;

    private OnClickListener eventACallback = new OnClickListener() {

        @Override
        public void onClick(View v) {

            Intent intent = new Intent(DaysDiffActivity.this,
                    EventChooserActivity.class);
            startActivityForResult(intent, EVENT_A_ACTIVITY);
        }
    };

    private OnClickListener eventBCallback = new OnClickListener() {

        @Override
        public void onClick(View v) {

            Intent intent = new Intent(DaysDiffActivity.this,
                    EventChooserActivity.class);
            startActivityForResult(intent, EVENT_B_ACTIVITY);

        }
    };

    private void updateDisplay(String usDate, TextView tv, String eventExplained) {
        updateDisplay(usDate, tv);

        tv.setText(eventExplained);
    }

    private void updateDisplay(String usDate, TextView tv) {

        String systemDateFormat = DateFormat.GetSystemDateFormat(this);

        SimpleDate sd = new SimpleDate(usDate, SimpleDate.DateStyle.US);

        if (systemDateFormat.equals(getString(R.string.uk_date_style))) {
            tv.setText(sd.getDate2(SimpleDate.DateStyle.UK));
        } else {
            tv.setText(sd.getDate2(SimpleDate.DateStyle.US));
        }


        if ((dateText.getText() != click_choose)
                && (dateText2.getText() != click_choose)) {

            DaysSinceCalculations dsc = new DaysSinceCalculations(usDate1, usDate2);

            long daysSince = Math.abs(dsc.getDaysSinceEvent());

            if (daysSince == 1)
                daysText.setText(daysSince + " " + getString(R.string.day));
            else {
                daysText.setText(daysSince + " " + getString(R.string.days));

                if (daysSince > 31)
                    explainText.setText("(" + dsc.toString() + ")");
            }

        }
    }

    void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String eventDate;
        String str;

        switch (requestCode) {
            case EVENT_A_ACTIVITY:
                if (resultCode == RESULT_OK) {

                    eventDate = data.getStringExtra("eventDate");
                    str = data.getStringExtra("event") + ": "
                            + data.getStringExtra("eventDateText");

                    dateText.setText(str);
                    usDate1 = eventDate;

                    updateDisplay(usDate1, dateText, str);
                }
                break;
            case EVENT_B_ACTIVITY:
                if (resultCode == RESULT_OK) {

                    eventDate = data.getStringExtra("eventDate");
                    str = data.getStringExtra("event") + ": "
                            + data.getStringExtra("eventDateText");

                    dateText2.setText(str);
                    usDate2 = eventDate;
                    updateDisplay(usDate2, dateText2, str);
                }
                break;
        }

    }

}
