package com.merware.dayssincepro;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the Until Next tab's SQL-level row eligibility semantics with real SQLite.
 */
public class UntilNextFilterSqlTest {

    @Test
    public void untilNextWhereClause_excludesEndedRecurring_andKeepsEligibleRows() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT, date DATE, recur INTEGER, end_date DATE, planned_date DATE)");

                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('recurring-ended-past', '2019-08-03', 90, '2021-08-15', NULL)");
                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('recurring-ended-today', '2019-08-03', 90, '2026-09-01', NULL)");
                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('recurring-active-with-end', '2019-08-03', 90, '2026-10-15', NULL)");
                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('recurring-active-no-end', '2019-08-03', 90, NULL, NULL)");
                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('one-time-future', '2026-12-01', 0, NULL, NULL)");
                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('one-time-past', '2020-01-01', 0, NULL, NULL)");
                st.execute("INSERT INTO event (event, date, recur, end_date, planned_date) VALUES ('one-time-planned-future', '2020-01-01', 0, NULL, '2026-11-11')");
            }

            String today = "2026-09-01";
            String sql = "select event from event where (date > '" + today
                    + "' or (recur = 0 and planned_date > '" + today + "')"
                    + " or (recur > 0 and (end_date is null or end_date > '" + today
                    + "'))) order by event";

            List<String> actual = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    actual.add(rs.getString(1));
                }
            }

            assertEquals(
                    Arrays.asList(
                            "one-time-future",
                            "one-time-planned-future",
                            "recurring-active-no-end",
                            "recurring-active-with-end"
                    ),
                    actual
            );
        }
    }
}
