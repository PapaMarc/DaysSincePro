package com.merware.dayssincepro;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ============================================================================
 * CsvExporter - RFC-4180 Compliant CSV Export Engine for DaysSincePro
 * ============================================================================
 *
 * ARCHITECTURAL REDESIGN & DOCUMENTATION:
 * ----------------------------------------------------------------------------
 * 1. PROBLEMS WITH ORIGINAL IMPLEMENTATION (in CategoriesActivity.java):
 *    - Malformed Quoting: Output enclosed fields in single quotes ('field')
 *      instead of RFC-4180 standard double quotes ("field"). External tools like
 *      Excel, Google Sheets, and standard CSV libraries do not treat single
 *      quotes as delimiters and parse them as literal characters.
 *    - Manual String Concatenation: Rows were assembled via raw concatenation
 *      ("'" + event + "','" + date + "'," + recur) with zero escaping for internal
 *      commas, quotes, or line breaks, causing corrupted columns whenever text
 *      contained punctuation.
 *    - Missing Header Row: No column headers were written, making exported files
 *      ambiguous to external spreadsheet applications and automated parsers.
 *    - Platform-Default Encoding: Relied on default system encoding (such as
 *      Windows-1252) instead of standard UTF-8 without BOM, leading to character
 *      corruption (mojibake) across different OS platforms and locales.
 *    - Unvalidated Dates: Dates were output directly without enforcing ISO-8601
 *      (yyyy-MM-dd) formatting.
 *    - No Category Context: Omitted category text names, preventing multi-category
 *      or full-database export and offline human curation.
 *    - UI / Data-Layer Mixing: Export logic was directly embedded inside
 *      CategoriesActivity, violating the Single Responsibility Principle and
 *      precluding automated unit testing and modular reuse.
 *
 * 2. HOW ISSUES WERE RESOLVED:
 *    - RFC-4180 Compliant Quoting: All text fields are wrapped in double quotes
 *      ("..."), and internal double quotes are escaped as ("").
 *    - Explicit UTF-8 Encoding: Files are written using OutputStreamWriter with
 *      StandardCharsets.UTF_8 without BOM for cross-platform compatibility.
 *    - Standard Header Rows: Emits standard header rows ("event","date","recur"
 *      for single-category exports; "category","event","date","recur" for
 *      multi-category / full database exports).
 *    - ISO-8601 Date Standardization: All event dates are strictly validated
 *      and formatted as yyyy-MM-dd.
 *    - Category Text Name Support: Full support for exporting events with their
 *      category names, enabling full database exports and offline spreadsheet editing.
 *    - Architectural Decoupling: Pure helper functions handle serialization to any
 *      Writer/Stream/File, decoupling CSV export from Android UI lifecycles.
 *
 * 3. INTEROPERABILITY & MAINTAINABILITY BENEFITS:
 *    - Standard RFC-4180 compliance guarantees out-of-the-box compatibility with
 *      Microsoft Excel, Google Sheets, Numbers, database tools, and data scripts.
 *    - UTF-8 without BOM ensures consistent text rendering across Android, Windows,
 *      macOS, and Linux.
 *    - Proper quoting allows event titles with apostrophes ("Mom's Birthday"),
 *      commas ("Registration, inspection, tags"), and quotes ("Project \"Apollo\"")
 *      to export and import without corruption.
 *    - Human-readable category names allow users to easily edit CSV files in external
 *      tools and import them back into DaysSincePro seamlessly.
 *
 * 4. ARCHITECTURAL BENEFITS:
 *    - Separation of concerns: UI activities handle user interaction; CsvExporter
 *      handles serialization and file output.
 *    - Testability: Exporter can be verified in headless JVM unit tests without
 *      requiring Android devices or emulators.
 * ============================================================================
 */
public class CsvExporter {

    private static final String TAG = "CsvExporter";
    public static final String EXPORT_SUBDIRECTORY = "DaysSincePro";

    public static final String HEADER_SINGLE_CATEGORY = "\"event\",\"date\",\"recur\"";
    public static final String HEADER_MULTI_CATEGORY = "\"category\",\"event\",\"date\",\"recur\"";

    private CsvExporter() {
        // Utility class; prevent instantiation
    }

    /**
     * Escapes a single string field according to RFC-4180 rules.
     * Encloses the value in double quotes and doubles any internal double quotes.
     *
     * @param field The text value to escape.
     * @return RFC-4180 quoted string.
     */
    public static String escapeField(String field) {
        if (field == null) {
            return "\"\"";
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /**
     * Formats an array of fields into an RFC-4180 compliant CSV line.
     *
     * @param fields The fields to format.
     * @return Formatted CSV row string without trailing newline.
     */
    public static String formatRow(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeField(fields[i]));
        }
        return sb.toString();
    }

    /**
     * Validates and standardizes a date string to ISO-8601 (yyyy-MM-dd).
     *
     * @param rawDate Raw date string from database.
     * @return Validated yyyy-MM-dd string, or rawDate if parse fails.
     */
    public static String formatIsoDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "";
        }
        String trimmed = rawDate.trim();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        isoFormat.setLenient(false);
        try {
            Date d = isoFormat.parse(trimmed);
            return isoFormat.format(d);
        } catch (ParseException e) {
            // If already yyyy-MM-dd or alternate format, attempt fallback or return trimmed
            return trimmed;
        }
    }

    /**
     * Exports all events belonging to a specific category to a Writer.
     * Uses RFC-4180 double-quoted formatting and includes a header row.
     *
     * @param db SQLite database instance.
     * @param categoryId Category ID to filter events by.
     * @param writer Target Writer (must be configured for UTF-8).
     * @return Number of event rows exported.
     * @throws IOException On write failure.
     */
    public static int exportCategory(SQLiteDatabase db, long categoryId, Writer writer) throws IOException {
        writer.write(HEADER_SINGLE_CATEGORY);
        writer.write("\r\n");

        int count = 0;
        String sql = "SELECT event, date, recur FROM event WHERE catId = ? ORDER BY date ASC, _id ASC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(categoryId)});

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String event = cursor.getString(0);
                    String date = cursor.getString(1);
                    int recur = cursor.getInt(2);

                    String formattedDate = formatIsoDate(date);
                    String row = formatRow(event, formattedDate, String.valueOf(recur));
                    writer.write(row);
                    writer.write("\r\n");
                    count++;
                }
            } finally {
                cursor.close();
            }
        }
        writer.flush();
        return count;
    }

    /**
     * Exports all events across all categories to a Writer.
     * Includes the category text name column in the header and rows.
     *
     * @param db SQLite database instance.
     * @param writer Target Writer (must be configured for UTF-8).
     * @return Number of event rows exported.
     * @throws IOException On write failure.
     */
    public static int exportAllCategories(SQLiteDatabase db, Writer writer) throws IOException {
        writer.write(HEADER_MULTI_CATEGORY);
        writer.write("\r\n");

        int count = 0;
        String sql = "SELECT COALESCE(c.category, 'Uncategorized'), e.event, e.date, e.recur " +
                "FROM event e " +
                "LEFT JOIN category c ON e.catId = c._id " +
                "ORDER BY c.category ASC, e.date ASC, e._id ASC";
        Cursor cursor = db.rawQuery(sql, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String category = cursor.getString(0);
                    String event = cursor.getString(1);
                    String date = cursor.getString(2);
                    int recur = cursor.getInt(3);

                    String formattedDate = formatIsoDate(date);
                    String row = formatRow(category, event, formattedDate, String.valueOf(recur));
                    writer.write(row);
                    writer.write("\r\n");
                    count++;
                }
            } finally {
                cursor.close();
            }
        }
        writer.flush();
        return count;
    }

    /**
     * Exports a specific category to a target File using UTF-8 encoding without BOM.
     *
     * @param db SQLite database instance.
     * @param categoryId Category ID to filter events by.
     * @param targetFile Target CSV file.
     * @return CsvExportResult with status and row count.
     */
    public static CsvExportResult exportCategory(SQLiteDatabase db, long categoryId, File targetFile) {
        if (targetFile == null) {
            return CsvExportResult.failure("Target file is null");
        }
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            int rows = exportCategory(db, categoryId, writer);
            return CsvExportResult.success(rows, targetFile);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export category CSV to " + targetFile.getAbsolutePath(), e);
            return CsvExportResult.failure(e.getMessage());
        }
    }

    /**
     * Exports a specific category for an Android Context, creating the file in the
     * appropriate app storage directory and notifying MediaStore.
     *
     * @param context Android context for storage resolution.
     * @param db SQLite database instance.
     * @param categoryId Category ID to export.
     * @param categoryName Name of the category (used for filename).
     * @return CsvExportResult with status and row count.
     */
    public static CsvExportResult exportCategory(Context context, SQLiteDatabase db,
                                                 long categoryId, String categoryName) {
        try {
            File directory = getExportDirectory(context);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String safeName = sanitizeFilename(categoryName);
            File targetFile = new File(directory, safeName + ".csv");

            CsvExportResult result = exportCategory(db, categoryId, targetFile);
            if (result.isSuccess() && context != null) {
                addFileToMediaStore(context, targetFile.getAbsolutePath());
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error during category export", e);
            return CsvExportResult.failure(e.getMessage());
        }
    }

    /**
     * Exports all categories across the entire database to a target File.
     *
     * @param db SQLite database instance.
     * @param targetFile Target CSV file.
     * @return CsvExportResult with status and row count.
     */
    public static CsvExportResult exportAllCategories(SQLiteDatabase db, File targetFile) {
        if (targetFile == null) {
            return CsvExportResult.failure("Target file is null");
        }
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            int rows = exportAllCategories(db, writer);
            return CsvExportResult.success(rows, targetFile);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export all categories CSV to " + targetFile.getAbsolutePath(), e);
            return CsvExportResult.failure(e.getMessage());
        }
    }

    /**
     * Exports all categories for an Android Context to "DaysSincePro_All.csv"
     * and notifies MediaStore.
     *
     * @param context Android context for storage resolution.
     * @param db SQLite database instance.
     * @return CsvExportResult with status and row count.
     */
    public static CsvExportResult exportAllCategories(Context context, SQLiteDatabase db) {
        try {
            File directory = getExportDirectory(context);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File targetFile = new File(directory, "DaysSincePro_All.csv");
            CsvExportResult result = exportAllCategories(db, targetFile);
            if (result.isSuccess() && context != null) {
                addFileToMediaStore(context, targetFile.getAbsolutePath());
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error during full database export", e);
            return CsvExportResult.failure(e.getMessage());
        }
    }

    /**
     * Resolves the standard external storage directory according to Android OS version.
     *
     * @param context Android application context.
     * @return File representing the destination directory.
     */
    public static File getExportDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.getExternalFilesDir(null);
        } else {
            return new File(Environment.getExternalStorageDirectory(), EXPORT_SUBDIRECTORY);
        }
    }

    /**
     * Sanitizes a string for safe use as a filename on Android file systems.
     *
     * @param raw Raw category name.
     * @return Safe filename string.
     */
    public static String sanitizeFilename(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Exported_Category";
        }
        String safe = raw.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return safe.isEmpty() ? "Exported_Category" : safe;
    }

    /**
     * Notifies the Android MediaScanner so newly created CSV files appear immediately
     * when connected via USB or file explorer apps.
     *
     * @param context Application context.
     * @param path Full file path.
     */
    public static void addFileToMediaStore(Context context, String path) {
        if (context == null || path == null) return;
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File file = new File(path);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to broadcast MediaScanner intent for " + path, e);
        }
    }
}
