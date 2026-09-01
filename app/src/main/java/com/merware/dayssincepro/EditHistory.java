package com.merware.dayssincepro;

import java.util.Calendar;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.merware.dayssincepro.SimpleDate.DateStyle;

public class EditHistory extends AppCompatActivity {

    SharedPreferences preferences;
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
            setTheme(R.style.DatePickerHostThemeDark);
        } else {// 0 light
            setTheme(R.style.DatePickerHostThemeLight);
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
            long initial = DatePickerSupport.utcMillis(mYear, mMonth, mDay);
            MaterialDatePicker<Long> picker = DatePickerSupport.newPicker(initial);
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = DatePickerSupport.toUtcCalendar(selection);
                mYear = cal.get(Calendar.YEAR);
                mMonth = cal.get(Calendar.MONTH);
                mDay = cal.get(Calendar.DAY_OF_MONTH);
                updateDisplay();
            });
            picker.show(getSupportFragmentManager(), "historyDatePicker");
        }
    };

    SimpleDate chosenDate;

    public void updateDisplay() {

        String systemDateFormat = DateFormat.GetSystemDateFormat(this);

        String usDate = DatePickerSupport.isoDateString(mYear, mMonth, mDay);
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

            String dateText = DatePickerSupport.isoDateString(mYear, mMonth, mDay);

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
