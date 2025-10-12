package com.alexcmak.dayssincepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.SpannableString;
import android.widget.TextView;

public class AboutDialog {
    public static AlertDialog create(Context context, String author,
                                     String date, String url) {

        String versionInfo = "0.0";

        PackageInfo pInfo;

        try {
            // Try to load the a package matching the name of our own package

            pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            versionInfo = pInfo.versionName;
        } catch (NameNotFoundException e) {

        }

        String aboutTitle = context.getString(R.string.about) + " " +
                context.getString(R.string.app_name);
        String versionString = String.format("Version: %s", versionInfo);
        //String aboutText = "Written by " + author + "\nFrench Translation by Yacine Bezzaz\n" + date;
        String aboutText = "Written by " + author + "\n" + date;


        // Set up the TextView
        final TextView message = new TextView(context);
        // We'll use a spannablestring to be able to make links clickable
        final SpannableString s = new SpannableString(aboutText);

        // Set some padding
        message.setPadding(5, 5, 5, 5);
        // Set up the final string
        message.setText(versionString + "\n\n" + s);

        // Now linkify the text
        // Linkify.addLinks(message, Linkify.ALL);

        return new AlertDialog.Builder(context)
                .setTitle(aboutTitle)
                .setInverseBackgroundForced(true)
                .setCancelable(true)
                .setPositiveButton(context.getString(android.R.string.ok), null)
                .setView(message).create();
    }
}