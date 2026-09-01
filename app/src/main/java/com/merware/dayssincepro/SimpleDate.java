package com.merware.dayssincepro;

/* ****************************************************
 * Simple Date - Java have long had a Date class,
 * for no good reason the get methods are depreciated.
 * the proper way nowadays is to use SimpleDateFormat
 *
 * History -
 *
 * port from original C# DaysSinceApp 2/11/2011
 * support US/UK 2/11/2012
 * 2/12/2012 - change format YYYY-MM-DD for SQLLite.
 *
 * $Date: 2012-07-21 21:33:55 -0500 (Sat, 21 Jul 2012) $
 * $Rev: 87 $
 * Alex Mak
 * ******************************************************/

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class SimpleDate {

    public enum DateStyle {
        US, UK, YMD, MYD
    }

    int month;
    int day;
    int year;

    Date date;
    DateStyle style;

    SimpleDateFormat monthFormat = DateFormats.prolepticGregorian("MM");
    SimpleDateFormat dayFormat = DateFormats.prolepticGregorian("dd");
    SimpleDateFormat yearFormat = DateFormats.prolepticGregorian("yyyy");

    // m is for minutes, M is for month
    SimpleDateFormat formatter = DateFormats.prolepticGregorian("yyyy-MM-dd");

    // US
    public SimpleDate(String sDate) {

        style = DateStyle.US;
        month = 0;
        day = 0;
        year = 0;

        try {
            date = (Date) formatter.parse(sDate);
            // get the date parts of now.
            month = Integer.parseInt(monthFormat.format(date));
            day = Integer.parseInt(dayFormat.format(date));
            year = Integer.parseInt(yearFormat.format(date));
        } catch (ParseException e) {
            System.out.println("can't parse: " + e);
        }
    }

    // US and UK
    public SimpleDate(String sDate, DateStyle style) {

        // create a date from a string
        // m is for minutes, M is for month

        this.style = style;
        if (style == DateStyle.UK)
            formatter = DateFormats.prolepticGregorian("MM-dd-yyyy");

        month = 0;
        day = 0;
        year = 0;

        try {
            date = (Date) formatter.parse(sDate);

            // get the date parts of now.
            month = Integer.parseInt(monthFormat.format(date));
            day = Integer.parseInt(dayFormat.format(date));
            year = Integer.parseInt(yearFormat.format(date));
        } catch (ParseException e) {
            System.out.println("can't parse: " + e);
        }
    }

    public SimpleDate(Date date) {
        this.date = date;
        month = Integer.parseInt(monthFormat.format(date));
        day = Integer.parseInt(dayFormat.format(date));
        year = Integer.parseInt(yearFormat.format(date));
    }

    public SimpleDate(int y, int m, int d)
    {
        year = y;
        month = m;
        day = d;

        String str =  y + "-" + m + "-" + d;

        try {
            date = (Date) formatter.parse(str);
        }
        catch (ParseException e)
        {

        }
    }

    /**
     *
     * @return Month number: 1 is January
     */
    int getMonth() {
        return month;
    }

    int getDay() {
        return day;
    }

    int getYear() {
        return year;
    }

    DateStyle getStyle() {
        return style;
    }

    Date getDate() {
        return date;
    }

    public String toString() {
        return getDate(DateStyle.US);
    }

    String getDate(DateStyle style) {
        SimpleDateFormat sdf = null;
        switch (style) {
            case US:
                sdf = DateFormats.prolepticGregorian("MM-dd-yyyy");
                break;
            case UK:
                sdf = DateFormats.prolepticGregorian("dd-MM-yyyy");
                break;
            case YMD:
                sdf = DateFormats.prolepticGregorian("yyyy-MM-dd");
                break;
            case MYD:
                sdf = DateFormats.prolepticGregorian("MM-dd-yyyy");
                break;
        }
        return sdf.format(date);
    }

    String getDate2(DateStyle style) {
        SimpleDateFormat sdf = null;
        switch (style) {
            case US:
                sdf = DateFormats.prolepticGregorian("EEEE, MMMM dd, yyyy");
                break;
            case UK:
                sdf = DateFormats.prolepticGregorian("EEEE, dd MMMM, yyyy");
                break;
            case YMD:
                sdf = DateFormats.prolepticGregorian("EEEE, yyyy MMMM dd");
                break;
            case MYD:
                sdf = DateFormats.prolepticGregorian("EEEE, MMM, dd, yyyy");
                break;
        }
        return sdf.format(date);
    }
}
