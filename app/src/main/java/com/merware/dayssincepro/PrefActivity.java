package com.merware.dayssincepro;

import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PrefActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private String appliedThemeValue = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppLocaleManager.applyStoredLocale(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        applySettingsTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pref);
        EdgeToEdgeUtil.applyContentInsets(this);
        TopBarHelper.setupCenteredBackToolbar(this, R.id.settings_toolbar, R.string.settings_title);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsPreferenceFragment())
                    .commit();
        }
    }

    private void applySettingsTheme() {
        String stheme = ThemeMode.getThemeValue(this);
        appliedThemeValue = stheme;
        setTheme(ThemeMode.settingsThemeResId(stheme));
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
        private ListPreference appLanguagePref;
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
            appLanguagePref = (ListPreference) findPreference(AppLocaleManager.PREF_APP_LANGUAGE);
            remindPref = (ListPreference) findPreference("remind_percent");
            tabStylePref = (ListPreference) findPreference("tab_style");

            configureAppLanguagePreference();
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
                if (getActivity() != null) {
                    Toast.makeText(
                            getActivity(),
                            isNotiOn
                                    ? R.string.daily_notifications_enabled_toast
                                    : R.string.daily_notifications_disabled_toast,
                            Toast.LENGTH_SHORT
                    ).show();
                }
                if (isNotiOn && getActivity() != null
                        && !NotificationPermissionHelper.areNotificationsEnabled(getActivity())) {
                    NotificationPermissionHelper.promptEnableNotifications(getActivity());
                }
            }

            if ("theme".equals(key) && getActivity() instanceof PrefActivity) {
                ((PrefActivity) getActivity()).onThemePreferenceChanged(
                        sharedPreferences.getString("theme", ThemeMode.THEME_LIGHT));
            }

            if (AppLocaleManager.PREF_APP_LANGUAGE.equals(key)) {
                String localeValue = sharedPreferences.getString(
                        AppLocaleManager.PREF_APP_LANGUAGE,
                        AppLocaleManager.VALUE_SYSTEM
                );
                AppLocaleManager.applyLocaleValue(localeValue);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            applyPersistentScrollIndicator();

            // Keep summary behavior identical to legacy Settings defaults.
            setListSummary(categorySortOrderPref, R.string.input_order);
            setListSummary(eventsSortOrderPref, R.string.input_order);
            setListSummary(fontSizePref, R.string.medium);
            setListSummary(displayStylePref, R.string.years_months_days);
            setListSummary(dateStylePref, R.string.us_date_style);
            setListSummary(themePref, R.string.light);
            syncLanguagePreferenceValue();
            setListSummary(appLanguagePref, R.string.settings_language_use_device);
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

        private void applyPersistentScrollIndicator() {
            View root = getView();
            if (root == null) {
                return;
            }

            ListView listView = root.findViewById(android.R.id.list);
            if (listView == null) {
                return;
            }

            listView.setVerticalScrollBarEnabled(true);
            listView.setScrollbarFadingEnabled(false);
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

        private void syncLanguagePreferenceValue() {
            if (appLanguagePref == null) {
                return;
            }

            String currentValue = AppLocaleManager.currentPreferenceValue();
            if (!LocaleExposureConfig.isPickerValueExposed(currentValue, getActivity())) {
                currentValue = AppLocaleManager.VALUE_SYSTEM;
                AppLocaleManager.applyLocaleValue(currentValue);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                prefs.edit().putString(AppLocaleManager.PREF_APP_LANGUAGE, currentValue).apply();
            }

            if (!currentValue.equals(appLanguagePref.getValue())) {
                appLanguagePref.setValue(currentValue);
            }
        }

        private void configureAppLanguagePreference() {
            if (appLanguagePref == null) {
                return;
            }

            LocaleExposureConfig.PickerOption[] options = LocaleExposureConfig.pickerOptions(getActivity());
            CharSequence[] entries = new CharSequence[options.length];
            CharSequence[] values = new CharSequence[options.length];
            for (int i = 0; i < options.length; i++) {
                entries[i] = getString(options[i].labelResId);
                values[i] = options[i].value;
            }

            appLanguagePref.setEntries(entries);
            appLanguagePref.setEntryValues(values);
        }

    }
}
