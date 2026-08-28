package com.merware.dayssincepro;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DaysUntilTest {
    // Returns the next occurrence date after 'now' for a recurring event
    public static Date getNextOccurrence(Date eventDate, int intervalDays, Date now) {
        Calendar recurCal = Calendar.getInstance();
        recurCal.setTime(eventDate);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);

        if (recurCal.after(nowCal)) {
            return recurCal.getTime();
        } else {
            while (!recurCal.after(nowCal)) {
                recurCal.add(Calendar.DAY_OF_YEAR, intervalDays);
            }
            return recurCal.getTime();
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date now = sdf.parse("2026-04-29");

        // Test cases: {eventDate, intervalDays, description}
        Object[][] tests = {
            {"2026-04-15", 365, "Annual event (Tax Day, after event)"},
            {"2026-04-29", 365, "Annual event (Today)"},
            {"2026-07-01", 365, "Annual event (Future this year)"},
            {"2026-01-01", 90,  "Quarterly event (Jan 1)"},
            {"2026-04-01", 90,  "Quarterly event (Apr 1)"},
            {"2026-04-29", 30,  "Monthly event (Today)"},
            {"2026-04-01", 30,  "Monthly event (Earlier this month)"},
            {"2026-04-29", 7,   "Weekly event (Today)"},
            {"2026-04-25", 7,   "Weekly event (Earlier this week)"},
        };

        for (Object[] test : tests) {
            Date eventDate = sdf.parse((String)test[0]);
            int interval = (int)test[1];
            String desc = (String)test[2];
            Date next = getNextOccurrence(eventDate, interval, now);
            long daysUntil = (next.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
            System.out.printf("%s: Next occurrence = %s, Days until = %d\n", desc, sdf.format(next), daysUntil);
        }
    }
}
