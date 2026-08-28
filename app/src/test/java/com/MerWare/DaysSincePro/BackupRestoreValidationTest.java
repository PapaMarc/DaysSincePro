package com.MerWare.DaysSincePro;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Validates the redesigned backup/restore mechanism:
 *   - backup uses "VACUUM INTO" (a consistent, SQLite-native snapshot) instead of a raw
 *     file-byte copy of the live db, so it's correct even if other connections are open.
 *   - restore closes the single shared DatabaseHelper connection first, then swaps the
 *     file, then a fresh connection is opened - instead of overwriting the file behind
 *     connections that are still open.
 *
 * Runs against a real SQLite engine (org.xerial:sqlite-jdbc) on the plain JVM, since
 * android.database.sqlite can't execute outside the Android runtime. This exercises real
 * SQL semantics (VACUUM INTO, connection/file interaction) rather than simulating bytes.
 */
public class BackupRestoreValidationTest {

    private final List<File> tempFiles = new ArrayList<>();

    @After
    public void cleanup() {
        for (File f : tempFiles) {
            f.delete();
        }
    }

    private File newTempDbPath(String prefix) throws IOException {
        File f = File.createTempFile(prefix, ".db");
        f.delete(); // we only want a unique path; SQLite/VACUUM INTO will create the file
        tempFiles.add(f);
        return f;
    }

    private Connection open(File dbFile) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    private void createSchemaAndSeed(Connection conn, String... eventNames) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT)");
            for (String name : eventNames) {
                st.execute("INSERT INTO event (event) VALUES ('" + name + "')");
            }
        }
    }

    private List<String> readEventNames(Connection conn) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("select event from event order by _id")) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    // Mirrors MainActivity.isSQLiteFile()
    private static boolean isSQLiteFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        byte[] byteArr = new byte[6];
        fis.read(byteArr);
        fis.close();
        return Arrays.equals(byteArr, "SQLite".getBytes());
    }

    // Mirrors MainActivity.doImportDB()'s raw copy loop, used once the shared connection
    // has already been closed (the new, correct order of operations).
    private static void rawCopy(String src, String dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    @Test
    public void vacuumIntoBackupCapturesCommittedDataEvenWithOtherConnectionsOpen() throws Exception {
        File live = newTempDbPath("alex_db");
        File backup = newTempDbPath("daysSince");
        // VACUUM INTO refuses to write to a file that already exists.
        backup.delete();

        try (Connection liveConn = open(live)) {
            createSchemaAndSeed(liveConn, "committed-event-1", "committed-event-2");

            // Simulate another screen concurrently holding a read connection open -
            // exactly the situation the old raw-file-copy backup could corrupt/miss data in.
            try (Connection otherReader = open(live);
                 Statement s = otherReader.createStatement();
                 ResultSet ignored = s.executeQuery("select * from event")) {

                try (Statement st = liveConn.createStatement()) {
                    st.execute("VACUUM INTO '" + backup.getAbsolutePath().replace("'", "''") + "'");
                }

                assertTrue("VACUUM INTO should have created the backup file", backup.exists());

                try (Connection backupConn = open(backup)) {
                    assertEquals("Backup must contain exactly the committed rows",
                            Arrays.asList("committed-event-1", "committed-event-2"),
                            readEventNames(backupConn));
                }
            }
        }
    }

    @Test
    public void restoreClosesSharedConnectionBeforeSwappingFileThenReopensCleanly() throws Exception {
        File live = newTempDbPath("alex_db");
        File chosenBackup = newTempDbPath("chosen_backup");

        // "Live" app state before restore.
        try (Connection sharedConn = open(live)) {
            createSchemaAndSeed(sharedConn, "pre-restore-event");
        } // <-- represents DatabaseHelper.closeInstance(), called before the file swap.

        // The backup file the user picked to restore.
        try (Connection backupConn = open(chosenBackup)) {
            createSchemaAndSeed(backupConn, "restored-event-1", "restored-event-2");
        }

        assertTrue("Chosen file must look like a SQLite database before restoring",
                isSQLiteFile(chosenBackup.getAbsolutePath()));

        // Now that no connection holds `live` open, it's safe to swap the file directly.
        rawCopy(chosenBackup.getAbsolutePath(), live.getAbsolutePath());

        // A fresh connection (what DatabaseHelper.getInstance() creates after the app restarts)
        // must see exactly the restored data, with no leftover pre-restore rows.
        try (Connection freshConn = open(live)) {
            assertEquals("Restore should replace pre-restore data with exactly the backup's rows",
                    Arrays.asList("restored-event-1", "restored-event-2"),
                    readEventNames(freshConn));
        }
    }

    @Test
    public void restoreRejectsFilesThatAreNotSQLiteDatabases() throws Exception {
        File notADb = File.createTempFile("notes", ".txt");
        tempFiles.add(notADb);
        Files.write(notADb.toPath(), "just some plain text, not a database".getBytes());

        assertTrue("A plain-text file must be rejected before any restore is attempted",
                !isSQLiteFile(notADb.getAbsolutePath()));
    }

    @Test
    public void staleConnectionKeptOpenAcrossARawFileSwapIsUnreliable() throws Exception {
        // Demonstrates why the OLD design (overwriting the db file while other
        // connections stay open, with no restart/refresh) was fragile: once the file is
        // swapped underneath a connection that was never closed, its cached schema/pages
        // no longer reliably correspond to what's on disk. The new design avoids this
        // entirely by always closing the shared connection first.
        File live = newTempDbPath("alex_db");
        File otherBackup = newTempDbPath("other_backup");

        Connection staleConn = open(live);
        createSchemaAndSeed(staleConn, "original-event");
        // Prime the connection's cache the way a long-lived fragment connection would.
        readEventNames(staleConn);

        try (Connection backupConn = open(otherBackup)) {
            createSchemaAndSeed(backupConn, "swapped-in-event-1", "swapped-in-event-2");
        }

        boolean sawStaleOrBrokenState;
        try {
            // Old (buggy) behavior: swap the file WITHOUT closing staleConn first.
            Files.copy(otherBackup.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING);

            List<String> afterSwap = readEventNames(staleConn);
            sawStaleOrBrokenState = !afterSwap.equals(Arrays.asList("swapped-in-event-1", "swapped-in-event-2"));
        } catch (IOException | SQLException e) {
            // Also acceptable evidence of the problem: on some platforms SQLite's lock on
            // the still-open connection blocks the raw file swap outright (e.g. Windows
            // throws FileSystemException here), and on others the stale connection can
            // fail or return inconsistent results once the swap does go through.
            sawStaleOrBrokenState = true;
        } finally {
            staleConn.close();
        }

        assertTrue("A connection left open across a raw file swap does not reliably reflect "
                        + "the new data - this is exactly the risk the redesign (close-then-swap-"
                        + "then-reopen) eliminates.",
                sawStaleOrBrokenState);
    }
}
