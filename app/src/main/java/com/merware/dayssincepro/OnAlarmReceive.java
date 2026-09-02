package com.merware.dayssincepro;

import java.util.Calendar;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
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

    /** Notification urgency for a single event, evaluated relative to its current recurrence cycle. */
    enum Urgency { NONE, GREEN, YELLOW, RED }

    /**
     * Determines notification urgency from days elapsed since an event's most recent
     * occurrence, relative to its recurrence interval. Pure function (no Android/DB
     * dependencies), extracted so cycle-aware urgency can be verified without a real
     * alarm/notification pipeline - see OnAlarmReceiveUrgencyTest.
     */
    static Urgency computeUrgency(long daysSinceLastOccurrence, long nEstDays, double percent) {
        if (nEstDays == 0) {
            return daysSinceLastOccurrence == 0 ? Urgency.GREEN : Urgency.NONE;
        }
        if (daysSinceLastOccurrence == 0) {
            return Urgency.GREEN;
        }
        if (daysSinceLastOccurrence > nEstDays) {
            return Urgency.RED;
        }
        if (daysSinceLastOccurrence > nEstDays * percent) {
            long daysTill = (long) (daysSinceLastOccurrence - (nEstDays * percent));
            if (daysTill <= 7) {
                return Urgency.YELLOW;
            }
        }
        return Urgency.NONE;
    }

    /**
     * Builds day-count calculations relative to the event's most recent recurrence (its
     * "last occurrence"), not its original stored date - this is the fix for the
     * perpetual-overdue bug, where a years-old recurring event was always treated as
     * overdue relative to when it was first created, rather than its current cycle.
     */
    static DaysSinceCalculations currentCycleCalculations(String usDate, long nEstDays) {
        RecurrenceCycle.Occurrences occurrences = currentCycleOccurrences(
                usDate, nEstDays, Calendar.getInstance());
        return new DaysSinceCalculations(occurrences.lastOccurrence);
    }

    static RecurrenceCycle.Occurrences currentCycleOccurrences(String usDate, long nEstDays,
                                                               Calendar now) {
        SimpleDate sd = new SimpleDate(usDate, SimpleDate.DateStyle.US);
        return RecurrenceCycle.computeOccurrences(sd, nEstDays, now);
    }

    static boolean alreadyNotifiedInCurrentCycle(String lastNotifiedDate,
                                                 SimpleDate lastOccurrence,
                                                 SimpleDate nextOccurrence,
                                                 long nEstDays,
                                                 String today) {
        if (lastNotifiedDate == null || lastNotifiedDate.trim().length() == 0) {
            return false;
        }

        if (nEstDays == 0) {
            return today.equals(lastNotifiedDate);
        }

        try {
            SimpleDate notified = new SimpleDate(lastNotifiedDate, SimpleDate.DateStyle.US);
            return !notified.getDate().before(lastOccurrence.getDate())
                    && notified.getDate().before(nextOccurrence.getDate());
        } catch (Exception ignored) {
            return false;
        }
    }

    private void markEventNotified(SQLiteDatabase db, int id, String today) {
        ContentValues values = new ContentValues();
        values.put("last_notified_date", today);
        db.update("event", values, "_id = " + id, null);
    }

    private boolean dispatchNotificationForUrgency(int id, String event, long nEstDays,
                                                   DaysSinceCalculations dsc,
                                                   Urgency urgency) {
        switch (urgency) {
            case GREEN:
                if (nEstDays == 0) {
                    createNotification(id, event, 3, "");
                } else {
                    String todayExplain = event + " "
                            + context.getString(R.string.repeats_every) + " "
                            + nEstDays + " " + context.getString(R.string.days);
                    createNotification(id, event, 3, todayExplain);
                }
                return true;
            case RED:
                // style 1 is number of days - for shortest.
                createNotification(id, event, 1, dsc.getExplain(true, 1));
                return true;
            case YELLOW:
                createNotification(id, event, 2, dsc.getExplain(true, 1));
                return true;
            case NONE:
            default:
                return false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        long eventID;
        double percent = 0.75;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        eventID = intent.getLongExtra("eventID", 0);
        boolean isManualReview = intent.getBooleanExtra("manual_review", false);

      //  Log.wtf("alarm", "Alarm Receive eventID is " + eventID);
        String percentOption = preferences.getString("remind_percent",
                context.getString(R.string.quarter_till));

        percent = getPercent(percentOption);

        SQLiteDatabase db = DatabaseHelper.getInstance(context).getWritableDatabase();

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
                sql = "select _id, catID, event, date, recur, last_notified_date from event where "
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
            String lastNotifiedDate = cursor.getString(5);

            Calendar nowCal = Calendar.getInstance();
            RecurrenceCycle.Occurrences occurrences = currentCycleOccurrences(usDate, nEstDays, nowCal);
            dsc1 = new DaysSinceCalculations(occurrences.lastOccurrence);

            if (!isManualReview && alreadyNotifiedInCurrentCycle(lastNotifiedDate,
                    occurrences.lastOccurrence, occurrences.nextOccurrence, nEstDays, today)) {
                return;
            }

            Urgency urgency = computeUrgency(dsc1.getDaysSinceEvent(), nEstDays, percent);
            if (dispatchNotificationForUrgency(id, event, nEstDays, dsc1, urgency)) {
                markEventNotified(db, id, today);
            }

            return;
        }

        // make notification

        // showToast("Alarm received! for all!");
        int notificationCount = 0;

        sql = "select _id, catID, event, date, recur, last_notified_date from event where "
                + dateCondition;

        cursor = db.rawQuery(sql, null);

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {

            id = cursor.getInt(0);
            event = cursor.getString(2);
            usDate = cursor.getString(3); // date
            nEstDays = cursor.getLong(4); // recur
            String lastNotifiedDate = cursor.getString(5);

            Calendar nowCal = Calendar.getInstance();
            RecurrenceCycle.Occurrences occurrences = currentCycleOccurrences(usDate, nEstDays, nowCal);
            dsc1 = new DaysSinceCalculations(occurrences.lastOccurrence);

            if (!isManualReview && alreadyNotifiedInCurrentCycle(lastNotifiedDate,
                    occurrences.lastOccurrence, occurrences.nextOccurrence, nEstDays, today)) {
                cursor.moveToNext();
                continue;
            }

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

            Urgency urgency = computeUrgency(dsc1.getDaysSinceEvent(), nEstDays, percent);
            if (dispatchNotificationForUrgency(id, event, nEstDays, dsc1, urgency)) {
                markEventNotified(db, id, today);
                notificationCount++;
            }
            cursor.moveToNext();
        }

        // showToast(rowCount + " rows!");

        cursor.close();

        if (notificationCount > 0) {
            if (NotificationPermissionHelper.areNotificationsEnabled(context)) {
                showToast(context.getString(R.string.review_notifications_created));
            } else {
                showToast(context.getString(R.string.review_notifications_blocked));
            }
        } else {
            showToast(context.getString(R.string.review_no_events));
        }
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
