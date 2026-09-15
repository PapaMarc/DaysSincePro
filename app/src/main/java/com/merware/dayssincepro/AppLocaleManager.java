package com.merware.dayssincepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

final class AppLocaleManager {

    static final String PREF_APP_LANGUAGE = "app_language";
    static final String VALUE_SYSTEM = "system";

    private AppLocaleManager() {
    }

    static void applyStoredLocale(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String localeValue = prefs.getString(PREF_APP_LANGUAGE, VALUE_SYSTEM);
        applyLocaleValue(context, localeValue, "stored_locale");
    }

    static void applyLocaleValue(String localeValue) {
        applyLocaleValue(null, localeValue, "unspecified");
    }

    static void applyLocaleValue(Context context, String localeValue, String source) {
        LocaleListCompat targetLocales;
        if (localeValue == null || VALUE_SYSTEM.equals(localeValue)) {
            targetLocales = LocaleListCompat.getEmptyLocaleList();
        } else {
            targetLocales = LocaleListCompat.forLanguageTags(localeValue);
        }

        String currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String targetTags = targetLocales.toLanguageTags();

        DeveloperToolsSession.logTrackB(
                "LocaleFlow",
                "event=locale_apply_start source=" + source
                        + " current_tags=" + tagsForLog(currentTags)
                        + " target_tags=" + tagsForLog(targetTags));

        boolean applied = !targetTags.equals(currentTags);
        if (applied) {
            AppCompatDelegate.setApplicationLocales(targetLocales);
        }

        String finalTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        DeveloperToolsSession.logTrackB(
                "LocaleFlow",
                "event=locale_apply_result source=" + source
                        + " applied=" + applied
                        + " final_tags=" + tagsForLog(finalTags));
    }

    static String currentPreferenceValue() {
        String tags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (tags == null || tags.trim().length() == 0) {
            return VALUE_SYSTEM;
        }

        int firstComma = tags.indexOf(',');
        return firstComma > 0 ? tags.substring(0, firstComma) : tags;
    }

    private static String tagsForLog(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return VALUE_SYSTEM;
        }
        return tags;
    }
}
