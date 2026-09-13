package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReminderLeadDaysResolverTest {

    @Test
    public void namedAnnualDefault_is21Days() {
        ReminderLeadDaysResolver.Resolution resolution =
                ReminderLeadDaysResolver.resolve(365, null);

        assertEquals(21, resolution.effectiveLeadDays);
        assertFalse(resolution.custom);
    }

    @Test
    public void numberOfDaysDefault_usesTieredMapping() {
        assertEquals(1, ReminderLeadDaysResolver.resolve(1, null).effectiveLeadDays);
        assertEquals(2, ReminderLeadDaysResolver.resolve(2, null).effectiveLeadDays);
        assertEquals(3, ReminderLeadDaysResolver.resolve(14, null).effectiveLeadDays);
        assertEquals(7, ReminderLeadDaysResolver.resolve(15, null).effectiveLeadDays);
    }

    @Test
    public void customLeadDays_areClampedToConfiguredRange() {
        assertEquals(0, ReminderLeadDaysResolver.resolve(365, -9).effectiveLeadDays);
        assertEquals(30, ReminderLeadDaysResolver.resolve(365, 99).effectiveLeadDays);
        assertTrue(ReminderLeadDaysResolver.resolve(365, 5).custom);
    }

    @Test
    public void numberOfDaysRecurrence_customLeadDays_areClampedToInterval() {
        ReminderLeadDaysResolver.Resolution resolution =
                ReminderLeadDaysResolver.resolve(5, 10);

        assertEquals(5, resolution.effectiveLeadDays);
        assertTrue(resolution.custom);
    }

    @Test
    public void namedRecurrence_customLeadDays_areNotIntervalClamped() {
        ReminderLeadDaysResolver.Resolution resolution =
                ReminderLeadDaysResolver.resolve(7, 10);

        assertEquals(10, resolution.effectiveLeadDays);
    }
}
