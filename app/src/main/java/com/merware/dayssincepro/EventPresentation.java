package com.merware.dayssincepro;

import android.content.Context;
import android.database.Cursor;

import java.util.Calendar;

import com.merware.dayssincepro.SimpleDate.DateStyle;

/**
 * Shared event-date semantics for row rendering and future sort-key calculation.
 */
final class EventPresentation {

    final long recurrenceDays;
    final String plannedDate;
    final String startDateText;
    final String endDateText;
    final SimpleDate startDate;
    final SimpleDate plannedFutureDate;
    final SimpleDate nextDate;
    final SimpleDate sinceLastDate;
    final boolean hasPlannedFutureDate;
    final boolean useNextDate;
    final DaysSinceCalculations startCalculation;
    final DaysSinceCalculations nextCalculation;
    final DaysSinceCalculations sinceLastCalculation;
    final DaysSinceCalculations plannedCalculation;
    final DaysSinceCalculations untilNextCalculation;
    final EventTimeline.Snapshot timeline;

    private EventPresentation(long recurrenceDays,
                              String plannedDate,
                              String startDateText,
                              String endDateText,
                              SimpleDate startDate,
                              SimpleDate plannedFutureDate,
                              SimpleDate nextDate,
                              SimpleDate sinceLastDate,
                              boolean hasPlannedFutureDate,
                              boolean useNextDate,
                              DaysSinceCalculations startCalculation,
                              DaysSinceCalculations nextCalculation,
                              DaysSinceCalculations sinceLastCalculation,
                              DaysSinceCalculations plannedCalculation,
                              DaysSinceCalculations untilNextCalculation,
                              EventTimeline.Snapshot timeline) {
        this.recurrenceDays = recurrenceDays;
        this.plannedDate = plannedDate;
        this.startDateText = startDateText;
        this.endDateText = endDateText;
        this.startDate = startDate;
        this.plannedFutureDate = plannedFutureDate;
        this.nextDate = nextDate;
        this.sinceLastDate = sinceLastDate;
        this.hasPlannedFutureDate = hasPlannedFutureDate;
        this.useNextDate = useNextDate;
        this.startCalculation = startCalculation;
        this.nextCalculation = nextCalculation;
        this.sinceLastCalculation = sinceLastCalculation;
        this.plannedCalculation = plannedCalculation;
        this.untilNextCalculation = untilNextCalculation;
        this.timeline = timeline;
    }

    static EventPresentation fromCursor(Context context, Cursor cursor, Calendar now) {
        long recurrenceDays = cursor.getLong(4);
        String plannedDate = cursor.getString(8);
        String startDateText = cursor.getString(3);
        String endDateText = cursor.getString(5);
        int lastHappenedDateCol = cursor.getColumnIndex("last_happened_date");
        String lastHappenedDate = lastHappenedDateCol >= 0
                ? cursor.getString(lastHappenedDateCol) : null;
        int lastOffTimeDateCol = cursor.getColumnIndex("last_off_time_date");
        String lastOffTimeDate = lastOffTimeDateCol >= 0
                ? cursor.getString(lastOffTimeDateCol) : null;
        return compute(context, startDateText, endDateText, recurrenceDays, plannedDate,
                lastHappenedDate, lastOffTimeDate, now);
    }

    static EventPresentation compute(Context context,
                                     String startDateText,
                                     String endDateText,
                                     long recurrenceDays,
                                     String plannedDate,
                                     String lastHappenedDate,
                                     String lastOffTimeDate,
                                     Calendar now) {
        SimpleDate startDate = new SimpleDate(startDateText, DateStyle.US);
        SimpleDate plannedFutureDate = null;
        boolean hasPlannedFutureDate = false;
        if (recurrenceDays == 0 && plannedDate != null && plannedDate.length() > 0
                && plannedDate.compareTo(HistoryDateRules.todayIsoDate()) > 0) {
            plannedFutureDate = new SimpleDate(plannedDate, DateStyle.US);
            hasPlannedFutureDate = true;
        }

        SimpleDate latestHistory = lastHappenedDate == null
                ? null : new SimpleDate(lastHappenedDate, DateStyle.US);
        SimpleDate latestOffTime = lastOffTimeDate == null
                ? null : new SimpleDate(lastOffTimeDate, DateStyle.US);
        EventTimeline.Snapshot timeline = EventTimeline.compute(
                startDate, endDateText, recurrenceDays, latestHistory, latestOffTime, now);

        SimpleDate sinceLastDate = timeline.sinceLastReferenceDate;
        if (recurrenceDays == 0 && latestHistory != null) {
            sinceLastDate = latestHistory;
        }

        SimpleDate nextDate = timeline.nextOccurrence;
        boolean useNextDate = timeline.hasFutureOccurrence
                && startDate.getDate().before(now.getTime());

        DaysSinceCalculations startCalculation =
                new DaysSinceCalculations(context, startDateText);
        DaysSinceCalculations nextCalculation =
                recurrenceDays == 0
                        ? startCalculation
                        : new DaysSinceCalculations(context, nextDate);
        DaysSinceCalculations sinceLastCalculation =
                new DaysSinceCalculations(context, sinceLastDate);
        DaysSinceCalculations plannedCalculation =
                hasPlannedFutureDate
                        ? new DaysSinceCalculations(context, plannedDate)
                        : nextCalculation;
        DaysSinceCalculations untilNextCalculation =
                hasPlannedFutureDate
                        ? plannedCalculation
                        : useNextDate
                        ? nextCalculation
                        : startCalculation;

        return new EventPresentation(
                recurrenceDays,
                plannedDate,
                startDateText,
                endDateText,
                startDate,
                plannedFutureDate,
                nextDate,
                sinceLastDate,
                hasPlannedFutureDate,
                useNextDate,
                startCalculation,
                nextCalculation,
                sinceLastCalculation,
                plannedCalculation,
                untilNextCalculation,
                timeline);
    }

    SimpleDate untilNextDisplayDate() {
        if (hasPlannedFutureDate) {
            return plannedFutureDate;
        }
        if (useNextDate) {
            return nextDate;
        }
        return startDate;
    }

    long daysSinceValue() {
        return startCalculation.getDaysSinceEvent();
    }

    long sinceLastValue() {
        return sinceLastCalculation.getDaysSinceEvent();
    }

    long untilNextValue() {
        return Math.max(0, -untilNextCalculation.getDaysSinceEvent());
    }
}
