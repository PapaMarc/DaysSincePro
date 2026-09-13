package com.merware.dayssincepro;

final class ReminderDefaults {

    private ReminderDefaults() {
    }

    static int defaultLeadDays(long recurDays) {
        switch ((int) recurDays) {
            case 0:
            case 7:
            case 14:
                return 3;
            case 30:
            case 90:
                return 7;
            case 180:
            case 365:
                return 21;
            default:
                return defaultLeadDaysForNumberOfDays(recurDays);
        }
    }

    static boolean isNamedRecurrence(long recurDays) {
        switch ((int) recurDays) {
            case 0:
            case 7:
            case 14:
            case 30:
            case 90:
            case 180:
            case 365:
                return true;
            default:
                return false;
        }
    }

    private static int defaultLeadDaysForNumberOfDays(long recurDays) {
        if (recurDays <= 0) {
            return 0;
        }
        if (recurDays == 1) {
            return 1;
        }
        if (recurDays == 2) {
            return 2;
        }
        if (recurDays <= 14) {
            return 3;
        }
        return 7;
    }
}
