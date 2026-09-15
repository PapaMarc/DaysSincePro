package com.merware.dayssincepro;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqliteImportPreflightTest {

    @Test
    public void readUserVersion_readsPragmaUserVersion() throws Exception {
        File db = File.createTempFile("schema_v7", ".db");
        db.deleteOnExit();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE event (_id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT)");
            st.execute("PRAGMA user_version = 7");
        }

        assertEquals(7, SqliteImportPreflight.readUserVersion(db));
    }

    @Test
    public void hasSqliteHeader_rejectsPlainTextFile() throws Exception {
        File txt = File.createTempFile("not_sqlite", ".txt");
        txt.deleteOnExit();
        Files.write(txt.toPath(), "plain text".getBytes(StandardCharsets.UTF_8));

        assertFalse(SqliteImportPreflight.hasSqliteHeader(txt));
    }

    @Test
    public void isSchemaVersionSupported_rejectsOnlyNewerImports() {
        assertTrue(SqliteImportPreflight.isSchemaVersionSupported(5, 6));
        assertTrue(SqliteImportPreflight.isSchemaVersionSupported(6, 6));
        assertFalse(SqliteImportPreflight.isSchemaVersionSupported(7, 6));
    }
}