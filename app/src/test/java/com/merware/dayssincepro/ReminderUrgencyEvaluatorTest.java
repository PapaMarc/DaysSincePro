package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReminderUrgencyEvaluatorTest {

    @Test
    public void oneTime_dueToday_isDue() {
        assertEquals(ReminderUrgencyEvaluator.ReminderUrgency.DUE,
                ReminderUrgencyEvaluator.evaluate(0, 0, 0, 3));
    }

    @Test
    public void oneTime_futureWithinLeadWindow_isNearDue() {
        assertEquals(ReminderUrgencyEvaluator.ReminderUrgency.NEAR_DUE,
                ReminderUrgencyEvaluator.evaluate(0, -2, 2, 3));
    }

    @Test
    public void oneTime_pastEvent_isNone() {
        assertEquals(ReminderUrgencyEvaluator.ReminderUrgency.NONE,
                ReminderUrgencyEvaluator.evaluate(0, 2, 0, 3));
    }

    @Test
    public void recurring_dueToday_isDue() {
        assertEquals(ReminderUrgencyEvaluator.ReminderUrgency.DUE,
                ReminderUrgencyEvaluator.evaluate(365, 0, 365, 21));
    }

    @Test
    public void recurring_withinLeadWindow_isNearDue() {
        assertEquals(ReminderUrgencyEvaluator.ReminderUrgency.NEAR_DUE,
                ReminderUrgencyEvaluator.evaluate(365, 359, 6, 21));
    }

    @Test
    public void recurring_outsideLeadWindow_isNone() {
        assertEquals(ReminderUrgencyEvaluator.ReminderUrgency.NONE,
                ReminderUrgencyEvaluator.evaluate(365, 120, 245, 21));
    }
}
