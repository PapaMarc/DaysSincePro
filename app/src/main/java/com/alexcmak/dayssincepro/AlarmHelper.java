package com.alexcmak.dayssincepro;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by Alex on 1/31/2015.
 */
public class AlarmHelper {

    void showToast(String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }


    Context context;
    public AlarmHelper(Context context)
    {
        this.context = context;
    }

    public void setAlarm(int seconds) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, OnAlarmReceive.class);

        //showToast("Classic setAlarm");

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Getting current time and add the seconds in it
        Calendar cal = Calendar.getInstance();

        // showToast("seconds option is " + seconds);

        if (seconds == 1)
        {
            // when the app is first called; send an immediate alarm
            // then at midnight set additional alarms

            cal.add(Calendar.SECOND, seconds);

            AlarmManager alarmManager1 = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent1 = new Intent(context, OnAlarmReceive.class);

            PendingIntent pendingIntent1;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent1 = PendingIntent.getBroadcast(
                        context, 1, intent1,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            }else {
                pendingIntent1 = PendingIntent.getBroadcast(
                        context, 1, intent1,
                        PendingIntent.FLAG_UPDATE_CURRENT);
            }


            // showToast("set once");
            alarmManager1.set(AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    pendingIntent1);
        }

        // set alarm at midnight
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 1);
        cal.set(Calendar.MILLISECOND, 0);

        // next day
        cal.add(Calendar.DAY_OF_MONTH, 1);
        //	showToast(getString(R.string.next_noti));

        // repeat every day
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    int requestCode = 0;

    public void setAlarm(long id, int hour, int minute) {

        //  showToast("factored out set Alarm");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, OnAlarmReceive.class);

        intent.putExtra("eventID", id);

        requestCode = (int) id; // for uniqueness, use event id.

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }else {
            pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Getting current time and add time to it
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 1);
        cal.set(Calendar.MILLISECOND, 0);


        // repeat every day
        alarmManager
                .setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public void cancelAlarm() {

        Intent intent = new Intent(context, OnAlarmReceive.class);
        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }else {
            pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        if (pendingIntent != null) {
            pendingIntent.cancel();
            // showToast("Cancel alarm, sir");
        }
    }


}
