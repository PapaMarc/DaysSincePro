package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThemeModeTest {

    @Test
    public void isDark_usesThemeValueContract() {
        assertTrue(ThemeMode.isDark(ThemeMode.THEME_DARK));
        assertFalse(ThemeMode.isDark(ThemeMode.THEME_LIGHT));
        assertFalse(ThemeMode.isDark("unexpected"));
    }

    @Test
    public void styleMapping_methodsReturnExpectedThemeResIds() {
        assertEquals(R.style.AppTheme2, ThemeMode.mainThemeResId(ThemeMode.THEME_DARK));
        assertEquals(R.style.AppTheme, ThemeMode.mainThemeResId(ThemeMode.THEME_LIGHT));

        assertEquals(R.style.SettingsThemeDark, ThemeMode.settingsThemeResId(ThemeMode.THEME_DARK));
        assertEquals(R.style.SettingsThemeLight, ThemeMode.settingsThemeResId(ThemeMode.THEME_LIGHT));

        assertEquals(R.style.AppDialogTheme2, ThemeMode.dialogThemeResId(ThemeMode.THEME_DARK));
        assertEquals(R.style.AppDialogTheme, ThemeMode.dialogThemeResId(ThemeMode.THEME_LIGHT));

        assertEquals(R.style.MiniAThemeDark, ThemeMode.miniAScreenThemeResId(ThemeMode.THEME_DARK));
        assertEquals(R.style.MiniAThemeLight, ThemeMode.miniAScreenThemeResId(ThemeMode.THEME_LIGHT));

        assertEquals(R.style.MiniAEventThemeDark, ThemeMode.miniAEventThemeResId(ThemeMode.THEME_DARK));
        assertEquals(R.style.MiniAEventThemeLight, ThemeMode.miniAEventThemeResId(ThemeMode.THEME_LIGHT));

        assertEquals(R.style.DatePickerHostThemeDark, ThemeMode.datePickerHostThemeResId(ThemeMode.THEME_DARK));
        assertEquals(R.style.DatePickerHostThemeLight, ThemeMode.datePickerHostThemeResId(ThemeMode.THEME_LIGHT));
    }
}
