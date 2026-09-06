package com.merware.dayssincepro;

import java.util.Calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

public class DaysDiffActivity extends AppCompatActivity {

    private int mYear;
    private int mMonth;
    private int mDay;
    TextView dateText;
    TextView dateText2;
    TextView daysText;
    TextView explainText;

    String click_choose;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String themeValue = ThemeMode.getThemeValue(this);
        setTheme(ThemeMode.miniAEventThemeResId(themeValue));

        super.onCreate(savedInstanceState);
        click_choose = getString(R.string.click_choose);

        setContentView(R.layout.days_diff);
        EdgeToEdgeUtil.applyContentInsets(this);
        TopBarHelper.setupCenteredBackToolbar(this, R.id.mini_b_toolbar, R.string.days_diff);

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
            long initial = DatePickerSupport.utcMillis(mYear, mMonth, mDay);
            MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(DaysDiffActivity.this, initial);
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = DatePickerSupport.toUtcCalendar(selection);
                mYear = cal.get(Calendar.YEAR);
                mMonth = cal.get(Calendar.MONTH);
                mDay = cal.get(Calendar.DAY_OF_MONTH);

                usDate1 = DatePickerSupport.isoDateString(mYear, mMonth, mDay);
                updateDisplay(usDate1, dateText);
            });
            picker.show(getSupportFragmentManager(), "datePickerA");
        }
    };

    private OnClickListener buttonBCallback = new OnClickListener() {
        public void onClick(View v) {
            long initial = DatePickerSupport.utcMillis(mYear, mMonth, mDay);
            MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(DaysDiffActivity.this, initial);
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = DatePickerSupport.toUtcCalendar(selection);
                mYear = cal.get(Calendar.YEAR);
                mMonth = cal.get(Calendar.MONTH);
                mDay = cal.get(Calendar.DAY_OF_MONTH);

                usDate2 = DatePickerSupport.isoDateString(mYear, mMonth, mDay);
                updateDisplay(usDate2, dateText2);
            });
            picker.show(getSupportFragmentManager(), "datePickerB");
        }
    };

    String usDate1 = click_choose;

    String usDate2 = click_choose;

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
