package com.merware.dayssincepro;

import java.util.Calendar;

/**
 * Cycle-aware recurrence math shared by the list-view adapter (MyEventAdapter) and the
 * notification pipeline (OnAlarmReceive), so both agree on which occurrence of a
 * recurring event is "current" as of a given moment, instead of maintaining two
 * independent implementations that can silently diverge.
 */
final class RecurrenceCycle {

    private RecurrenceCycle() {
    }

    /** The most recent occurrence on/before {@code now}, and the next occurrence strictly after it. */
    static final class Occurrences {
        final SimpleDate lastOccurrence;
        final SimpleDate nextOccurrence;

        Occurrences(SimpleDate lastOccurrence, SimpleDate nextOccurrence) {
            this.lastOccurrence = lastOccurrence;
            this.nextOccurrence = nextOccurrence;
        }
    }

    // Advances cal by one recurrence interval. Month/year-based intervals use calendar
    // fields instead of a fixed day count, so leap years don't cause the recurring date
    // to drift away from its original month/day; week-based intervals (7/14) are exact
    // day counts already, so the default day-based advance is correct for them too.
    static void addRecurrenceInterval(Calendar cal, long nEstDays) {
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

    /**
     * Computes the most recent occurrence on/before {@code now} and the next occurrence
     * strictly after it, for a recurring event. For non-recurring events (nEstDays == 0),
     * or when the event's original date is still in the future, both results equal the
     * event's original date.
     */
    static Occurrences computeOccurrences(SimpleDate eventDate, long nEstDays, Calendar now) {
        if (nEstDays == 0) {
            return new Occurrences(eventDate, eventDate);
        }

        Calendar recurCal = Calendar.getInstance();
        recurCal.setTime(eventDate.getDate());
        Calendar nowCal = (Calendar) now.clone();

        if (recurCal.after(nowCal)) {
            return new Occurrences(eventDate, eventDate);
        }

        Calendar lastCal = (Calendar) recurCal.clone();
        while (!recurCal.after(nowCal)) {
            lastCal = (Calendar) recurCal.clone();
            addRecurrenceInterval(recurCal, nEstDays);
        }
        return new Occurrences(new SimpleDate(lastCal.getTime()), new SimpleDate(recurCal.getTime()));
    }
}
