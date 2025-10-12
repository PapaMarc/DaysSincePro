package com.alexcmak.dayssincepro;

import java.util.Calendar;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;

import android.widget.Toast;
import android.app.NotificationChannel;

public class OnAlarmReceive extends BroadcastReceiver {

    Context context;
    SharedPreferences preferences;

    // resources do not exist with receiver
    // this string appears in the widget boxes
    String APP_NAME = "Days Since Pro 3"; // Resources.getSystem().getString(R.string.app_name);

    private double getPercent(String percentOption)
    {
        double percent = .75;

        switch (percentOption)
        {
            case "75 percent of days passed":  // better way to get array item?
                percent = .75;
                break;
            case "85 percent of days passed":
                percent = .85;
                break;
            case "95 percent of days passed":
                percent = .95;
                break;
            default:
                percent = 0.75;
                break;
        }

        return percent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        long eventID;
        double percent = 0.75;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        eventID = intent.getLongExtra("eventID", 0);

      //  Log.wtf("alarm", "Alarm Receive eventID is " + eventID);
        String percentOption = preferences.getString("remind_percent",
                context.getString(R.string.quarter_till));

        percent = getPercent(percentOption);

        SQLiteDatabase db = (new DatabaseHelper(context)).getWritableDatabase();

        SimpleDate now = new SimpleDate(new Date());
        String today = now.getDate(SimpleDate.DateStyle.YMD);
        String dateCondition = "date <= '" + today + "'";

        Cursor cursor;

        String sql;
        int id;
        String event;
        String usDate;
        long nEstDays;
        DaysSinceCalculations dsc1;

        if (eventID != 0) {
         //   showToast("ah, alarm received for specific id" + eventID);

            // see if needs an notification
            sql = "select _id, catID, event, date, recur, date(date, '+' || recur || ' day') as nextdate from event where "
                    + dateCondition + " and _id = '" + eventID + "'";

            cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst() == false) {
               // showToast(" ah alarm received for specific id " + eventID + " but rejected");

                return;
            }

            // the first time an eligible event is created it will be received.
            // so check the time if it is the same time go ahead.
            Calendar rightNow = Calendar.getInstance();
            int theHour = rightNow.get(Calendar.HOUR_OF_DAY);
            int theMinute = rightNow.get(Calendar.MINUTE);

            int notifyHour = Preferences.getPreferenceInt(context, APP_NAME,
                    "notify_hour_" + eventID);
            int notifyMinute = Preferences.getPreferenceInt(context, APP_NAME,
                    "notify_minute_" + eventID);

            if (theHour == notifyHour && theMinute == notifyMinute) {
                // OK.
            } else {
                // showToast("reject new event" + eventID + " " + notifyHour +
                // ":" + notifyMinute);
                return;
            }

            id = cursor.getInt(0);
            event = cursor.getString(2);
            usDate = cursor.getString(3); // date
            nEstDays = cursor.getLong(4); // recur
            dsc1 = new DaysSinceCalculations(usDate);

            if (nEstDays == 0 && dsc1.getDaysSinceEvent() == 0) {
                // one time today
                createNotification(id, event, 3, "");
            }

            if (nEstDays != 0) {

                if (dsc1.getDaysSinceEvent() == 0) {
                    // green
                    String todayExplain = event + " "
                            + context.getString(R.string.repeats_every) + " "
                            + nEstDays + " " + context.getString(R.string.days);

                    createNotification(id, event, 3, todayExplain);
                } else if (dsc1.getDaysSinceEvent() > nEstDays) {
                    // red - style 1 is number of days - for shortest.
                    createNotification(id, event, 1, dsc1.getExplain(true, 1));

                } else if (dsc1.getDaysSinceEvent() > nEstDays * percent) {
                    // yellow

                    long daysTill = (long) (dsc1.getDaysSinceEvent() - (nEstDays * percent));

                    // only send notification if less than a week left

                    if (daysTill <= 7) {

                        createNotification(id, event, 2, dsc1.getExplain(true, 1));
                    }
                }
            }

            return;
        }

        // make notification

        // showToast("Alarm received! for all!");

        sql = "select _id, catID, event, date, recur, date(date, '+' || recur || ' day') as nextdate from event where "
                + dateCondition;

        cursor = db.rawQuery(sql, null);

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {

            id = cursor.getInt(0);
            event = cursor.getString(2);
            usDate = cursor.getString(3); // date
            nEstDays = cursor.getLong(4); // recur
            dsc1 = new DaysSinceCalculations(usDate);

            // cross check with preference.
            boolean letItGo = false;

            int notifyHour = Preferences.getPreferenceInt(context, APP_NAME,
                    "notify_hour_" + id);
            int notifyMinute = Preferences.getPreferenceInt(context, APP_NAME,
                    "notify_minute_" + id);

            if (notifyHour == 0 && notifyMinute == 0) {
                // showToast("worthy generic notification " + id + " "
                // + notifyHour + ":" + notifyMinute);

                letItGo = false;
            } else {

                // showToast("reject notification " + id + " " + notifyHour +
                // ":"
                // + notifyMinute);

                letItGo = true;
            }

            if (letItGo) {
                cursor.moveToNext();
                continue;
            }

            if (nEstDays == 0 && dsc1.getDaysSinceEvent() == 0) {
                // one time today
                createNotification(id, event, 3, "");
            }


            if (nEstDays != 0) {

                if (dsc1.getDaysSinceEvent() == 0) {
                    // green
                    String todayExplain = event + " "
                            + context.getString(R.string.repeats_every) + " "
                            + nEstDays + " " + context.getString(R.string.days);

                    createNotification(id, event, 3, todayExplain);
                } else if (dsc1.getDaysSinceEvent() > nEstDays) {
                    // red - style 1 is number of days - for shortest.
                    createNotification(id, event, 1, dsc1.getExplain(true, 1));

                } else if (dsc1.getDaysSinceEvent() > nEstDays * percent) {
                    // yellow

                    long daysTill = (long) (dsc1.getDaysSinceEvent() - (nEstDays * percent));

                    // only send notification if less than a week left

                    if (daysTill <= 7) {

                        createNotification(id, event, 2, dsc1.getExplain(true, 1));
                    }
                }
            }
            cursor.moveToNext();
        }

        // showToast(rowCount + " rows!");

        cursor.close();
    }

    void showToast(String s) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show();
    }

    public void createNotification(int id, String event, int color,
                                   String explain) {

        Notification noti= null;
        String title = event;
        String text = event + " " + explain;

        int notificationId = id;
        String channelId = "dsp-01";
        String channelName = "DaySinceProChannel";
        int importance = NotificationManager.IMPORTANCE_HIGH;


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        switch (color) {
            case 3: // green
                title += " " + context.getString(R.string.is_today);

                text = explain;


                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                {
                    noti = new NotificationCompat.Builder(context)
                            .setContentTitle(title).setContentText(text)
                            .setSmallIcon(R.drawable.ic_green)
                            .build();
                }
                else
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        NotificationChannel mChannel = new NotificationChannel(
                                channelId, channelName, importance);
                        notificationManager.createNotificationChannel(mChannel);



                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(R.drawable.ic_today)
                                .setContentTitle(title)
                                .setContentText(text)
                                .setColor(context.getResources().getColor(R.color.holo_green_dark));

                        // some intent code

                        notificationManager.notify(notificationId, mBuilder.build());


                    }

                    else {
                        // Marshmallow, Nougat works fine with this

                        noti = new NotificationCompat.Builder(context)
                                .setContentTitle(title).setContentText(text)
                                .setSmallIcon(R.drawable.ic_today)
                                .setColor(context.getResources().getColor(R.color.holo_green_dark))
                                .build();
                    }
                }

                break;

            case 2: // yellow
                title += " " + context.getString(R.string.is_near_due);

                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                    noti = new NotificationCompat.Builder(context)
                            .setContentTitle(title).setContentText(text)
                            .setSmallIcon(R.drawable.ic_launcher)
                            // .setContentIntent(pIntent)
                            .build();
                }
                else
                {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        NotificationChannel mChannel = new NotificationChannel(
                                channelId, channelName, importance);
                        notificationManager.createNotificationChannel(mChannel);


                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(R.drawable.ic_near)
                                .setContentTitle(title)
                                .setContentText(text)
                                .setColor(context.getResources().getColor(R.color.holo_yellow));


                        // some intent code

                        notificationManager.notify(notificationId, mBuilder.build());

                    }
                    else {


                        noti = new NotificationCompat.Builder(context)
                                .setContentTitle(title).setContentText(text)
                                .setSmallIcon(R.drawable.ic_near)
                                .setColor(context.getResources().getColor(R.color.holo_yellow))
                                // .setContentIntent(pIntent)
                                .build();
                    }
                }
                break;

            default: // red

                title += " " + context.getString(R.string.is_due);

                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                    noti = new NotificationCompat.Builder(context)
                            .setContentTitle(title).setContentText(text)
                            .setSmallIcon(R.drawable.ic_red)
                            // .setContentIntent(pIntent)
                            .build();
                }
                else
                {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        NotificationChannel mChannel = new NotificationChannel(
                                channelId, channelName, importance);
                        notificationManager.createNotificationChannel(mChannel);


                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(R.drawable.ic_due)
                                .setContentTitle(title)
                                .setContentText(text)
                                .setColor(context.getResources().getColor(R.color.holo_red));

                        // some intent code

                        notificationManager.notify(notificationId, mBuilder.build());

                    }
                    else {

                        noti = new NotificationCompat.Builder(context)
                                .setContentTitle(title).setContentText(text)
                                .setSmallIcon(R.drawable.ic_due)
                                .setColor(context.getResources().getColor(R.color.holo_red))
                                // .setContentIntent(pIntent)
                                .build();
                    }

                }
        }

        // brilliant necessary thing(?) 2.2 will crash if you don't want intent
        // http://stackoverflow.com/questions/7040742/android-notification-manager-having-a-notification-without-an-intent

        // commit out this DaysSincePro will work only in Ice-cream sandwich and later
      //  noti.setLatestEventInfo(context, title, text,
      //          PendingIntent.getActivity(context, 0, new Intent(), 0));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {


            NotificationManager notificationManager2 = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            // hide the notification after its selected

            if (noti != null) {
                noti.flags |= Notification.FLAG_AUTO_CANCEL;

                notificationManager2.notify(id, noti);
            }
        }
    }

}
