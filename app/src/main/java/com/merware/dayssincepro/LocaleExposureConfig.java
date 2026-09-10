package com.merware.dayssincepro;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class LocaleExposureConfig {

    static final String[] RELEASE_EXPOSED_LOCALES = new String[]{
            "en",
            "fr",
            "es",
            "de",
            "pt",
            "pt-BR",
            "it",
            "zh-CN",
            "hi"
    };

    static final String[] SIDELOAD_PSEUDO_LOCALES = new String[]{
            "en-XA",
            "ar-XB"
    };

    private static final String[] PICKER_LOCALE_ORDER = new String[]{
            "en",
            "en-XA",
            "ar-XB",
            "fr",
            "es",
            "de",
            "pt",
            "pt-BR",
            "it",
            "zh-CN",
            "hi"
    };

    private LocaleExposureConfig() {
    }

    static final class PickerOption {
        final String value;
        final int labelResId;

        PickerOption(String value, int labelResId) {
            this.value = value;
            this.labelResId = labelResId;
        }
    }

    static boolean isSideloadBuild(Context context) {
        if (context == null) {
            return false;
        }
        String packageName = context.getPackageName();
        return packageName != null && packageName.endsWith(".dev");
    }

    static PickerOption[] pickerOptions(Context context) {
        List<PickerOption> options = new ArrayList<>();
        options.add(new PickerOption(AppLocaleManager.VALUE_SYSTEM, R.string.settings_language_use_device));

        Set<String> exposed = new LinkedHashSet<>(Arrays.asList(RELEASE_EXPOSED_LOCALES));
        if (isSideloadBuild(context) && DeveloperToolsSession.isPseudoLangsEnabled()) {
            exposed.addAll(Arrays.asList(SIDELOAD_PSEUDO_LOCALES));
        }

        for (String tag : PICKER_LOCALE_ORDER) {
            if (!exposed.contains(tag)) {
                continue;
            }
            options.add(new PickerOption(tag, labelResIdForTag(tag)));
        }

        return options.toArray(new PickerOption[0]);
    }

    static boolean isPickerValueExposed(String value, Context context) {
        if (value == null || AppLocaleManager.VALUE_SYSTEM.equals(value)) {
            return true;
        }

        for (PickerOption option : pickerOptions(context)) {
            if (option.value.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static int labelResIdForTag(String tag) {
        switch (tag) {
            case "en":
                return R.string.settings_language_english;
            case "en-XA":
                return R.string.settings_language_pseudo_accented;
            case "ar-XB":
                return R.string.settings_language_pseudo_bidi;
            case "fr":
                return R.string.settings_language_french;
            case "es":
                return R.string.settings_language_spanish;
            case "de":
                return R.string.settings_language_german;
            case "pt":
                return R.string.settings_language_portuguese;
            case "pt-BR":
                return R.string.settings_language_portuguese_brazil;
            case "it":
                return R.string.settings_language_italian;
            case "zh-CN":
                return R.string.settings_language_chinese_simplified;
            case "hi":
                return R.string.settings_language_hindi;
            default:
                throw new IllegalArgumentException("Unsupported locale tag: " + tag);
        }
    }
}