package com.merware.dayssincepro;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.merware.dayssincepro.SimpleDate.DateStyle;

public class MyEventAdapter extends SimpleCursorAdapter {

    private Cursor c;
    private Context context;
    private String systemDateFormat;
    private TabKind kind;

    SharedPreferences preferences;
    int themeOption;

    // Advances cal by one recurrence interval. Standard intervals use calendar
    // fields (month/year) instead of a fixed day count, so leap years don't
    // cause the recurring date to drift away from its original month/day.
    private static void addRecurrenceInterval(Calendar cal, long nEstDays) {
        switch ((int) nEstDays) {
            case 30:
                cal.add(Calendar.MONTH, 1);
                break;
            case 90:
                cal.add(Calendar.MONTH, 3);
                break;
            case 180:
                cal.add(Calendar.MONTH, 6);
                break;
            case 365:
                cal.add(Calendar.YEAR, 1);
                break;
            default:
                cal.add(Calendar.DAY_OF_YEAR, (int) nEstDays);
                break;
        }
    }

    public MyEventAdapter(Context context, int layout, Cursor c, String[] from,
                          int[] to, String systemDateFormat, TabKind kind) {
        super(context, layout, c, from, to);

        this.context = context;
        this.c = c;
        this.kind = kind;

        this.systemDateFormat = systemDateFormat;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String sTheme = preferences.getString("theme", "0");
        themeOption = Integer.parseInt(sTheme);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.event_item, parent, false);
        TextView eventView = (TextView) rowView.findViewById(R.id.eventView);
        TextView dateView = (TextView) rowView.findViewById(R.id.dateView);
        TextView explainView = (TextView) rowView.findViewById(R.id.explain);
        c.moveToPosition(position);

        long nEstDays = c.getLong(4); // recur
        String sNextDate = c.getString(6);

        if (nEstDays == 0) { // distinct real future one time date
            if (themeOption == 1) // dark
                eventView.setTextColor(Color.WHITE);
            else
                eventView.setTextColor(Color.BLACK);
        }

        // ------------ first column -----------------
        eventView.setText(c.getString(2));
        String usDate = c.getString(3);
        DateStyle dateStyle = DateStyle.US;

        SimpleDate sd = new SimpleDate(usDate, SimpleDate.DateStyle.US);

        DaysSinceCalculations dsc;
        DaysSinceCalculations dsc1 = new DaysSinceCalculations(usDate);
        DaysSinceCalculations dsc2 = dsc1;
        String dayText;
        SimpleDate nextDate = sd;
        SimpleDate lastDate = sd; // most recent occurrence on or before today
        Calendar now = Calendar.getInstance();

        if (nEstDays != 0) {
            // Calculate the next scheduled recurrence date after today
            Calendar recurCal = Calendar.getInstance();
            recurCal.setTime(sd.getDate());
            Calendar nowCal = Calendar.getInstance();
            nowCal.setTime(now.getTime());

            // If the event date is in the future, use it
            if (recurCal.after(nowCal)) {
                nextDate = new SimpleDate(recurCal.getTime());
            } else {
                // Find the last occurrence on/before today, and the next occurrence after today
                Calendar lastCal = (Calendar) recurCal.clone();
                while (!recurCal.after(nowCal)) {
                    lastCal = (Calendar) recurCal.clone();
                    addRecurrenceInterval(recurCal, nEstDays);
                }
                nextDate = new SimpleDate(recurCal.getTime());
                lastDate = new SimpleDate(lastCal.getTime());
            }
            dsc2 = new DaysSinceCalculations(nextDate);
            dateView.setText(nextDate.getDate(dateStyle));
        }

        DaysSinceCalculations dsc3 = new DaysSinceCalculations(lastDate);

        if (kind == TabKind.DaysSince) {
            dsc = dsc1;
        } else if (kind == TabKind.SinceLast) {
            dsc = dsc3;
        } else {
            dsc = dsc2;

            // if event hasn't happened quite yet, use predetermined says till.

            if (now.getTime().before(sd.getDate())) {
                dsc = dsc1;
            }
        }

        boolean useNextDate = false;  // use next recurring date in days until

        if (sd.getDate().before(now.getTime()))
        {
            // good enough use this
            useNextDate = true;
         }

        // ----------------- 2nd column ----------------
        String usEndDate = c.getString(5);

        //Log.wtf("look", "event [" + c.getString(2) + "] end day ar " + usEndDate);

        if (themeOption == 1) // dark
            dateView.setTextColor(Color.WHITE);

        if (systemDateFormat == null) {
            if (kind == TabKind.DaysSince) {
                dayText = sd.getDate(DateStyle.US);
            }
            else if (kind == TabKind.SinceLast) {
                dayText = lastDate.getDate(DateStyle.US);
            }
            else {
                // Log.wtf("look", "ok no system date format what is useNextDate " + useNextDate);
                if (useNextDate) {
                    dayText = nextDate.getDate(DateStyle.US);
                }
                else
                {
                    dayText = sd.getDate(DateStyle.US);
                }
            }

        } else {
            if (systemDateFormat.equals(context.getString(R.string.us_date_style))) {
                dateStyle = DateStyle.YMD;
            } else if (systemDateFormat.equals(context.getString(R.string.uk_date_style))) {
                dateStyle = DateStyle.UK;
            } else {
                dateStyle = DateStyle.US;
            }

            if (kind == TabKind.DaysSince) {
                // Log.wtf("look", "days since ok what is useNextDate " + useNextDate);
                dayText = sd.getDate(dateStyle);
            }
            else if (kind == TabKind.SinceLast) {
                dayText = lastDate.getDate(dateStyle);
            }
            else {
                // Log.wtf("look", "days until ok what is useNextDate " + useNextDate);

                if (useNextDate) {
                    dayText = nextDate.getDate(dateStyle);
                }
                else {
                    dayText = sd.getDate(dateStyle);
                }
            }
        }
        DaysSinceCalculations dscStartToEnd = null;
        DaysSinceCalculations dscEnd = null;

        if (usEndDate != null)
        {
            dscStartToEnd = new DaysSinceCalculations(usDate, usEndDate);
            dscEnd = new DaysSinceCalculations(usEndDate);

            SimpleDate sdEndDate = new SimpleDate(usEndDate, SimpleDate.DateStyle.US);
            String endDateText = sdEndDate.getDate(dateStyle);
            dayText += " " + endDateText;
        }

        //Log.wtf("look", "dayText is " + dayText);
        dateView.setText(dayText);

        // ----------------- third column ----------------

        if (themeOption == 1) // dark
            explainView.setTextColor(Color.WHITE);

//        Log.wtf("3rd","dsc1 is " + dsc1);
//        Log.wtf("3rd","dsc2 is " + dsc2);

        String percentOption = preferences.getString("remind_percent",  context.getString(R.string.quarter_till));

        // Log.wtf("remind percent", percentOption);

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

        // Log.wtf("remind percent", "percent is :" + percent);

        if (nEstDays != 0) {
            if (dsc.getDaysSinceEvent() > nEstDays * percent) {
                explainView.setTextColor(Color.parseColor("#FF9C00")); // Color.YELLOW
            }
            if (dsc.getDaysSinceEvent() > nEstDays) {
                explainView.setTextColor(Color.RED);
            }

            if (dsc.getDaysSinceEvent() == 0) {
                explainView.setTextColor(context.getResources().getColor(R.color.holo_green_dark));
            }
        }

        String styleOption = preferences.getString("disp_style", "0");
        int nStyleOption = Integer.parseInt(styleOption);

        //  Log.wtf("adapter","look, useNextDate is " + useNextDate);

        if (kind == TabKind.DaysUntil && useNextDate) {
            explainView.setText(dsc2.getExplain(false, nStyleOption));
            //   Log.wtf("myeventadapter", "days until behold " + dsc.getExplain(false, nStyleOption));
        }
        else
        {
            StringBuffer sb = new StringBuffer();

            DaysSinceCalculations dscForExplain = (kind == TabKind.SinceLast) ? dsc3 : dsc1;

            if (dscStartToEnd == null) {
                sb.append(dscForExplain.getExplain(false, nStyleOption));  // start date (or last recurrence)
                explainView.setText(sb.toString());
            }
           else
            {
                // end day in the pass
               SimpleDate sdEndDate = new SimpleDate(usEndDate, DateStyle.US);
               boolean bothInPast = false;

                if (sdEndDate.getDate().before(now.getTime()) && sd.getDate().before(now.getTime()))
                {
                    bothInPast = true;
                }

                switch (kind)
                {
                    case DaysSince:
                       // if both in pass, use end date
                        if (bothInPast)
                            sb.append(dscEnd.getExplain(false, nStyleOption));
                        else
                            sb.append(dsc1.getExplain(false, nStyleOption));

                        sb.append("\n");
                        break;
                    case SinceLast:
                        if (bothInPast)
                            sb.append(dscEnd.getExplain(false, nStyleOption));
                        else
                            sb.append(dsc3.getExplain(false, nStyleOption));

                        sb.append("\n");
                        break;
                    case DaysUntil:
                        // since the start date, dsc2 is the recur date
                        sb.append(dsc1.getExplain(false, nStyleOption));
                        sb.append("\n");
                        break;
                }

                sb.append("(");

                long daysFromEvent = dscStartToEnd.getDaysSinceEvent();
                if (daysFromEvent == 0 || daysFromEvent == 1)  // avoid Yesterday or Today bogus explanation of short events
                    sb.append(dscStartToEnd.getExplain(false, 1));
                else
                    sb.append(dscStartToEnd.getExplain(false, nStyleOption));
                sb.append(")");
                explainView.setText(sb.toString());
            }
        }

        String option = preferences.getString("font_size", "16");
        int fontSize = Integer.parseInt(option);

        eventView.setTextSize(fontSize);
        dateView.setTextSize(fontSize - 2);
        explainView.setTextSize(fontSize - 2);

        return rowView;
    }
}