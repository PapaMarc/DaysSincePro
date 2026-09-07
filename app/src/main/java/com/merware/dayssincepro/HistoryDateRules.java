package com.merware.dayssincepro;

import java.util.Date;

final class HistoryDateRules {

    private HistoryDateRules() {
    }

    static String todayIsoDate() {
        return new SimpleDate(new Date()).getDate(SimpleDate.DateStyle.YMD);
    }

    static boolean isFutureHappenedDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return false;
        }
        return isoDate.compareTo(todayIsoDate()) > 0;
    }
}