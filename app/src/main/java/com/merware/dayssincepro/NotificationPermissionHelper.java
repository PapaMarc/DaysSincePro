package com.merware.dayssincepro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {

    public static final int REQUEST_NOTIF_PERMISSION = 101;

    public static boolean areNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void promptEnableNotifications(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.enable_notifications_title)
                            .setMessage(R.string.notification_permission_rationale)
                            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(activity,
                                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                            REQUEST_NOTIF_PERMISSION);
                                }
                            })
                            .setNegativeButton(R.string.Cancel, null)
                            .show();
                } else {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_NOTIF_PERMISSION);
                }
                return;
            }
        }

        showNotificationSettingsDialog(activity);
    }

    public static void showNotificationSettingsDialog(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.enable_notifications_title)
                .setMessage(R.string.enable_notifications_msg)
                .setPositiveButton(R.string.open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                        try {
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            fallbackIntent.setData(Uri.parse("package:" + context.getPackageName()));
                            context.startActivity(fallbackIntent);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }
}
