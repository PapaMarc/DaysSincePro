package com.alexcmak.dayssincepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by alexc on 4/2/2016.
 */

public class DateFormat {

    public static String GetSystemDateFormat(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String dateStyle = preferences.getString("date_style", "0");

        // gets 0 for US, 1 for UK

        switch (dateStyle)
        {
            case "0":
                dateStyle = context.getString(R.string.us_date_style);
                break;
            case "1":
                dateStyle = context.getString(R.string.uk_date_style);
                break;
            case "2":
                dateStyle = "Month-Day-Year";
                break;
        }

        return dateStyle;

    }
}
