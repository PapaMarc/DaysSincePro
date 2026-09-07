package com.merware.dayssincepro;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Verifies that search SQL is title-only, case-insensitive, and tab-parity safe.
 */
public class SearchFilterSqlParityTest {

    @Test
    public void buildSearchSql_doesNotApplyTabDateFilters() {
        String sql = PastFutureListFragment.buildSearchSql("event ASC");
        String upperSql = sql.toUpperCase();

        assertFalse(upperSql.contains("FROM EVENT WHERE DATE <="));
        assertFalse(upperSql.contains("FROM EVENT WHERE DATE >"));
        assertFalse(upperSql.contains("FROM EVENT WHERE END_DATE"));
    }

    @Test
    public void searchSql_caseInsensitiveTitleMatch_returnsSameEventSetRegardlessOfDateType() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, catID INTEGER, event TEXT, date DATE, recur INTEGER, end_date DATE, details TEXT, planned_date DATE)");
                st.execute("CREATE TABLE history (_id INTEGER PRIMARY KEY AUTOINCREMENT, eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT)");

                st.execute("INSERT INTO event (catID, event, date, recur, end_date, details, planned_date) VALUES (1, 'Jill BDay', '1961-04-11', 0, NULL, '', NULL)");
                st.execute("INSERT INTO event (catID, event, date, recur, end_date, details, planned_date) VALUES (2, 'Naturalis Ji Chronicle', '2045-06-15', 0, NULL, '', NULL)");
                st.execute("INSERT INTO event (catID, event, date, recur, end_date, details, planned_date) VALUES (3, 'No Match Item', '2020-01-01', 365, '2022-01-01', '', NULL)");
            }

            String sql = PastFutureListFragment.buildSearchSql("event ASC");
            List<String> actual = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "%Ji%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        actual.add(rs.getString("event"));
                    }
                }
            }

            assertEquals(Arrays.asList("Jill BDay", "Naturalis Ji Chronicle"), actual);
        }
    }
}
