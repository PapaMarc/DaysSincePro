package com.merware.dayssincepro;

final class ReminderLeadDaysResolver {

    static final class Resolution {
        final int effectiveLeadDays;
        final boolean custom;

        Resolution(int effectiveLeadDays, boolean custom) {
            this.effectiveLeadDays = effectiveLeadDays;
            this.custom = custom;
        }
    }

    private ReminderLeadDaysResolver() {
    }

    static Resolution resolve(long recurDays, Integer customLeadDays) {
        boolean custom = customLeadDays != null;
        int configuredLeadDays = custom
                ? customLeadDays.intValue()
                : ReminderDefaults.defaultLeadDays(recurDays);

        configuredLeadDays = clampConfiguredRange(configuredLeadDays);

        int effectiveLeadDays = configuredLeadDays;
        if (recurDays > 0 && !ReminderDefaults.isNamedRecurrence(recurDays)) {
            effectiveLeadDays = (int) Math.min(effectiveLeadDays, recurDays);
        }

        return new Resolution(effectiveLeadDays, custom);
    }

    private static int clampConfiguredRange(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 30);
    }
}
