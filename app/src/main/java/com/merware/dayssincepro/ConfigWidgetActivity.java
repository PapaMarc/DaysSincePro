package com.merware.dayssincepro;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ConfigWidgetActivity extends AppCompatActivity {

    // resources do not exist with widgets
    String APP_NAME =  "Days Since Pro 3"; // Resources.getSystem().getString(R.string.app_name);
    int mAppWidgetId = 0;

    SharedPreferences preferences;
    protected SQLiteDatabase db;
    String selectedEvent = "";
    int selectedColorIndex = 0;
    int selectedDisplayStyle = 0;
    Spinner spinnerW;
    Spinner spinnerC;
    Spinner spinnerS;
    CheckBox checkBoxOpaque;
    int selectedOpaque = 0;

    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String themeValue = ThemeMode.getThemeValue(this);
        boolean darkTheme = ThemeMode.isDark(themeValue);
        setTheme(ThemeMode.miniAScreenThemeResId(themeValue));

        super.onCreate(savedInstanceState);

        setContentView(R.layout.configwidget);
        EdgeToEdgeUtil.applyContentInsets(this);
        TopBarHelper.setupCenteredBackToolbar(this, R.id.mini_b_toolbar, 0);
        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        int position = 0;
        String savedEvent = Preferences.getPreferenceString(
                ConfigWidgetActivity.this, APP_NAME, "widget" + mAppWidgetId);

        int savedColorIndex = Preferences.getPreferenceInt(
                ConfigWidgetActivity.this, APP_NAME, "widgetColor"
                        + mAppWidgetId);

        int savedOpaqueIndex = Preferences.getPreferenceInt(
                ConfigWidgetActivity.this, APP_NAME, "widgetOpaque"
                        + mAppWidgetId);

        int savedStyleIndex = Preferences.getPreferenceInt(
                ConfigWidgetActivity.this, APP_NAME, "widgetStyle"
                        + mAppWidgetId);

        // get all event

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                this, android.R.layout.simple_spinner_item);

        try {

            Cursor cursor = db.query("event", /* table */
                    new String[] { "event", }, /* columns */
                    null, null, null, null, "event");

            // looping through all rows and adding to list
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {

                String event = cursor.getString(0);
                adapter.add(event);
                if (savedEvent.equals(event))
                    position = i;

                cursor.moveToNext();

            }

            startManagingCursor(cursor);
        } catch (Exception e) {
            showToast("db problems." + e.getMessage());
            Log.wtf("PROB", e.getMessage());

        }

        if (darkTheme) { // dark
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_compact);
        }
        else {
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        spinnerW = (Spinner) this.findViewById(R.id.spinnerW);
        spinnerW.setAdapter(adapter);
        spinnerW.setSelection(position);

        spinnerC = (Spinner) this.findViewById(R.id.spinnerC);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this, R.array.colors, android.R.layout.simple_spinner_item);

        if (darkTheme) { // dark
            adapter2.setDropDownViewResource(R.layout.spinner_dropdown_item_compact);
        }
        else {
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        spinnerC.setAdapter(adapter2);
        spinnerC.setSelection(savedColorIndex);

        checkBoxOpaque = (CheckBox) this.findViewById(R.id.checkBoxOpaque);

        if (savedOpaqueIndex == 1)
            checkBoxOpaque.setChecked(true);

        spinnerS = (Spinner) this.findViewById(R.id.spinnerS);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(
                this, R.array.styles, android.R.layout.simple_spinner_item);

        if (darkTheme) { // dark
            adapter3.setDropDownViewResource(R.layout.spinner_dropdown_item_compact);
        }
        else {
            adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        spinnerS.setAdapter(adapter3);
        spinnerS.setSelection(savedStyleIndex);

        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(myButtonListener);

    }

    private OnClickListener myButtonListener = new OnClickListener() {
        public void onClick(View v) {
            selectedEvent = (String) spinnerW.getSelectedItem();
            selectedColorIndex = spinnerC.getSelectedItemPosition();
            selectedDisplayStyle = spinnerS.getSelectedItemPosition();

            if (checkBoxOpaque.isChecked())
                selectedOpaque = 1;
            else
                selectedOpaque = 0;

            Preferences.storePreferenceString(ConfigWidgetActivity.this,
                    APP_NAME, "widget" + mAppWidgetId, selectedEvent);

            Preferences.storePreferenceInt(ConfigWidgetActivity.this, APP_NAME,
                    "widgetColor" + mAppWidgetId, selectedColorIndex);

            Preferences.storePreferenceInt(ConfigWidgetActivity.this, APP_NAME,
                    "widgetOpaque" + mAppWidgetId, selectedOpaque);

            Preferences.storePreferenceInt(ConfigWidgetActivity.this, APP_NAME,
                    "widgetStyle" + mAppWidgetId, selectedDisplayStyle);

            updateWidget();

            finish();
        }
    };

    void showToast(String s) {
        Toast.makeText(ConfigWidgetActivity.this, s, Toast.LENGTH_LONG).show();
    }

    void updateWidget() {
        Intent intent = new Intent(this, DaysSinceAppWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        // Use an array and EXTRA_APPWIDGET_IDS instead of
        // AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        int[] ids = { mAppWidgetId };
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        // showToast("broadcast!");
        sendBroadcast(intent);
    }

}
