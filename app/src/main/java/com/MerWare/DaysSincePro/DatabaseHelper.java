package com.MerWare.DaysSincePro;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
        super(context, DATABASE_NAME, null, 3); // third param is version
        // 1 original version
        // 2 add history table
        // 3 add end date column
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
                + "event TEXT, " + "date DATE, " + "recur INTEGER, end_date DATE)";

        db.execSQL(sql2);

        String sql3 = "CREATE TABLE IF NOT EXISTS history ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT, "
                + "UNIQUE(eventId, date))";

        db.execSQL(sql3);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // history table new for version 2
        String sql3 = "CREATE TABLE IF NOT EXISTS history ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "eventId INTEGER, catID INTEGER, date DATE, onTime INTEGER, note TEXT, UNIQUE(eventId, date))";

        String sql4 = "ALTER TABLE event ADD COLUMN end_date DATE";

        if (oldVersion == 1 && newVersion == 2)
        {
            db.execSQL(sql3);
        }
        if (oldVersion == 1 && newVersion == 3)
        {
            db.execSQL(sql3);
            db.execSQL(sql4);
        }
        else if (oldVersion == 2 && newVersion == 3)
        {
            db.execSQL(sql4);
        }
        else {

            db.execSQL("DROP TABLE IF EXISTS category");
            db.execSQL("DROP TABLE IF EXISTS event");
            db.execSQL("DROP TABLE IF EXISTS history");

            onCreate(db);
        }
    }

}
