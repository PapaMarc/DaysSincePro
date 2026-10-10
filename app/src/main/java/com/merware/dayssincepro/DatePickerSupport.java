package com.merware.dayssincepro;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Shared date selection rules and UTC date conversions for the date-picker call sites.
 */
final class DatePickerSupport {

    private DatePickerSupport() {
    }

    static final int MIN_DATE_YEAR = 1;
    static final int MAX_DATE_YEAR = 9999;
    static final long MIN_DATE_UTC_MILLIS = utcMillis(MIN_DATE_YEAR, Calendar.JANUARY, 1);

    /** Builds a UTC-normalized millis value for the given calendar fields (0-based month). */
    static long utcMillis(int year, int month, int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month, day);
        return cal.getTimeInMillis();
    }

    /**
     * Builds a zero-padded ISO-8601 "yyyy-MM-dd" date string for DB storage and SQL
     * comparisons. Always zero-pads the year to 4 digits - hand-rolled string
     * concatenation (e.g. {@code year + "-" + month + "-" + day}) silently produced
     * un-padded years like "45-01-15", which broke lexicographic date comparisons/sorts
     * (e.g. "date &lt;= 'today'") for years below 1000. Use this everywhere an ISO date
     * string is built from separate year/month/day fields, instead of ad hoc concatenation.
     *
     * @param month 0-based (java.util.Calendar convention, e.g. Calendar.JANUARY == 0)
     */
    static String isoDateString(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
    }

    static boolean isSelectableYear(int year, boolean pastOnly, int currentYear) {
        return year >= MIN_DATE_YEAR
                && year <= MAX_DATE_YEAR
                && (!pastOnly || year <= currentYear);
    }

    static boolean isSelectableDate(long dateUtcMillis, boolean pastOnly, long todayUtcMillis) {
        return !pastOnly || dateUtcMillis <= todayUtcMillis;
    }

    /** Converts a UTC-midnight selection into UTC calendar fields. */
    static Calendar toUtcCalendar(long selectionUtcMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(selectionUtcMillis);
        return cal;
    }
}
