package com.merware.dayssincepro;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "alex_db";

    // Single shared instance so the whole app uses one connection to alex_db,
    // instead of every Activity/Fragment opening its own.
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    // Closes and drops the shared instance so a fresh connection is opened next time
    // getInstance() is called. Used by restore, right before the db file is replaced.
    public static synchronized void closeInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 4); // third param is version
        // 1 original version
        // 2 add history table
        // 3 add end date column
        // 4 add details + last_notified_date columns
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        db.disableWriteAheadLogging();
        super.onOpen(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String sql = "CREATE TABLE IF NOT EXISTS category ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category TEXT, type INTEGER)";
        db.execSQL(sql);

        String sql2 = "CREATE TABLE IF NOT EXISTS event ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "catId INTEGER, "
            + "event TEXT, " + "date DATE, " + "recur INTEGER, "
            + "end_date DATE, details TEXT, last_notified_date DATE)";

        db.execSQL(sql2);

        String sql3 = "CREATE TABLE IF NOT EXISTS history ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT, "
                + "UNIQUE(eventId, date))";

        db.execSQL(sql3);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        List<String> statements = getMigrationStatements(oldVersion, newVersion);
        if (statements == null) {
            // No known incremental migration path for this version transition. The
            // previous behavior here silently DROPped and recreated every table (category,
            // event, history) for any unmatched (oldVersion, newVersion) pair, which meant
            // any user on an older version than expected would lose all their data with no
            // warning. Refuse loudly instead - a crash surfaces the problem for a fix,
            // rather than silently destroying user data.
            throw new IllegalStateException("No migration path from DB version "
                    + oldVersion + " to " + newVersion);
        }
        for (String sql : statements) {
            db.execSQL(sql);
        }
    }

    // history table, new for version 2.
    static final String CREATE_HISTORY_TABLE_SQL = "CREATE TABLE IF NOT EXISTS history ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT, UNIQUE(eventId, date))";

    // event.end_date column, new for version 3.
    static final String ADD_END_DATE_COLUMN_SQL = "ALTER TABLE event ADD COLUMN end_date DATE";

        // event.details and event.last_notified_date columns, new for version 4.
        static final String ADD_DETAILS_COLUMN_SQL = "ALTER TABLE event ADD COLUMN details TEXT";
        static final String ADD_LAST_NOTIFIED_DATE_COLUMN_SQL =
            "ALTER TABLE event ADD COLUMN last_notified_date DATE";

    /**
     * Returns the ordered SQL statements needed to migrate a database from oldVersion to
     * newVersion, by applying each intermediate version step in sequence - rather than
     * matching specific (oldVersion, newVersion) pairs combinatorially, which silently
     * fails to cover every possible starting version as new schema versions are added.
     * Pure function (no Android types), so it's unit-testable on the plain JVM. Returns
     * null if any step in the chain has no known migration (an unsupported/invalid
     * transition), so the caller can refuse rather than fall back to a destructive rebuild.
     */
    static List<String> getMigrationStatements(int oldVersion, int newVersion) {
        if (oldVersion < 1 || newVersion < oldVersion) {
            return null;
        }
        List<String> statements = new ArrayList<>();
        for (int v = oldVersion; v < newVersion; v++) {
            List<String> step = getStepStatements(v, v + 1);
            if (step == null) {
                return null;
            }
            statements.addAll(step);
        }
        return statements;
    }

    private static List<String> getStepStatements(int fromVersion, int toVersion) {
        if (fromVersion == 1 && toVersion == 2) {
            List<String> step = new ArrayList<>();
            step.add(CREATE_HISTORY_TABLE_SQL);
            return step;
        }
        if (fromVersion == 2 && toVersion == 3) {
            List<String> step = new ArrayList<>();
            step.add(ADD_END_DATE_COLUMN_SQL);
            return step;
        }
        if (fromVersion == 3 && toVersion == 4) {
            List<String> step = new ArrayList<>();
            step.add(ADD_DETAILS_COLUMN_SQL);
            step.add(ADD_LAST_NOTIFIED_DATE_COLUMN_SQL);
            return step;
        }
        return null;
    }

}
