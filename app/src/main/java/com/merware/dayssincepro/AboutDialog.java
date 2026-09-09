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
        public static AlertDialog create(Context context) {

        String versionInfo = "0.0";

        PackageInfo pInfo;

        try {
            // Try to load the a package matching the name of our own package

            pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            versionInfo = pInfo.versionName;
        } catch (NameNotFoundException e) {

        }

        String author = context.getString(R.string.about_author_name);
        String date = context.getString(R.string.about_author_timeline);

        String aboutTitle = context.getString(R.string.about_title, context.getString(R.string.app_name));
        String versionString = context.getString(R.string.about_version_format, versionInfo);
        String packageName = context.getPackageName();
        boolean isSideloadBuild = packageName.endsWith(".dev");
        String schemaString = context.getString(R.string.about_schema_format, DatabaseHelper.DATABASE_VERSION);
        String sideloadString = context.getString(R.string.about_sideload_format, packageName);
        String headerText = isSideloadBuild
                ? versionString + "\n" + schemaString + "\n" + sideloadString
                : versionString;
        String aboutText = context.getString(R.string.about_originally_written_by, author, date);

        String maintained = context.getString(R.string.about_maintained);
        String republished = context.getString(R.string.about_republished);
        String maintainedAndRepublished = context.getString(
                R.string.about_maintained_and_republished,
                maintained,
                republished);

        String marcSeinfeld = context.getString(R.string.about_marc_name);
        String merWare = context.getString(R.string.about_merware_name);
        String donationLinkText = context.getString(R.string.about_donation_link_text);
        String donationText = context.getString(R.string.about_donation_text, donationLinkText);
        String supportText = context.getString(R.string.about_support_feedback);
        String supportEmail = context.getString(R.string.about_support_email);

        // Maintained/republished line appears first, then the donation blurb,
        // then the original author credit below it, each separated by a blank line.
        String fullAboutText = maintainedAndRepublished + "\n\n" + donationText +
                "\n\n" + supportText + "\n\n" + aboutText;

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

        int merWareStart = marcSeinfeldStart >= 0
                ? messageText.toString().indexOf(merWare, marcSeinfeldStart + marcSeinfeld.length())
                : messageText.toString().indexOf(merWare);
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

        int supportEmailStart = messageText.toString().indexOf(supportEmail);
        if (supportEmailStart >= 0) {
            messageText.setSpan(
                    new URLSpan("mailto:" + supportEmail),
                    supportEmailStart,
                    supportEmailStart + supportEmail.length(),
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

        return DialogThemeHelper.themedBuilder(context)
                .setCustomTitle(title)
                .setCancelable(true)
                .setPositiveButton(context.getString(android.R.string.ok), null)
                .setView(message).create();
    }
}