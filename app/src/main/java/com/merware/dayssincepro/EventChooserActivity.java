package com.merware.dayssincepro;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Alex on 5/14/2015.
 */
public class EventChooserActivity extends Activity {

    SharedPreferences preferences;
    protected SQLiteDatabase db;

    Spinner spinnerS; // sort order
    Spinner spinnerC; // category

    Spinner spinnerE; // event
    String usDate1 = "";
    TextView descText;

    ArrayList<Integer> listOfCatIds = new ArrayList<>();

    ArrayList<String> listOfDates = new ArrayList<>();

    ArrayAdapter<CharSequence> sortAdapter;
    ArrayAdapter<CharSequence> catAdapter;
    ArrayAdapter<CharSequence> eventAdapter;

    String orderBy = null;
    String orderByColumn = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String stheme = preferences.getString("theme", "0");
        int theme = Integer.parseInt(stheme);

        if (theme == 1) { // dark
            setTheme(R.style.AppDialogTheme2);
        } else {// 0 light
            setTheme(R.style.AppDialogTheme);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.event_chooser);

        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(doneDialog);

        sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.cat_days_between, android.R.layout.simple_spinner_item);

        if (theme == 1) { // dark - use a custom view so that the options are visible
            sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        }
        else {
            sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        eventAdapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);


        if (theme == 1) { // dark - use a custom view so that the options are visible
            eventAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        }
        else {
            eventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        spinnerS = (Spinner) this.findViewById(R.id.spinnerS);
        spinnerC = (Spinner) this.findViewById(R.id.spinnerC);
        spinnerE = (Spinner) this.findViewById(R.id.spinnerE);

        descText = (TextView) findViewById(R.id.descText);

        spinnerE.setAdapter(eventAdapter);

        catAdapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);

        if (theme == 1) { // dark - use a custom view so that the options are visible
            catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        }
        else {
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }



        spinnerC.setAdapter(catAdapter);
        spinnerS.setAdapter(sortAdapter);

        spinnerS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                                       View selectedItemView, int position, long id) {

                switch (position) {
                    case 0:
                        orderBy = "date ASC";  // rabi guy request
                        orderByColumn = null;
                        break;
                    case 1:
                        orderBy = "event ASC";
                        orderByColumn = "category ASC";
                        break;
                    case 2:
                        orderBy = "event DESC";
                        orderByColumn = "category DESC";
                        break;
                }

                setCatDropDown(orderByColumn);

                if (listOfCatIds.size() == 0) {
                    // showToast("There are no categories");
                    spinnerC.setEnabled(false);
                    setEventDropDown(-1, orderBy);

                } else {
                    int catId = listOfCatIds.get(0); // first thing in category
                    setEventDropDown(catId, orderBy);
                }

                updateDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        spinnerS.setSelection(0); // 0, chronological;  1 alphabetize

        spinnerC.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                                       View selectedItemView, int position, long id) {

                int catId = listOfCatIds.get(position);
                setEventDropDown(catId, orderBy);
                updateDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        spinnerE.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                                       View selectedItemView, int position, long id) {
                usDate1 = listOfDates.get(position);
                updateDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
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

    private void setCatDropDown(String orderBy) {

        try {
            listOfCatIds = new ArrayList<Integer>();
            catAdapter.clear();
            catAdapter.notifyDataSetChanged();

            if (CategorySelectionPolicy.shouldIncludeSyntheticUncategorized(getUncategorizedEventCount())) {
                listOfCatIds.add((int) CategorySelectionPolicy.UNCATEGORIZED_CAT_ID);
                catAdapter.add(CategorySelectionPolicy.getUncategorizedDisplayLabel());
            }

            String sql = "select _id, category from category order by "
                    + orderBy;
            Cursor cursor = db.rawQuery(sql, null);

            // looping through all rows and adding to list
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {

                int id = cursor.getInt(0);
                String category = cursor.getString(1);

                listOfCatIds.add(new Integer(id));

                catAdapter.add(category);
                cursor.moveToNext();

                startManagingCursor(cursor);

            }
        } catch (Exception e) {
            // showToast("wut?" + e.getMessage());
            Log.wtf("PROB", e.getMessage());
        }

        catAdapter.notifyDataSetChanged();
        eventAdapter.notifyDataSetChanged();

        spinnerC.setSelection(0);
        spinnerE.setSelection(0);

    }

    // based on work long done in ConfigWidgetActivity
    // get all event

    private void setEventDropDown(int catId, String orderBy) {
        try {
            eventAdapter.clear();
            listOfDates = new ArrayList<>();
            eventAdapter.notifyDataSetChanged();

            String sql;

            sql = "select event, date from event where catId = " + catId
                    + " order by " + orderBy;

            if (catId == -1) {
                sql = "select event, date from event order by " + orderBy;
            }

            Cursor cursor = db.rawQuery(sql, null);

            // looping through all rows and adding to list
            cursor.moveToFirst();

            for (int i = 0; i < cursor.getCount(); i++) {

                String event = cursor.getString(0);
                String date = cursor.getString(1);

                listOfDates.add(date);
                eventAdapter.add(event);
                cursor.moveToNext();
            }

            startManagingCursor(cursor);
        } catch (Exception e) {
            Log.wtf("PROB", e.getMessage());

        }

        eventAdapter.notifyDataSetChanged();

        if (listOfDates.size() == 0) {
            showToast(getString(R.string.no_events));

            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private String asDate(String usDate) {
        String systemDateFormat = DateFormat.GetSystemDateFormat(this);

        String str;

        SimpleDate sd = new SimpleDate(usDate, SimpleDate.DateStyle.US);

        if (systemDateFormat.equals(getString(R.string.uk_date_style))) {
            str = sd.getDate2(SimpleDate.DateStyle.UK);
        }
        else if (systemDateFormat.equals("Year-Month-Day")) {
            str = sd.getDate2(SimpleDate.DateStyle.US);
        }
        else {
            str = sd.getDate2(SimpleDate.DateStyle.MYD);
        }

        return str;
    }

    private String selectedEventDate;
    private String selectedEvent;

    public void updateDisplay() {

        StringBuilder sb = new StringBuilder();

        if (spinnerE.getSelectedItem() == null) {
            // showToast("mo liu do!");
            return;
        }

        // showToast("Date1 is " + usDate1);

        if (usDate1 == null || usDate1 == "")
            return;

        selectedEvent = spinnerE.getSelectedItem().toString();

        // showToast("selected event is" + selectedEvent);

        sb.append(selectedEvent);
        sb.append(": ");

        selectedEventDate = asDate(usDate1);
        sb.append(selectedEventDate);

        descText.setText(sb.toString());
    }

    private View.OnClickListener doneDialog = new View.OnClickListener() {
        public void onClick(View v) {

            Intent intent = getIntent();

            intent.putExtra("event", selectedEvent);
            intent.putExtra("eventDate", usDate1);
            intent.putExtra("eventDateText", selectedEventDate);

            // showToast(" look " + selectedEvent + " @ " + selectedEventDate);

            setResult(RESULT_OK, intent);
            finish();
        }
    };

}
