package com.merware.dayssincepro;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Utility for validating imported SQLite backups before they can replace the live DB.
 */
public final class SqliteImportPreflight {

    private static final int SQLITE_HEADER_MIN_BYTES = 64;
    private static final int SQLITE_USER_VERSION_OFFSET = 60;

    private SqliteImportPreflight() {
    }

    static boolean hasSqliteHeader(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[6];
            int read = fis.read(header);
            return read == 6
                    && Arrays.equals(header, "SQLite".getBytes(StandardCharsets.US_ASCII));
        }
    }

    static int readUserVersion(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < SQLITE_HEADER_MIN_BYTES) {
                throw new IOException("Database file too small to read schema version");
            }
            raf.seek(SQLITE_USER_VERSION_OFFSET);
            return raf.readInt();
        }
    }

    static boolean isSchemaVersionSupported(int importedSchemaVersion, int supportedSchemaVersion) {
        return importedSchemaVersion <= supportedSchemaVersion;
    }
}