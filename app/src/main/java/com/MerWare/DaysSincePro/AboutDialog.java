package com.MerWare.DaysSincePro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
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
        String aboutText = "Originally written by " + author + "\n" + date;

        String maintained = context.getString(R.string.about_maintained);
        String republished = context.getString(R.string.about_republished);
        String maintainedAndRepublished = context.getString(
                R.string.about_maintained_and_republished,
                maintained,
                republished);
        String fullAboutText = aboutText + "\n" + maintainedAndRepublished;

        // Set up the TextView
        final TextView message = new TextView(context);
        SpannableStringBuilder messageText = new SpannableStringBuilder(
                versionString + "\n\n" + fullAboutText);

        int maintainedStart = messageText.toString().indexOf(maintained);
        int republishedStart = messageText.toString().indexOf(
                republished, maintainedStart + maintained.length());

        if (maintainedStart >= 0) {
            messageText.setSpan(
                    new URLSpan("https://github.com/PapaMarc/DaysSincePro/tree/PapaMarcBranch"),
                    maintainedStart,
                    maintainedStart + maintained.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (republishedStart >= 0) {
            messageText.setSpan(
                    new URLSpan("https://play.google.com/store/search?q=DaysSincePro&c=apps"),
                    republishedStart,
                    republishedStart + republished.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Set some padding
        message.setPadding(5, 5, 5, 5);
        message.setText(messageText);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setLinksClickable(true);

        return new AlertDialog.Builder(context)
                .setTitle(aboutTitle)
                .setInverseBackgroundForced(true)
                .setCancelable(true)
                .setPositiveButton(context.getString(android.R.string.ok), null)
                .setView(message).create();
    }
}