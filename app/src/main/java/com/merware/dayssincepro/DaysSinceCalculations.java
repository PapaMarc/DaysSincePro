package com.merware.dayssincepro;

/* ****************************************************
 * DaysSinceCalculations
 *
 * History -
 *
 * 2/11/2011 - port from original C# DaysSinceApp
 * 2/6/2012 - bug fix 2/6/2012
 * 2/12/2012 - change format YYYY-MM-DD for SQLLite.
 * 3/28/2012 - day light saving time bug fix
 *
 * Alex Mak
 * ******************************************************/

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DaysSinceCalculations {

    Date date;
    Date now;
    long nDaysSinceEvent = 0;
    long nWeekDaysSinceEvent = 0;

    boolean isFuture = false;
    boolean isToday = false;
    boolean isYesterday = false;
    boolean isTomorrow = false;

    String explanation;

    int month;
    int day;
    int year;

    String tomorrow;
    String today;
    String yesterday;
    String oneDay;
    String s_days;

    String s_day;
    String s_year;
    String s_years;
    String s_month;
    String s_months;
    String s_week;
    String s_weeks;

    String s_infuture;
    String s_inpast;

    String s_weekday;
    String s_weekdays;

    String s_is;
    String s_was;

    // m is for minutes, M is for month
    // Explicitly proleptic Gregorian (no Julian/Gregorian cutover): java.text.SimpleDateFormat
    // parses via a default GregorianCalendar whose implicit cutover (Oct 15, 1582) otherwise
    // interprets dates before that as Julian, silently mixing calendar systems in day-count
    // math for pre-1582 dates. Verified that GregorianCalendar.setGregorianChange() only has
    // an effect when applied to the calendar used for field<->millis conversion at parse time
    // (here) - applying it inside daysBetween()'s own Calendar objects (which only ever call
    // setTime()/getTimeInMillis(), never re-deriving fields) has no effect at all.
    SimpleDateFormat formatter = newProlepticGregorianFormatter("yyyy-MM-dd");

    private static SimpleDateFormat newProlepticGregorianFormatter(String pattern) {
        SimpleDateFormat fmt = new SimpleDateFormat(pattern);
        ((GregorianCalendar) fmt.getCalendar()).setGregorianChange(new Date(Long.MIN_VALUE));
        return fmt;
    }

    boolean isFrench = false;

    void setTerms() {
        String lang = Locale.getDefault().getLanguage();

        if (lang.equals("fr"))
            isFrench = true;

        if (isFrench) {
            tomorrow = "demain";
            today = "aujourd'hui";
            yesterday = "hier";
            oneDay = "1 jour";
            s_days = "jours";
            s_day = "jour";
            s_year = "an";
            s_years = "ans";
            s_month = "mois";
            s_months = "mois";
            s_week = "semaine";
            s_weeks = "semaines";
            s_weekday = "jour de la semaine ";
            s_weekdays = "jours de la semaine";
            s_is = ""; // "c'est";
            s_was = ""; // "c'était";

            s_infuture = "dans le futur.";
            s_inpast = "auparavant.";
        }
        else {
            tomorrow = "Tomorrow";
            today = "Today";
            yesterday = "Yesterday";
            oneDay = "1 day";
            s_days = "days";

            s_day = "day";
            s_year = "year";
            s_years = "years";
            s_month = "month";
            s_months = "months";
            s_week = "week";
            s_weeks = "weeks";

            s_weekday = "weekday";
            s_weekdays = "weekdays";

            s_is = "is";
            s_was = "was";

            s_infuture = "in future.";
            s_inpast = "ago.";
        }
    }

    public DaysSinceCalculations(SimpleDate sd) {

        date = sd.getDate();
        month = sd.getMonth();
        day = sd.getDay();
        year = sd.getYear();

        // now = new Date();
        // now should be midnight of today

        Calendar cal = Calendar.getInstance();
        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.YEAR));
        sb.append("-");
        sb.append(cal.get(Calendar.MONTH) + 1); // month is zero based.
        sb.append("-");
        sb.append(cal.get(Calendar.DAY_OF_MONTH));

        try {
            now = (Date) formatter.parse(sb.toString());

            nDaysSinceEvent = daysBetween(date, now);
            nWeekDaysSinceEvent = weekDaysBetween(date, now);

            if (nDaysSinceEvent < 0)
                isFuture = true;

            explain();
        } catch (ParseException e) {

        }
    }

    public DaysSinceCalculations(String sDate) {

        try {
            date = (Date) formatter.parse(sDate);
            //	Log.wtf("dsc", "date is " + sDate.toString());

            SimpleDate sd = new SimpleDate(sDate);

            month = sd.getMonth();
            day = sd.getDay();
            year = sd.getYear();

            // now = new Date();
            // now should be midnight of today

            Calendar cal = Calendar.getInstance();
            StringBuffer sb = new StringBuffer();
            sb.append(cal.get(Calendar.YEAR));
            sb.append("-");
            sb.append(cal.get(Calendar.MONTH) + 1); // month is zero based.
            sb.append("-");
            sb.append(cal.get(Calendar.DAY_OF_MONTH));

            now = (Date) formatter.parse(sb.toString());

            nDaysSinceEvent = daysBetween(date, now);
            nWeekDaysSinceEvent = weekDaysBetween(date, now);

            //	Log.wtf("dsa", "Look, man " + nDaysSinceEvent);

            if (nDaysSinceEvent < 0)
                isFuture = true;

            explain();

        } catch (ParseException e) {
            System.out.println("can't parse: " + e);
            e.printStackTrace();
        }

    }

    Date date2;

    // 2 date constructor, not use today but sDate2
    public DaysSinceCalculations(String sDate1, String sDate2) {

        try {
            date = (Date) formatter.parse(sDate1);

            SimpleDate sd = new SimpleDate(sDate1);

            month = sd.getMonth();
            day = sd.getDay();
            year = sd.getYear();

            date2 = (Date) formatter.parse(sDate2);

            nDaysSinceEvent = daysBetween(date, date2);
            nWeekDaysSinceEvent = weekDaysBetween(date, date2);

            if (nDaysSinceEvent < 0)
                isFuture = true;

            explain2();

        } catch (ParseException e) {
            // well too bad
        }
    }


    // based on excellent work by Paul Hill
    // http://user.xmission.com/~goodhill/dates/deltaDates.html
    public static final long MILLISECS_PER_MINUTE = 60 * 1000;
    public static final long MILLISECS_PER_HOUR = 60 * MILLISECS_PER_MINUTE;
    protected static final long MILLISECS_PER_DAY = 24 * MILLISECS_PER_HOUR;

    public static long daysBetween(Date d1, Date d2) {

        GregorianCalendar gc1 = new GregorianCalendar();
        GregorianCalendar gc2 = new GregorianCalendar();

        gc1.setTime(d1);
        gc2.setTime(d2);

        //	Log.wtf("dsa", "days between " + d1 + " and " + d2);

        long endL = gc2.getTimeInMillis()
                + gc2.getTimeZone().getOffset(gc2.getTimeInMillis());
        long startL = gc1.getTimeInMillis()
                + gc1.getTimeZone().getOffset(gc1.getTimeInMillis());
        return (endL - startL) / MILLISECS_PER_DAY;

    }

    private void explain() {
        StringBuffer sb = new StringBuffer();
        long nDaysBetween = Math.abs(nDaysSinceEvent);

        setTerms();

        if (nDaysBetween <= 31) {
            if (nDaysBetween == 0) {
                if (isFuture) {
                    sb.append(tomorrow);
                    isTomorrow = true;
                } else {
                    sb.append(today);
                    isToday = true;
                }
            } else if (nDaysBetween == 1) {
                if (isFuture) {
                    sb.append(oneDay);
                } else {
                    sb.append(yesterday);
                    isYesterday = true;
                }
            } else {
                sb.append(nDaysBetween);
                sb.append(" ");
                sb.append(s_days);
            }

            explanation = sb.toString();

            return;
        }

        // use field by field method
        // future date - present day

        // y1 m1 d1
        // - y0 m0 d0
        // -------------

        // borrow 1 year to make 12 months
        // borrow 1 month to make 30 days

        int y, m, d;

        int y1, y0;
        int d1, d0;
        int m1, m0;

        SimpleDate sd = new SimpleDate(now);

        int nowMonth = sd.getMonth();
        int nowDay = sd.getDay();
        int nowYear = sd.getYear();

        if (isFuture) {
            y1 = year;
            y0 = nowYear;
            m1 = month;
            m0 = nowMonth;
            d1 = day;
            d0 = nowDay;
        } else {
            y1 = nowYear;
            y0 = year;
            m1 = nowMonth;
            m0 = month;
            d1 = nowDay;
            d0 = day;
        }

        if (d1 < d0) {
            d1 += 30;
            m1 -= 1;
        }

        d = d1 - d0;

        if (m1 < m0) {
            m1 += 12;
            y1 -= 1;
        }

        m = m1 - m0;

        y = y1 - y0;

        if (y > 0) {
            //	if (isFrench)
            //		sb.append("ll y a "); // there are

            sb.append(y);
            sb.append(" ");

            if (y > 1) {
                sb.append(s_years);
            } else {
                sb.append(s_year);
            }
        }
        if (m > 0) {
            if (y > 0) {
                if (isFrench)
                    sb.append(",");
                sb.append(" ");

            }
            sb.append(m);

            sb.append(" ");
            if (m > 1)
                sb.append(s_months);
            else
                sb.append(s_month);
        }
        if (d > 0) {
            if (y == 0 && m == 0 && d == 1) {
                if (isFuture)
                    sb.append(tomorrow);
                else
                    sb.append(yesterday);
            }

            else {
                if (m > 0 || y > 0)
                    sb.append(" ");
                sb.append(d);
                sb.append(" ");
                if (d > 1)
                    sb.append(s_days);
                else
                    sb.append(s_day);
            }
        }

        if (y == 0 && m == 0 && d == 0)
            sb.append(today);

        explanation = sb.toString();
    }

    private void explain2() {
        StringBuffer sb = new StringBuffer();
        long nDaysBetween = Math.abs(nDaysSinceEvent);

        setTerms();

        if (nDaysBetween <= 31) {
            if (nDaysBetween == 0) {
                if (isFuture) {
                    sb.append(tomorrow);
                    isTomorrow = true;
                } else {
                    sb.append(today);
                    isToday = true;
                }
            } else if (nDaysBetween == 1) {
                if (isFuture) {
                    sb.append(oneDay);
                } else {
                    sb.append(yesterday);
                    isYesterday = true;
                }
            } else {
                sb.append(nDaysBetween);
                sb.append(" ");
                sb.append(s_days);
            }

            explanation = sb.toString();
            return;
        }


        // use field by field method
        // future date - present day

        // y1 m1 d1
        // - y0 m0 d0
        // -------------

        // borrow 1 year to make 12 months
        // borrow 1 month to make 30 days

        int y, m, d;

        int y1, y0;
        int d1, d0;
        int m1, m0;

        SimpleDate sd = new SimpleDate(date2);  // <------------------  only difference

        int nowMonth = sd.getMonth();
        int nowDay = sd.getDay();
        int nowYear = sd.getYear();

        if (isFuture) {
            y1 = year;
            y0 = nowYear;
            m1 = month;
            m0 = nowMonth;
            d1 = day;
            d0 = nowDay;
        } else {
            y1 = nowYear;
            y0 = year;
            m1 = nowMonth;
            m0 = month;
            d1 = nowDay;
            d0 = day;
        }

        if (d1 < d0) {
            d1 += 30;
            m1 -= 1;
        }

        d = d1 - d0;

        if (m1 < m0) {
            m1 += 12;
            y1 -= 1;
        }

        m = m1 - m0;

        y = y1 - y0;

        if (y > 0) {
            //	if (isFrench)
            //		sb.append("ll y a "); // there are

            sb.append(y);
            sb.append(" ");

            if (y > 1) {
                sb.append(s_years);
            } else {
                sb.append(s_year);
            }
        }
        if (m > 0) {
            if (y > 0) {
                if (isFrench)
                    sb.append(",");
                sb.append(" ");

            }
            sb.append(m);

            sb.append(" ");
            if (m > 1)
                sb.append(s_months);
            else
                sb.append(s_month);
        }
        if (d > 0) {
            if (y == 0 && m == 0 && d == 1) {
                if (isFuture)
                    sb.append(tomorrow);
                else
                    sb.append(yesterday);
            }

            else {
                if (m > 0 || y > 0)
                    sb.append(" ");
                sb.append(d);
                sb.append(" ");
                if (d > 1)
                    sb.append(s_days);
                else
                    sb.append(s_day);
            }
        }

        if (y == 0 && m == 0 && d == 0)
            sb.append(today);

        explanation = sb.toString();
    }

    public long getDaysSinceEvent() {
        return nDaysSinceEvent;
    }

    public String getDaysAsString() {
        return String.valueOf(Math.abs(nDaysSinceEvent));
    }

    public String getWeekDaysAsString() {
        return String.valueOf(Math.abs(nWeekDaysSinceEvent));
    }

    public String toString() {
        return explanation;
    }

    public String getExplain(boolean bWasAgo, int styleOption) {
        String toReturn = new String();

        if (bWasAgo) {
            if (isFuture || isToday) {
                toReturn += s_is + " ";
            } else {
                toReturn += s_was + " ";
            }
        }

        String suffix = " " + s_days;

        switch (styleOption) {
            case 0:
                toReturn += explanation;
                if (isTomorrow || isToday || isYesterday) {
                    // toReturn += ".";
                    return toReturn;
                }

                break;
            case 1:
                if (getDaysAsString().equals("1")) {
                    suffix = " " + s_day;
                }

                toReturn += getDaysAsString() + suffix;
                break;
            case 2:
                toReturn += getWeekDaysAsString() + " " + s_weekdays;
                break;
            case 3:
                String suffix2 = " " + s_weeks;

                long weeks = Math.abs(nDaysSinceEvent) / 7;
                long daysRemain = Math.abs(nDaysSinceEvent) % 7;

                if (weeks == 0 && daysRemain == 0) {
                    toReturn += today;
                    break;
                }

                if (weeks == 1)
                    suffix2 = " " + s_week;

                if (daysRemain == 1)
                    suffix = " " + s_day;

                if (weeks == 0) {
                    toReturn += daysRemain + suffix;
                    break;
                }

                toReturn += weeks + suffix2;

                if (daysRemain != 0)
                    toReturn += " " + daysRemain + suffix;

                break;
        }

        if (bWasAgo) {
            if (isFuture) {
                toReturn += " " + s_infuture;
            } else
                toReturn += " " + s_inpast;

        }
        return toReturn;
    }

	/*
	 * based on
	 * http://stackoverflow.com/questions/4600034/calculate-number-of-weekdays
	 * -between-two-dates-in-java
	 */

    public long weekDaysBetween(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        int w1 = c1.get(Calendar.DAY_OF_WEEK);
        c1.add(Calendar.DAY_OF_WEEK, -w1);

        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        int w2 = c2.get(Calendar.DAY_OF_WEEK);
        c2.add(Calendar.DAY_OF_WEEK, -w2);

        // end Saturday to start Saturday
        long days = (c2.getTimeInMillis() - c1.getTimeInMillis())
                / (1000 * 60 * 60 * 24);
        long daysWithoutSunday = days - (days * 2 / 7);

        return daysWithoutSunday - w1 + w2;
    }

    public long getWeekDaysSinceEvent() {
        return nWeekDaysSinceEvent;
    }

}
