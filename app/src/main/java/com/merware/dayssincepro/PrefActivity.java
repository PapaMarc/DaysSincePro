package com.merware.dayssincepro;

import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class PrefActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private String appliedThemeValue = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        applySettingsTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pref);
        EdgeToEdgeUtil.applyContentInsets(this);

        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle(R.string.settings_title);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsPreferenceFragment())
                    .commit();
        }
    }

    private void applySettingsTheme() {
        String stheme = preferences.getString("theme", "0");
        appliedThemeValue = stheme;
        int theme = Integer.parseInt(stheme);

        if (theme == 1) {
            setTheme(R.style.SettingsThemeDark);
        } else {
            setTheme(R.style.SettingsThemeLight);
        }
    }

    void onThemePreferenceChanged(String newThemeValue) {
        if (newThemeValue == null || newThemeValue.equals(appliedThemeValue)) {
            return;
        }

        appliedThemeValue = newThemeValue;
        getWindow().getDecorView().post(() -> {
            if (!isFinishing() && !isDestroyed()) {
                recreate();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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

    public static class SettingsPreferenceFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private ListPreference fontSizePref;
        private ListPreference categorySortOrderPref;
        private ListPreference eventsSortOrderPref;
        private ListPreference displayStylePref;
        private ListPreference dateStylePref;
        private ListPreference themePref;
        private ListPreference remindPref;
        private ListPreference tabStylePref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.options);

            fontSizePref = (ListPreference) findPreference("font_size");
            categorySortOrderPref = (ListPreference) findPreference("category_sort_order");
            eventsSortOrderPref = (ListPreference) findPreference("event_sort_order");
            displayStylePref = (ListPreference) findPreference("disp_style");
            dateStylePref = (ListPreference) findPreference("date_style");
            themePref = (ListPreference) findPreference("theme");
            remindPref = (ListPreference) findPreference("remind_percent");
            tabStylePref = (ListPreference) findPreference("tab_style");
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            Preference pref = findPreference(key);

            if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                pref.setSummary(listPref.getEntry());
            }

            if ("noti".equals(key)) {
                boolean isNotiOn = sharedPreferences.getBoolean("noti", false);
                if (isNotiOn && getActivity() != null
                        && !NotificationPermissionHelper.areNotificationsEnabled(getActivity())) {
                    NotificationPermissionHelper.promptEnableNotifications(getActivity());
                }
            }

            if ("theme".equals(key) && getActivity() instanceof PrefActivity) {
                ((PrefActivity) getActivity()).onThemePreferenceChanged(
                        sharedPreferences.getString("theme", "0"));
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            // Keep summary behavior identical to legacy Settings defaults.
            setListSummary(categorySortOrderPref, R.string.input_order);
            setListSummary(eventsSortOrderPref, R.string.input_order);
            setListSummary(fontSizePref, R.string.medium);
            setListSummary(displayStylePref, R.string.years_months_days);
            setListSummary(dateStylePref, R.string.us_date_style);
            setListSummary(themePref, R.string.light);
            setListSummary(remindPref, R.string.quarter_till);
            setListSummary(tabStylePref, R.string.show_tab);

            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        private void setListSummary(ListPreference pref, int fallbackResId) {
            if (pref == null) {
                return;
            }

            if (pref.getEntry() == null) {
                pref.setSummary(fallbackResId);
            } else {
                pref.setSummary(pref.getEntry());
            }
        }
    }
}
