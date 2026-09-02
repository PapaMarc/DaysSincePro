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
 * SQL-level verification for mixed category filtering, including uncategorized sentinel id 0.
 */
public class CategorySelectionFilterSqlTest {

    @Test
    public void categoryFilter_withRealPlusUncategorized_returnsUnion() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, catId INTEGER, event TEXT)");

                st.execute("INSERT INTO event (catId, event) VALUES (0, 'uncat-a')");
                st.execute("INSERT INTO event (catId, event) VALUES (2, 'cat2-a')");
                st.execute("INSERT INTO event (catId, event) VALUES (3, 'cat3-a')");
            }

            String sql = "SELECT event FROM event WHERE catId IN (0,2) ORDER BY event ASC";
            List<String> actual = new ArrayList<>();

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    actual.add(rs.getString(1));
                }
            }

            assertEquals(Arrays.asList("cat2-a", "uncat-a"), actual);
        }
    }
}
