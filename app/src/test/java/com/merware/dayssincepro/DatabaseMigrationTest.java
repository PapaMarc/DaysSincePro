package com.merware.dayssincepro;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies DatabaseHelper.getMigrationStatements() - the pure, stepwise migration-plan
 * function - both in isolation and by actually executing its output against a real
 * SQLite engine (org.xerial:sqlite-jdbc), confirming upgrades from every known prior
 * version preserve existing data and land on the correct final schema.
 *
 * Regression coverage for the previously-destructive onUpgrade() fallback, which used to
 * silently DROP and recreate every table (category, event, history) for any unmatched
 * (oldVersion, newVersion) pair - meaning any user upgrading from an unexpected starting
 * version would lose all their data with no warning.
 */
public class DatabaseMigrationTest {

    private final List<File> tempFiles = new ArrayList<>();

    @After
    public void cleanup() {
        for (File f : tempFiles) {
            f.delete();
        }
    }

    private File newTempDbPath() throws IOException {
        File f = File.createTempFile("migration-test", ".db");
        tempFiles.add(f);
        return f;
    }

    private Connection open(File dbFile) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    // ---- pure getMigrationStatements() coverage ----

    @Test
    public void v1ToV2_addsHistoryTableOnly() {
        List<String> statements = DatabaseHelper.getMigrationStatements(1, 2);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("history"));
    }

    @Test
    public void v2ToV3_addsEndDateColumnOnly() {
        List<String> statements = DatabaseHelper.getMigrationStatements(2, 3);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("end_date"));
    }

    @Test
    public void v1ToV3_appliesBothStepsInOrder() {
        List<String> statements = DatabaseHelper.getMigrationStatements(1, 3);
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("history"));
        assertTrue(statements.get(1).contains("end_date"));
    }

    @Test
    public void sameVersion_returnsEmptyList() {
        List<String> statements = DatabaseHelper.getMigrationStatements(3, 3);
        assertEquals(0, statements.size());
    }

    @Test
    public void unknownFutureVersion_returnsNullInsteadOfDroppingData() {
        // No step is defined yet from v3 to v4 - must return null (refuse), not silently
        // fabricate/execute a destructive migration.
        assertNull(DatabaseHelper.getMigrationStatements(3, 4));
        assertNull(DatabaseHelper.getMigrationStatements(1, 4));
    }

    @Test
    public void invalidVersions_returnNull() {
        assertNull(DatabaseHelper.getMigrationStatements(0, 1));
        assertNull(DatabaseHelper.getMigrationStatements(5, 2));
    }

    // ---- real-SQLite integration coverage: no data loss across every known upgrade path ----

    private void createV1Schema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE category (_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, type INTEGER)");
            st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, catId INTEGER, event TEXT, date DATE, recur INTEGER)");
            st.execute("INSERT INTO category (category, type) VALUES ('Bills', 0)");
            st.execute("INSERT INTO event (catId, event, date, recur) VALUES (1, 'Oil Change', '2020-01-15', 365)");
        }
    }

    private void createV2Schema(Connection conn) throws SQLException {
        createV1Schema(conn);
        try (Statement st = conn.createStatement()) {
            st.execute(DatabaseHelper.CREATE_HISTORY_TABLE_SQL);
            st.execute("INSERT INTO history (eventId, catID, date, onTime, note) VALUES (1, 1, '2021-01-15', 1, 'done')");
        }
    }

    private void applyMigration(Connection conn, int oldVersion, int newVersion) throws SQLException {
        List<String> statements = DatabaseHelper.getMigrationStatements(oldVersion, newVersion);
        try (Statement st = conn.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    private int rowCount(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("select count(*) from " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    public void upgradeFromV1_preservesDataAndReachesV3Schema() throws SQLException, IOException {
        File dbFile = newTempDbPath();
        try (Connection conn = open(dbFile)) {
            createV1Schema(conn);
            applyMigration(conn, 1, 3);

            assertTrue("history table should exist after v1->v3", tableExists(conn, "history"));
            assertTrue("event.end_date should exist after v1->v3", columnExists(conn, "event", "end_date"));
            assertEquals("category rows must survive migration", 1, rowCount(conn, "category"));
            assertEquals("event rows must survive migration", 1, rowCount(conn, "event"));
        }
    }

    @Test
    public void upgradeFromV2_preservesDataAndReachesV3Schema() throws SQLException, IOException {
        File dbFile = newTempDbPath();
        try (Connection conn = open(dbFile)) {
            createV2Schema(conn);
            applyMigration(conn, 2, 3);

            assertTrue("event.end_date should exist after v2->v3", columnExists(conn, "event", "end_date"));
            assertEquals("category rows must survive migration", 1, rowCount(conn, "category"));
            assertEquals("event rows must survive migration", 1, rowCount(conn, "event"));
            assertEquals("history rows must survive migration", 1, rowCount(conn, "history"));
        }
    }
}
