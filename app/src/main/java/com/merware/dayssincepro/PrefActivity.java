package com.merware.dayssincepro;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

public class PrefActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private ListPreference fontSizePref;
    private ListPreference categorySortOrderPref;
    private ListPreference eventsSortOrderPref;
    private ListPreference displayStylePref;  // days
    private ListPreference dateStylePref;    // us, uk style
    private ListPreference themePref;
    private ListPreference remindPref;
    private ListPreference tabStylePref; // Days Since or Days Until

    SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String stheme = preferences.getString("theme", "0");
        int theme = Integer.parseInt(stheme);

        if (theme == 1) {
            setTheme(R.style.AppBaseTheme2);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdgeUtil.applyContentInsets(this);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        addPreferencesFromResource(R.xml.options);

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        fontSizePref = (ListPreference) getPreferenceScreen().findPreference(
                "font_size");
        categorySortOrderPref = (ListPreference) getPreferenceScreen()
                .findPreference("category_sort_order");
        eventsSortOrderPref = (ListPreference) getPreferenceScreen()
                .findPreference("event_sort_order");
        displayStylePref = (ListPreference) getPreferenceScreen()
                .findPreference("disp_style");
        dateStylePref = (ListPreference) getPreferenceScreen().findPreference(
                "date_style");
        themePref = (ListPreference) getPreferenceScreen().findPreference(
                "theme");
        remindPref = (ListPreference) getPreferenceScreen().findPreference(
                "remind_percent");
        tabStylePref = (ListPreference) getPreferenceScreen().findPreference(
                "tab_style");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference pref = findPreference(key);

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        }

        if ("noti".equals(key)) {
            boolean isNotiOn = sharedPreferences.getBoolean("noti", false);
            if (isNotiOn && !NotificationPermissionHelper.areNotificationsEnabled(this)) {
                NotificationPermissionHelper.promptEnableNotifications(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NotificationPermissionHelper.REQUEST_NOTIF_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                NotificationPermissionHelper.showNotificationSettingsDialog(this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Setup the initial values
        if (categorySortOrderPref.getEntry() == null)
            categorySortOrderPref.setSummary(R.string.input_order);
        else
            categorySortOrderPref.setSummary(categorySortOrderPref.getEntry());

        if (eventsSortOrderPref.getEntry() == null)
            eventsSortOrderPref.setSummary(R.string.input_order);
        else
            eventsSortOrderPref.setSummary(eventsSortOrderPref.getEntry());

        if (fontSizePref.getEntry() == null)
            fontSizePref.setSummary(R.string.medium);
        else
            fontSizePref.setSummary(fontSizePref.getEntry());

        if (displayStylePref.getEntry() == null)
            displayStylePref.setSummary(R.string.years_months_days);
        else
            displayStylePref.setSummary(displayStylePref.getEntry());

        if (dateStylePref.getEntry() == null)
            dateStylePref.setSummary(R.string.us_date_style);
        else
            dateStylePref.setSummary(dateStylePref.getEntry());

        if (themePref.getEntry() == null)
            themePref.setSummary(R.string.light);
        else
            themePref.setSummary(themePref.getEntry());

        if (remindPref.getEntry() == null) {
            remindPref.setSummary(R.string.quarter_till);
        }
        else {
            remindPref.setSummary(remindPref.getEntry());
        }


        if (tabStylePref.getEntry() == null) {
            tabStylePref.setSummary(R.string.show_tab);
        }
        else {
            tabStylePref.setSummary(tabStylePref.getEntry());
        }


        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
