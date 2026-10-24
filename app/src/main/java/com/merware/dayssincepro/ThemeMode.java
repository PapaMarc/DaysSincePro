package com.merware.dayssincepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

final class ThemeMode {

    private ThemeMode() {
    }

    static final String THEME_LIGHT = "0";
    static final String THEME_DARK = "1";

    static String getThemeValue(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("theme", THEME_LIGHT);
    }

    static boolean isDark(String themeValue) {
        return THEME_DARK.equals(themeValue);
    }

    static boolean isDark(Context context) {
        return isDark(getThemeValue(context));
    }

    static int mainThemeResId(String themeValue) {
        return isDark(themeValue) ? R.style.AppTheme2 : R.style.AppTheme;
    }

    static int settingsThemeResId(String themeValue) {
        return isDark(themeValue) ? R.style.SettingsThemeDark : R.style.SettingsThemeLight;
    }

    static int dialogThemeResId(String themeValue) {
        return isDark(themeValue) ? R.style.AppDialogTheme2 : R.style.AppDialogTheme;
    }

    static int miniAScreenThemeResId(String themeValue) {
        return isDark(themeValue) ? R.style.MiniAThemeDark : R.style.MiniAThemeLight;
    }

    static int miniAEventThemeResId(String themeValue) {
        return isDark(themeValue) ? R.style.MiniAEventThemeDark : R.style.MiniAEventThemeLight;
    }

    static int datePickerHostThemeResId(String themeValue) {
        return isDark(themeValue) ? R.style.DatePickerHostThemeDark : R.style.DatePickerHostThemeLight;
    }

    static int datePickerDialogThemeResId(String themeValue) {
        return isDark(themeValue)
                ? R.style.DatePickerDialogThemeDark
                : R.style.DatePickerDialogThemeLight;
    }
}