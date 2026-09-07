package com.merware.dayssincepro;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

/**
 * Verifies Since Last projection semantics for one-time events.
 */
public class SinceLastProjectionSqlTest {

    @Test
    public void oneTimeEvent_prefersLatestHistoryOnOrBeforeToday_overAnchor() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, catID INTEGER, event TEXT, date DATE, recur INTEGER, end_date DATE, details TEXT, planned_date DATE)");
                st.execute("CREATE TABLE history (_id INTEGER PRIMARY KEY AUTOINCREMENT, eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT)");

                st.execute("INSERT INTO event (_id, catID, event, date, recur, end_date, details, planned_date) VALUES (1, 1, 'Hive2 started', '2026-08-27', 0, NULL, '', NULL)");
                st.execute("INSERT INTO history (eventId, catID, date, onTime, note) VALUES (1, 1, '2026-09-07', 1, '')");
                st.execute("INSERT INTO history (eventId, catID, date, onTime, note) VALUES (1, 1, '2026-09-08', 1, '')");
            }

            String today = "2026-09-07";
            String sql = "SELECT event.date as anchor_date, "
                    + "(SELECT max(h.date) FROM history h WHERE h.eventId = event._id AND h.date <= '" + today + "') as last_happened_date "
                    + "FROM event WHERE _id = 1";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                assertEquals("2026-08-27", rs.getString("anchor_date"));
                assertEquals("2026-09-07", rs.getString("last_happened_date"));
            }
        }
    }

    @Test
    public void oneTimeEvent_withoutHistory_fallsBackToAnchor() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, catID INTEGER, event TEXT, date DATE, recur INTEGER, end_date DATE, details TEXT, planned_date DATE)");
                st.execute("CREATE TABLE history (_id INTEGER PRIMARY KEY AUTOINCREMENT, eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT)");

                st.execute("INSERT INTO event (_id, catID, event, date, recur, end_date, details, planned_date) VALUES (1, 1, 'Hive2 started', '2026-08-27', 0, NULL, '', NULL)");
            }

            String today = "2026-09-07";
            String sql = "SELECT event.date as anchor_date, "
                    + "(SELECT max(h.date) FROM history h WHERE h.eventId = event._id AND h.date <= '" + today + "') as last_happened_date "
                    + "FROM event WHERE _id = 1";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                assertEquals("2026-08-27", rs.getString("anchor_date"));
                assertEquals(null, rs.getString("last_happened_date"));
            }
        }
    }
}
