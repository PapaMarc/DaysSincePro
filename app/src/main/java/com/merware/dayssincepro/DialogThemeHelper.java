package com.merware.dayssincepro;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

final class DialogThemeHelper {

    private DialogThemeHelper() {
    }

    static AlertDialog.Builder themedBuilder(Context context) {
        String themeValue = ThemeMode.getThemeValue(context);
        Context themedContext = new ContextThemeWrapper(context, ThemeMode.dialogThemeResId(themeValue));
        return new AlertDialog.Builder(themedContext);
    }
}
