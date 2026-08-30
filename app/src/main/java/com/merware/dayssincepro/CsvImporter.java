package com.merware.dayssincepro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * CsvImporter - RFC-4180 Compliant CSV Import Engine for DaysSincePro
 * ============================================================================
 *
 * ARCHITECTURAL REDESIGN & DOCUMENTATION:
 * ----------------------------------------------------------------------------
 * 1. PROBLEMS WITH ORIGINAL IMPLEMENTATION (in CategoriesActivity.java):
 *    - Broken Regex Parser: Used split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)") which
 *      was designed for double quotes, but the original exporter produced single
 *      quotes. On lines with single quotes or unclosed quotes (e.g., row 5 in
 *      ManualPayments.csv: Tags BigMama (97VWEVC  99BMW323ic)',...), the regex
 *      lookahead failed or produced malformed tokens.
 *    - Brittle Quote Stripping: Attempted naive substring(1, length - 1) which
 *      crashed or corrupted tokens when strings lacked symmetric single quotes.
 *    - SQL Injection & Apostrophe Crashes: Executed raw SQL string interpolation:
 *      sql = "insert into event (catId, event, date, recur) values (" + ... + "'" + event + "'..."
 *      Any event title containing an apostrophe (e.g., "Papa's Truck", "Mom's Birthday")
 *      caused immediate SQLiteException syntax crashes, and was vulnerable to SQL injection.
 *    - No Database Transactions: Each row was inserted with an individual execSQL()
 *      outside a transaction, resulting in severe I/O overhead and partial import corruption
 *      if a mid-file error occurred.
 *    - No Header Row Detection: Assumed the first row was always a data row. Files with
 *      headers had their header row fail date parsing or create bogus records.
 *    - No Category Column Support: Assumed all imported events belonged strictly to the
 *      currently selected UI category (importId), making it impossible to import multi-category
 *      files or full-database backups.
 *    - Platform Default Charset: FileReader used the host platform default charset instead of
 *      standard UTF-8 without BOM, corrupting non-ASCII and international characters.
 *    - Tight UI Coupling: Import logic was hardcoded inside CategoriesActivity, making it
 *      impossible to unit test or reuse in other activities.
 *
 * 2. HOW ISSUES WERE RESOLVED:
 *    - Robust RFC-4180 Character Parser: Replaced fragile regex with a full tokenizer that
 *      supports standard RFC-4180 double-quoted fields ("..."), escaped quotes (""),
 *      commas inside quotes, multi-line fields, and backward-compatible cleanup for legacy
 *      single-quoted CSVs (e.g., ManualPayments.csv).
 *    - Safe Parameterized Inserts: Replaced raw SQL strings with SQLiteDatabase.insert()
 *      using ContentValues. Apostrophes and special characters are stored cleanly without syntax
 *      crashes or security risks.
 *    - Transactional Batching: All row operations are executed inside a database transaction
 *      (beginTransaction / setTransactionSuccessful / endTransaction), making imports atomic
 *      and orders of magnitude faster.
 *    - Intelligent Header Detection & Column Mapping: Dynamically detects header rows containing
 *      "category", "event", "date", or "recur". Automatically maps columns by name.
 *    - Dynamic Category Resolution:
 *      * When a "category" column is present: Looks up the category name in the SQLite database.
 *        If found, links to existing category ID; if not found, automatically creates the new
 *        category in the database (type = 0).
 *      * When "category" column is absent: Seamlessly falls back to the UI-selected defaultCategoryId.
 *    - Date Standardization: Validates ISO-8601 (yyyy-MM-dd) dates and normalizes them for storage.
 *    - Explicit UTF-8 with BOM Tolerance: Reads streams in UTF-8 and transparently ignores UTF-8
 *      BOM (\uFEFF) if present from tools like Microsoft Excel.
 *    - Comprehensive Results: Returns CsvImportResult detailing imported rows, skipped rows,
 *      new categories created, and specific warning/error messages.
 *
 * 3. INTEROPERABILITY & MAINTAINABILITY BENEFITS:
 *    - Full RFC-4180 compliance ensures files exported from Excel, Google Sheets, or any other
 *      standard tool can be imported seamlessly.
 *    - Preserves backward compatibility with legacy single-quoted CSVs from earlier DaysSincePro releases.
 *    - Supports full-database and multi-category imports using text category names, making CSVs
 *      ideal for offline data curation, backups, and migrations.
 *
 * 4. ARCHITECTURAL BENEFITS:
 *    - Clean separation of concerns: CsvImporter handles parsing, validation, and database operations.
 *    - Fast, independent JVM unit testing against standard and edge-case CSV inputs.
 * ============================================================================
 */
public class CsvImporter {

    private static final String TAG = "CsvImporter";

    private CsvImporter() {
        // Utility class; prevent instantiation
    }

    /**
     * Parses an RFC-4180 compliant CSV stream into structured records.
     * Handles quoted fields, escaped double-quotes (""), commas inside quotes,
     * multi-line records, and backward-compatible cleanup for legacy single quotes.
     *
     * @param reader Reader providing the CSV character stream.
     * @return List of records, where each record is a List of string field values.
     * @throws IOException On read error.
     */
    public static List<List<String>> parseRecords(BufferedReader reader) throws IOException {
        List<List<String>> records = new ArrayList<>();
        String line;
        StringBuilder multilineField = null;
        List<String> currentRecord = new ArrayList<>();
        boolean inQuotes = false;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            // Handle UTF-8 BOM at the very start of the file
            if (firstLine) {
                firstLine = false;
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
            }

            if (multilineField == null && line.trim().isEmpty()) {
                // Skip empty lines between records
                continue;
            }

            int len = line.length();
            int i = 0;

            if (multilineField == null) {
                multilineField = new StringBuilder();
                inQuotes = false;
            } else {
                multilineField.append("\n");
            }

            while (i < len) {
                char c = line.charAt(i);

                if (inQuotes) {
                    if (c == '"') {
                        if (i + 1 < len && line.charAt(i + 1) == '"') {
                            // Escaped double quote ("")
                            multilineField.append('"');
                            i += 2;
                        } else {
                            // Closing quote
                            inQuotes = false;
                            i++;
                        }
                    } else {
                        multilineField.append(c);
                        i++;
                    }
                } else {
                    if (c == '"' && multilineField.length() == 0) {
                        inQuotes = true;
                        i++;
                    } else if (c == ',') {
                        currentRecord.add(cleanField(multilineField.toString()));
                        multilineField.setLength(0);
                        i++;
                    } else {
                        multilineField.append(c);
                        i++;
                    }
                }
            }

            if (!inQuotes) {
                currentRecord.add(cleanField(multilineField.toString()));
                multilineField = null;
                records.add(new ArrayList<>(currentRecord));
                currentRecord.clear();
            }
        }

        if (multilineField != null) {
            currentRecord.add(cleanField(multilineField.toString()));
            records.add(new ArrayList<>(currentRecord));
        }

        return records;
    }

    /**
     * Cleans a parsed token: strips leading/trailing whitespace and cleans up
     * legacy single quotes from earlier DaysSincePro exports (e.g. 'text' or text').
     *
     * @param raw Raw token string.
     * @return Cleaned field text.
     */
    /**
     * Cleans a parsed token: strips leading/trailing whitespace and cleans up
     * legacy single quotes from earlier DaysSincePro exports (e.g. 'text' or text').
     *
     * @param raw Raw token string.
     * @return Cleaned field text.
     */
    public static String cleanField(String raw) {
        if (raw == null) {
            return "";
        }
        String field = raw.trim();
        // Handle legacy single-quote enclosures: 'event'
        if (field.startsWith("'") && field.endsWith("'") && field.length() >= 2) {
            field = field.substring(1, field.length() - 1);
        } else if (field.endsWith("'") && !field.startsWith("'")) {
            // Legacy malformed line recovery: e.g. Tags BigMama (97VWEVC 99BMW323ic)'
            field = field.substring(0, field.length() - 1);
        } else if (field.startsWith("'") && !field.endsWith("'")) {
            field = field.substring(1);
        }
        return field.trim();
    }

    /**
     * Extracts a clean category name stem from a file name or display name.
     * For example, "Medical.csv" -> "Medical", "Vehicles_Backup.csv" -> "Vehicles_Backup".
     *
     * @param filename Raw filename or display name.
     * @return Extracted category stem or null if generic / empty.
     */
    public static String inferCategoryFromFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }
        String name = filename.trim();
        // Remove trailing query or parameters if any
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < name.length() - 1) {
            name = name.substring(lastSlash + 1);
        }
        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.trim();

        // Ignore generic full-database names
        if (name.equalsIgnoreCase("daysSince") || name.equalsIgnoreCase("daysSincePro") ||
            name.equalsIgnoreCase("daysSincePro_All") || name.equalsIgnoreCase("events") ||
            name.equalsIgnoreCase("export") || name.isEmpty()) {
            return null;
        }
        return name;
    }

    /**
     * Queries the display name of a Storage Access Framework Uri.
     *
     * @param context Application context.
     * @param uri Document Uri.
     * @return The display name (e.g. "Vehicles.csv"), or null if unavailable.
     */
    public static String getDisplayNameFromUri(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        String displayName = null;
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    displayName = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve display name for uri: " + uri, e);
        }
        if (displayName == null && uri.getLastPathSegment() != null) {
            displayName = uri.getLastPathSegment();
        }
        return displayName;
    }

    /**
     * Builds an in-memory Set of existing event keys (catId + "|" + event + "|" + date + "|" + recur)
     * to facilitate O(1) duplicate detection during batch imports.
     *
     * @param db SQLite database.
     * @return Set of unique key strings for all existing events.
     */
    public static Set<String> loadExistingEventKeys(SQLiteDatabase db) {
        Set<String> keys = new HashSet<>();
        if (db == null) return keys;
        Cursor cursor = db.rawQuery("SELECT catId, event, date, recur FROM event", null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long catId = cursor.getLong(0);
                    String event = cursor.getString(1);
                    String date = cursor.getString(2);
                    int recur = cursor.getInt(3);

                    String isoDate = parseAndFormatIsoDate(date);
                    if (isoDate == null) isoDate = (date != null ? date.trim() : "");
                    String key = catId + "|" + (event != null ? event.trim().toLowerCase(Locale.US) : "")
                            + "|" + isoDate + "|" + recur;
                    keys.add(key);
                }
            } finally {
                cursor.close();
            }
        }
        return keys;
    }

    /**
     * Determines whether a row is a CSV header row by inspecting common column keywords.
     *
     * @param row List of string tokens in the first row.
     * @return True if row matches recognized header patterns.
     */
    public static boolean isHeaderRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        for (String token : row) {
            String t = token.trim().toLowerCase(Locale.US);
            if (t.equals("category") || t.equals("cat") || t.equals("category_name") ||
                t.equals("event") || t.equals("events") || t.equals("title") || t.equals("name") ||
                t.equals("date") || t.equals("recur") || t.equals("recurrence")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates and parses a date string into canonical ISO-8601 (yyyy-MM-dd) format.
     * Supports strict yyyy-MM-dd and handles common fallbacks (yyyy/MM/dd, MM/dd/yyyy).
     *
     * @param dateString Date string from CSV.
     * @return Formatted yyyy-MM-dd string, or null if invalid.
     */
    public static String parseAndFormatIsoDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateString.trim();

        // 1. Primary: Strict ISO-8601 yyyy-MM-dd
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        isoFormat.setLenient(false);
        try {
            Date d = isoFormat.parse(trimmed);
            return isoFormat.format(d);
        } catch (ParseException ignored) {
        }

        // 2. Fallbacks for user-edited CSVs: yyyy/MM/dd, MM/dd/yyyy, MM-dd-yyyy
        String[] fallbackPatterns = new String[]{"yyyy/MM/dd", "MM/dd/yyyy", "MM-dd-yyyy"};
        for (String pattern : fallbackPatterns) {
            SimpleDateFormat alt = new SimpleDateFormat(pattern, Locale.US);
            alt.setLenient(false);
            try {
                Date d = alt.parse(trimmed);
                return isoFormat.format(d);
            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    /**
     * Resolves a category ID for the given category name. Looks up existing category
     * in the SQLite database (case-insensitive); if not found, creates a new category record.
     *
     * @param db SQLite database.
     * @param categoryName Name of the category.
     * @param categoryCache In-memory cache mapping lowercase category names to IDs.
     * @param counters Array tracking [categoriesCreatedCount].
     * @return The resolved category ID.
     */
    public static long resolveOrCreateCategory(SQLiteDatabase db, String categoryName,
                                                Map<String, Long> categoryCache, int[] counters) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return -1;
        }
        String name = categoryName.trim();
        String lookupKey = name.toLowerCase(Locale.US);

        if (categoryCache.containsKey(lookupKey)) {
            return categoryCache.get(lookupKey);
        }

        long catId = -1;
        Cursor cursor = db.rawQuery("SELECT _id FROM category WHERE category = ? COLLATE NOCASE",
                new String[]{name});
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    catId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        if (catId == -1) {
            ContentValues cv = new ContentValues();
            cv.put("category", name);
            cv.put("type", 0);
            catId = db.insert("category", null, cv);
            if (catId != -1) {
                counters[0]++; // Increment categoriesCreated count
            }
        }

        if (catId != -1) {
            categoryCache.put(lookupKey, catId);
        }
        return catId;
    }

    /**
     * Imports CSV data from a Reader into the SQLite database.
     * Uses atomic transactions, parameterized inserts, and automatic category routing.
     *
     * @param db SQLite database.
     * @param reader Reader providing CSV data.
     * @param defaultCategoryId Default category ID to use when CSV lacks a category column.
     * @return CsvImportResult with summary metrics and diagnostics.
     */
    public static CsvImportResult importCsv(SQLiteDatabase db, Reader reader, long defaultCategoryId) {
        return importCsv(db, reader, defaultCategoryId, null, null, true);
    }

    /**
     * Imports CSV data from a Reader with full support for duplicate skipping and shared category caches.
     *
     * @param db SQLite database.
     * @param reader Reader providing CSV data.
     * @param defaultCategoryId Default category ID fallback.
     * @param categoryCache Optional shared in-memory cache for resolved categories.
     * @param existingEventKeys Optional shared in-memory set of existing event keys for deduplication.
     * @param manageTransaction Whether this method should manage beginTransaction/setTransactionSuccessful.
     * @return CsvImportResult with metrics.
     */
    public static CsvImportResult importCsv(SQLiteDatabase db, Reader reader, long defaultCategoryId,
                                            Map<String, Long> categoryCache, Set<String> existingEventKeys,
                                            boolean manageTransaction) {
        if (db == null || reader == null) {
            return CsvImportResult.failure("Database or reader is null");
        }

        BufferedReader br = (reader instanceof BufferedReader)
                ? (BufferedReader) reader
                : new BufferedReader(reader);

        List<List<String>> records;
        try {
            records = parseRecords(br);
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV records", e);
            return CsvImportResult.failure("Failed to read CSV: " + e.getMessage());
        }

        if (records.isEmpty()) {
            return CsvImportResult.success(0, 0, 0, 0, new ArrayList<String>());
        }

        int colCategory = -1;
        int colEvent = -1;
        int colDate = -1;
        int colRecur = -1;

        int startIndex = 0;
        List<String> firstRow = records.get(0);

        if (isHeaderRow(firstRow)) {
            startIndex = 1;
            for (int col = 0; col < firstRow.size(); col++) {
                String h = firstRow.get(col).trim().toLowerCase(Locale.US);
                if (h.equals("category") || h.equals("cat") || h.equals("category_name")) {
                    colCategory = col;
                } else if (h.equals("event") || h.equals("events") || h.equals("title") ||
                           h.equals("name") || h.equals("description")) {
                    colEvent = col;
                } else if (h.equals("date") || h.equals("event_date") || h.equals("due_date")) {
                    colDate = col;
                } else if (h.equals("recur") || h.equals("recurrence") || h.equals("repeat") ||
                           h.equals("interval") || h.equals("days")) {
                    colRecur = col;
                }
            }

            // Fallback for header with unmatched event column
            if (colEvent == -1) {
                colEvent = (colCategory == 0) ? 1 : 0;
            }
            if (colDate == -1) {
                colDate = (colEvent == 0) ? 1 : 2;
            }
        } else {
            // Legacy / unheadered CSV: positional mapping based on column count
            int tokenCount = firstRow.size();
            if (tokenCount == 2) {
                colEvent = 0;
                colDate = 1;
            } else if (tokenCount == 3) {
                colEvent = 0;
                colDate = 1;
                colRecur = 2;
            } else if (tokenCount >= 4) {
                colCategory = 0;
                colEvent = 1;
                colDate = 2;
                colRecur = 3;
            } else {
                colEvent = 0;
                colDate = 1;
            }
        }

        int totalDataRows = records.size() - startIndex;
        int importedCount = 0;
        int skippedCount = 0;
        int[] categoryCounter = new int[]{0};
        List<String> errors = new ArrayList<>();
        if (categoryCache == null) {
            categoryCache = new HashMap<>();
        }
        if (existingEventKeys == null) {
            existingEventKeys = loadExistingEventKeys(db);
        }

        if (manageTransaction) {
            db.beginTransaction();
        }
        try {
            for (int r = startIndex; r < records.size(); r++) {
                List<String> row = records.get(r);
                int rowNumber = r + 1;

                if (row.isEmpty() || (row.size() == 1 && row.get(0).trim().isEmpty())) {
                    continue; // Skip blank line
                }

                String eventName = (colEvent >= 0 && colEvent < row.size()) ? row.get(colEvent).trim() : "";
                if (eventName.isEmpty()) {
                    skippedCount++;
                    errors.add("Row " + rowNumber + ": Event name is empty.");
                    continue;
                }

                String rawDate = (colDate >= 0 && colDate < row.size()) ? row.get(colDate).trim() : "";
                String isoDate = parseAndFormatIsoDate(rawDate);
                if (isoDate == null) {
                    skippedCount++;
                    errors.add("Row " + rowNumber + " ('" + eventName + "'): Invalid date format '" + rawDate + "'. Expected yyyy-MM-dd.");
                    continue;
                }

                int recur = 0;
                if (colRecur >= 0 && colRecur < row.size()) {
                    String recurStr = row.get(colRecur).trim();
                    if (!recurStr.isEmpty()) {
                        try {
                            recur = Integer.parseInt(recurStr);
                        } catch (NumberFormatException npe) {
                            Log.w(TAG, "Row " + rowNumber + ": Invalid recur '" + recurStr + "', defaulting to 0.");
                        }
                    }
                }

                // Determine target category ID
                long targetCatId = defaultCategoryId;
                if (colCategory >= 0 && colCategory < row.size()) {
                    String catName = row.get(colCategory).trim();
                    if (!catName.isEmpty()) {
                        long resolved = resolveOrCreateCategory(db, catName, categoryCache, categoryCounter);
                        if (resolved != -1) {
                            targetCatId = resolved;
                        }
                    }
                }

                // Check for duplicates based on (catId, event, isoDate, recur)
                String eventKey = targetCatId + "|" + eventName.toLowerCase(Locale.US) + "|" + isoDate + "|" + recur;
                if (existingEventKeys.contains(eventKey)) {
                    skippedCount++;
                    // Skipped as duplicate
                    continue;
                }

                ContentValues values = new ContentValues();
                values.put("catId", targetCatId);
                values.put("event", eventName);
                values.put("date", isoDate);
                values.put("recur", recur);

                long insertId = db.insert("event", null, values);
                if (insertId != -1) {
                    importedCount++;
                    existingEventKeys.add(eventKey);
                } else {
                    skippedCount++;
                    errors.add("Row " + rowNumber + " ('" + eventName + "'): Database insert failed.");
                }
            }

            if (manageTransaction) {
                db.setTransactionSuccessful();
            }
        } finally {
            if (manageTransaction) {
                db.endTransaction();
            }
        }

        return CsvImportResult.success(totalDataRows, importedCount, skippedCount,
                categoryCounter[0], errors);
    }

    /**
     * Imports CSV data from an InputStream into the SQLite database.
     *
     * @param db SQLite database.
     * @param inputStream Input stream providing CSV bytes (read as UTF-8).
     * @param defaultCategoryId Default category ID fallback.
     * @return CsvImportResult with summary metrics.
     */
    public static CsvImportResult importCsv(SQLiteDatabase db, InputStream inputStream, long defaultCategoryId) {
        return importCsv(db, inputStream, defaultCategoryId, null, null, true);
    }

    /**
     * Imports CSV data from an InputStream with shared category and duplicate caches.
     *
     * @param db SQLite database.
     * @param inputStream Input stream providing CSV bytes (read as UTF-8).
     * @param defaultCategoryId Default category ID fallback.
     * @param categoryCache Optional shared in-memory cache for resolved categories.
     * @param existingEventKeys Optional shared in-memory set of existing event keys.
     * @param manageTransaction Whether this method manages begin/end transaction.
     * @return CsvImportResult with summary metrics.
     */
    public static CsvImportResult importCsv(SQLiteDatabase db, InputStream inputStream, long defaultCategoryId,
                                            Map<String, Long> categoryCache, Set<String> existingEventKeys,
                                            boolean manageTransaction) {
        if (inputStream == null) {
            return CsvImportResult.failure("InputStream is null");
        }
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return importCsv(db, reader, defaultCategoryId, categoryCache, existingEventKeys, manageTransaction);
        } catch (IOException e) {
            Log.e(TAG, "Error closing InputStream", e);
            return CsvImportResult.failure(e.getMessage());
        }
    }

    /**
     * Imports multiple CSV documents represented by Storage Access Framework URIs.
     * Extracts filename stems to infer categories when not present in headers,
     * resolves/auto-creates categories, and skips duplicates across all files within a single transaction.
     *
     * @param context Application context for ContentResolver.
     * @param db SQLite database.
     * @param uris List of document URIs.
     * @param defaultCategoryId Fallback category ID if filename and header inference both fail.
     * @return Aggregated CsvImportResult.
     */
    public static CsvImportResult importMultipleCsvUris(Context context, SQLiteDatabase db,
                                                        List<Uri> uris, long defaultCategoryId) {
        if (context == null || db == null || uris == null || uris.isEmpty()) {
            return CsvImportResult.failure("Invalid context, database, or empty URIs list");
        }

        int totalDataRows = 0;
        int totalImported = 0;
        int totalSkipped = 0;
        int initialCategoriesCreated = 0;
        List<String> combinedErrors = new ArrayList<>();
        Map<String, Long> sharedCategoryCache = new HashMap<>();
        Set<String> sharedEventKeys = loadExistingEventKeys(db);

        db.beginTransaction();
        try {
            for (Uri uri : uris) {
                if (uri == null) continue;

                // Determine category from filename stem if applicable
                String displayName = getDisplayNameFromUri(context, uri);
                String inferredCategory = inferCategoryFromFilename(displayName);
                long fileDefaultCatId = defaultCategoryId;

                if (inferredCategory != null && !inferredCategory.isEmpty()) {
                    int[] counter = new int[]{0};
                    long resolved = resolveOrCreateCategory(db, inferredCategory, sharedCategoryCache, counter);
                    if (resolved != -1) {
                        fileDefaultCatId = resolved;
                    }
                    initialCategoriesCreated += counter[0];
                }

                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        CsvImportResult res = importCsv(db, is, fileDefaultCatId,
                                sharedCategoryCache, sharedEventKeys, false);
                        totalDataRows += res.getTotalRows();
                        totalImported += res.getImportedCount();
                        totalSkipped += res.getSkippedCount();
                        initialCategoriesCreated += res.getCategoriesCreated();
                        combinedErrors.addAll(res.getErrors());
                    } else {
                        combinedErrors.add("Unable to open stream for URI: " + uri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error importing URI: " + uri, e);
                    combinedErrors.add("Error reading " + (displayName != null ? displayName : uri) + ": " + e.getMessage());
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return CsvImportResult.success(totalDataRows, totalImported, totalSkipped,
                initialCategoriesCreated, combinedErrors);
    }

    /**
     * Imports CSV data from a File into the SQLite database.
     *
     * @param context Application context.
     * @param db SQLite database.
     * @param file CSV file on disk.
     * @param defaultCategoryId Default category ID fallback.
     * @return CsvImportResult with summary metrics.
     */
    public static CsvImportResult importCsv(Context context, SQLiteDatabase db, File file, long defaultCategoryId) {
        if (file == null || !file.exists()) {
            return CsvImportResult.failure("File does not exist: " + (file != null ? file.getAbsolutePath() : "null"));
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            return importCsv(db, fis, defaultCategoryId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to import CSV from " + file.getAbsolutePath(), e);
            return CsvImportResult.failure(e.getMessage());
        }
    }

    /**
     * Imports CSV data from a raw string into the SQLite database. Useful for unit testing.
     *
     * @param db SQLite database.
     * @param csvContent String containing CSV content.
     * @param defaultCategoryId Default category ID fallback.
     * @return CsvImportResult with summary metrics.
     */
    public static CsvImportResult importCsvString(SQLiteDatabase db, String csvContent, long defaultCategoryId) {
        if (csvContent == null) {
            return CsvImportResult.failure("CSV content string is null");
        }
        return importCsv(db, new StringReader(csvContent), defaultCategoryId);
    }
}
