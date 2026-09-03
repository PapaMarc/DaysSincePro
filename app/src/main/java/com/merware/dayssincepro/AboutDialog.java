package com.merware.dayssincepro;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

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

        String aboutTitle = "About: DaysSincePro";
        String versionString = String.format("Version: %s", versionInfo);
        String packageName = context.getPackageName();
        boolean isSideloadBuild = packageName.endsWith(".dev");
        String sideloadString = String.format("SideLoad .apk: %s", packageName);
        String headerText = isSideloadBuild
                ? versionString + "\n" + sideloadString
                : versionString;
        String aboutText = "Originally written by " + author + "\n" + date;

        String maintained = context.getString(R.string.about_maintained);
        String republished = context.getString(R.string.about_republished);
        String maintainedAndRepublished = context.getString(
                R.string.about_maintained_and_republished,
                maintained,
                republished);

        String marcSeinfeld = "Marc Seinfeld";
        String merWare = "MerWare";
        String donationLinkText = "optional donation here";
        String donationText = "Voluntary contributions help keep this app free. " +
                "You can share your " + donationLinkText +
                ", though doing so does not unlock features.";

        // Maintained/republished line appears first, then the donation blurb,
        // then the original author credit below it, each separated by a blank line.
        String fullAboutText = maintainedAndRepublished + "\n\n" + donationText +
                "\n\n" + aboutText;

        // Custom centered title
        final TextView title = new TextView(context);
        title.setText(aboutTitle);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setPadding(0, 32, 0, 16);

        // Set up the TextView
        final TextView message = new TextView(context);
        SpannableStringBuilder messageText = new SpannableStringBuilder(
                headerText + "\n\n" + fullAboutText);

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

        int marcSeinfeldStart = messageText.toString().indexOf(marcSeinfeld);
        if (marcSeinfeldStart >= 0) {
            messageText.setSpan(
                    new URLSpan("https://merware.net/index.html#marc"),
                    marcSeinfeldStart,
                    marcSeinfeldStart + marcSeinfeld.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int merWareStart = messageText.toString().indexOf(merWare);
        if (merWareStart >= 0) {
            messageText.setSpan(
                    new URLSpan("https://merware.net/index.html#portfolio"),
                    merWareStart,
                    merWareStart + merWare.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int donationLinkStart = messageText.toString().indexOf(donationLinkText);
        if (donationLinkStart >= 0) {
            messageText.setSpan(
                    new URLSpan("https://merware.net/index.html#support"),
                    donationLinkStart,
                    donationLinkStart + donationLinkText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Center the version line for all builds, and the sideload line when present.
        messageText.setSpan(
                new android.text.style.AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                headerText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Balanced left/right margins
        float density = context.getResources().getDisplayMetrics().density;
        int horizontalPadding = (int) (24 * density);
        message.setPadding(horizontalPadding, 8, horizontalPadding, 24);
        message.setText(messageText);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setLinksClickable(true);

        return new AlertDialog.Builder(context)
                .setCustomTitle(title)
                .setInverseBackgroundForced(true)
                .setCancelable(true)
                .setPositiveButton(context.getString(android.R.string.ok), null)
                .setView(message).create();
    }
}