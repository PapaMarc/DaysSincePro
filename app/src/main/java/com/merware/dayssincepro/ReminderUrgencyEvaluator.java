package com.merware.dayssincepro;

final class ReminderUrgencyEvaluator {

    enum ReminderUrgency { NONE, NEAR_DUE, DUE, OVERDUE }

    private ReminderUrgencyEvaluator() {
    }

    static ReminderUrgency evaluate(long recurDays,
                                    long daysSinceReference,
                                    long daysUntilNextOccurrence,
                                    int effectiveLeadDays) {
        if (recurDays == 0) {
            if (daysSinceReference == 0) {
                return ReminderUrgency.DUE;
            }
            if (daysSinceReference > 0) {
                // One-time events should not keep producing overdue reminders forever.
                return ReminderUrgency.NONE;
            }

            long daysUntilEvent = Math.abs(daysSinceReference);
            if (daysUntilEvent <= effectiveLeadDays) {
                return ReminderUrgency.NEAR_DUE;
            }
            return ReminderUrgency.NONE;
        }

        if (daysSinceReference == 0) {
            return ReminderUrgency.DUE;
        }
        if (daysUntilNextOccurrence < 0) {
            return ReminderUrgency.OVERDUE;
        }
        if (daysUntilNextOccurrence <= effectiveLeadDays) {
            return ReminderUrgency.NEAR_DUE;
        }
        return ReminderUrgency.NONE;
    }
}
