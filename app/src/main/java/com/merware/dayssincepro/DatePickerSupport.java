package com.merware.dayssincepro;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Shared MaterialDatePicker configuration for DaysDiffActivity, EditEventActivity, and
 * EditHistory - keeps the minimum-selectable-date floor identical across all 5 call sites
 * per DCR_evolveToMaterialDatePicker.md §7.4/§2.
 */
final class DatePickerSupport {

    private DatePickerSupport() {
    }

    static final long MIN_DATE_UTC_MILLIS = utcMillis(1, Calendar.JANUARY, 1);

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
     * (e.g. "date &lt;= 'today'") once dates before ~year 1000 became enterable via
     * MaterialDatePicker. Use this everywhere an ISO date string is built from separate
     * year/month/day fields, instead of ad hoc concatenation.
     *
     * @param month 0-based (java.util.Calendar convention, e.g. Calendar.JANUARY == 0)
     */
    static String isoDateString(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
    }

    static MaterialDatePicker<Long> newPicker(long initialSelectionUtcMillis) {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(MIN_DATE_UTC_MILLIS)
                .setOpenAt(initialSelectionUtcMillis)
                .build();

        return MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(constraints)
                .setSelection(initialSelectionUtcMillis)
                .build();
    }

    /** Converts a MaterialDatePicker UTC-midnight selection into UTC calendar fields. */
    static Calendar toUtcCalendar(long selectionUtcMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(selectionUtcMillis);
        return cal;
    }
}
