package com.merware.dayssincepro;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
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
