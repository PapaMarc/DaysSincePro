package com.merware.dayssincepro;

import java.util.Calendar;

/**
 * Centralized date semantics for list tabs. It keeps recurrence-cycle math and optional
 * end-date caps in one place so Days Since / Since Last / Until Next don't drift apart.
 */
final class EventTimeline {

    private EventTimeline() {
    }

    static final class Snapshot {
        final SimpleDate startDate;
        final SimpleDate endDate;
        final boolean hasEndDate;

        final SimpleDate lastOccurrence;
        final SimpleDate nextOccurrence;

        final SimpleDate daysSinceReferenceDate;
        final SimpleDate sinceLastReferenceDate;

        final boolean hasFutureOccurrence;
        final SimpleDate untilNextReferenceDate;

        Snapshot(SimpleDate startDate,
                 SimpleDate endDate,
                 boolean hasEndDate,
                 SimpleDate lastOccurrence,
                 SimpleDate nextOccurrence,
                 SimpleDate daysSinceReferenceDate,
                 SimpleDate sinceLastReferenceDate,
                 boolean hasFutureOccurrence,
                 SimpleDate untilNextReferenceDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.hasEndDate = hasEndDate;
            this.lastOccurrence = lastOccurrence;
            this.nextOccurrence = nextOccurrence;
            this.daysSinceReferenceDate = daysSinceReferenceDate;
            this.sinceLastReferenceDate = sinceLastReferenceDate;
            this.hasFutureOccurrence = hasFutureOccurrence;
            this.untilNextReferenceDate = untilNextReferenceDate;
        }
    }

    static Snapshot compute(SimpleDate startDate, String usEndDate, long recurDays, Calendar now) {
        boolean hasEndDate = usEndDate != null && usEndDate.trim().length() > 0;
        SimpleDate endDate = hasEndDate ? new SimpleDate(usEndDate, SimpleDate.DateStyle.US) : null;

        SimpleDate lastOccurrence = startDate;
        SimpleDate nextOccurrence = startDate;

        if (recurDays != 0) {
            RecurrenceCycle.Occurrences occurrences =
                    RecurrenceCycle.computeOccurrences(startDate, recurDays, now);
            lastOccurrence = occurrences.lastOccurrence;
            nextOccurrence = occurrences.nextOccurrence;
        }

        boolean startInPast = startDate.getDate().before(now.getTime());
        boolean endInPast = hasEndDate && endDate.getDate().before(now.getTime());
        boolean bothInPast = startInPast && endInPast;

        SimpleDate daysSinceReferenceDate = startDate;
        SimpleDate sinceLastReferenceDate = bothInPast
                ? endDate
                : (recurDays == 0 ? startDate : lastOccurrence);

        SimpleDate untilNextReferenceDate;
        if (recurDays == 0 || !startInPast) {
            untilNextReferenceDate = startDate;
        } else {
            untilNextReferenceDate = nextOccurrence;
        }

        boolean hasFutureOccurrence;
        if (recurDays == 0) {
            hasFutureOccurrence = now.getTime().before(startDate.getDate());
        } else {
            hasFutureOccurrence = true;

            if (hasEndDate) {
                // end date is inclusive for an occurrence on that exact day; anything after is invalid
                if (endDate.getDate().before(now.getTime()) || endDate.getDate().equals(now.getTime())) {
                    hasFutureOccurrence = false;
                } else if (untilNextReferenceDate.getDate().after(endDate.getDate())) {
                    hasFutureOccurrence = false;
                }
            }
        }

        return new Snapshot(
                startDate,
                endDate,
                hasEndDate,
                lastOccurrence,
                nextOccurrence,
                daysSinceReferenceDate,
                sinceLastReferenceDate,
                hasFutureOccurrence,
                untilNextReferenceDate);
    }
}
