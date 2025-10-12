package com.alexcmak.dayssincepro;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import com.alexcmak.dayssincepro.SimpleDate.DateStyle;

public class DaysSinceAppWidgetProvider extends AppWidgetProvider {

    protected SQLiteDatabase db;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        String APP_NAME = context.getResources().getString(R.string.app_name);
        db = (new DatabaseHelper(context)).getWritableDatabase();

        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this
        // provider

        // Log.wtf("widget", "provider on update N is" + N + " lets go for loop");


        for (int i = 0; i < N; i++) {

          //  Log.wtf("widget", "for loop i is " + i);


            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, ConfigWidgetActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(
                        context, appWidgetId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            }else {
                pendingIntent = PendingIntent.getActivity(
                        context, appWidgetId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
            }

            // Get the layout for the App Widget and attach an on-click listener
            // to the button

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget1);
            views.setOnClickPendingIntent(R.id.widget1label, pendingIntent);

            // default blue
            views.setInt(R.id.widgetlayout, "setBackgroundResource",
                    R.drawable.background);

            // To update a label
            // views.setTextViewText(R.id.widget1label, df.format(new Date()));
            // Tell the AppWidgetManager to perform an update on the current app
            // widget

            String dataToShow = context.getString(R.string.touch_here);

            String eventName = Preferences.getPreferenceString(context,
                    APP_NAME, "widget" + appWidgetId);

           // Log.wtf("widget", "event name is " + eventName);
           // Log.wtf("widget", "widget id is " + appWidgetId);

            int eventColor = Preferences.getPreferenceInt(context, APP_NAME,
                    "widgetColor" + appWidgetId);

            int isOpaque = Preferences.getPreferenceInt(context, APP_NAME,
                    "widgetOpaque" + appWidgetId);

            // replace single quotes
            String sqlEventName = eventName.replace("'", "\'\'");

            if (eventName.length() > 0) {

                // look up the event in preference, problem with there are dup
                // names.

                try {
                    Cursor cursor = db.query("event", /* table */
                            new String[] { "date" }, /* columns */
                            "event='" + sqlEventName + "'", null, null, null,
                            null);

                    // dataToShow = "Look" + cursor.getCount();

                    if (cursor.getCount() >= 0) {
                        cursor.moveToFirst();

                        dataToShow = eventName + " ";

                        String dateFromDatabase = cursor.getString(0);
                        String formattedDate = dateFromDatabase;

                        // style date depending on US/UK - request by reviewer

                        String systemDateFormat = DateFormat.GetSystemDateFormat(context);

                        SimpleDate sd = new SimpleDate(dateFromDatabase, SimpleDate.DateStyle.US);

                        if (systemDateFormat.equals("Day-Month-Year")) {
                            formattedDate = sd.getDate(DateStyle.UK);
                        }
                        else {
                            formattedDate = sd.getDate(DateStyle.US);
                        }

                        dataToShow += formattedDate;
                        
                        DaysSinceCalculations dsc = new DaysSinceCalculations(
                                cursor.getString(0));

                        int nStyleOption = Preferences.getPreferenceInt(
                                context, APP_NAME, "widgetStyle" + appWidgetId);

                        dataToShow += " ";
                        dataToShow += dsc.getExplain(true, nStyleOption);

                    }
                    cursor.close();

                } catch (Exception e) {
                    Log.wtf("widget", e.getMessage());
                    //dataToShow = eventName + " " + context.getString(R.string.not_found)  + " " + e.getMessage();
                    dataToShow = eventName + " " + context.getString(R.string.not_found);
                }

            }

            views.setTextViewText(R.id.widget1label, dataToShow);

            if (isOpaque == 1) {
                switch (eventColor) {
                    case 0:
                        // clear is um white?
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.clear_solid);
                        break;
                    case 1:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.blue_solid);
                        break;
                    case 2:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.red_solid);
                        break;
                    case 3:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.pink_solid);
                        break;
                    case 4:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.orange_solid);
                        break;
                    case 5:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.green_solid);
                        break;
                }

            } else {

                switch (eventColor) {
                    case 0:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.clear);
                        break;
                    case 1:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.background);
                        break;
                    case 2:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.red);
                        break;
                    case 3:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.pink);
                        break;
                    case 4:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.orange);
                        break;
                    case 5:
                        views.setInt(R.id.widgetlayout, "setBackgroundResource",
                                R.drawable.green);
                        break;

                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);

            // the database needs to remain open for the provider to return for update
            // db.close();

        }

    }
}
