package com.MerWare.DaysSincePro;


// util functions from uncle joe
// for use with widget


import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    public static String getPreferenceString(Context context, String appName,
                                             String prefName) {
        SharedPreferences settings = context.getSharedPreferences(appName,
                Context.MODE_PRIVATE);
        return settings.getString(prefName, "");
    }


    public static void storePreferenceString(Context context, String appName,
                                             String prefName, String value) {
        SharedPreferences settings = context.getSharedPreferences(appName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(prefName, value);
        editor.commit();
    }

    public static int getPreferenceInt(Context context, String appName,
                                       String prefName) {
        SharedPreferences settings = context.getSharedPreferences(appName,
                Context.MODE_PRIVATE);
        return settings.getInt(prefName, 0);
    }

    public static void storePreferenceInt(Context context, String appName,
                                          String prefName, int value) {
        SharedPreferences settings = context.getSharedPreferences(appName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(prefName, value);
        editor.commit();
    }
}
