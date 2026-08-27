package com.MerWare.DaysSincePro;

import java.util.Calendar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class HistoryActivity extends ListActivity {

    protected SQLiteDatabase db;
    private ListView lv;

    private Cursor cursor;

    private long eventId = 0;
    private long catId = 0;

    private String event;
    private Button addButton;
    private Button cancelButton;

    private TextView onTime;
    private TextView timesThisYear;
    private TextView timesThisMonth;

    private TextView tvInterval;

    long removeId;

    private static final int ADD_HISTORY_ACTIVITY = 1;
    private static final int EDIT_HISTORY_ACTIVITY = 2;

    static final private int MENU_EDIT = Menu.FIRST;
    static final private int MENU_REMOVE = Menu.FIRST + 1;

    SharedPreferences preferences;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String sTheme = preferences.getString("theme", "0");
        int theme = Integer.parseInt(sTheme);

        if (theme == 1) { // dark
            setTheme(R.style.AppDialogTheme2);
        } else {// 0 light
            setTheme(R.style.AppDialogTheme);
        }

        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, MENU_EDIT, Menu.NONE + 1, R.string.edit);
        menu.add(1, MENU_REMOVE, Menu.NONE + 2, R.string.remove);
    }

    void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void listData() {

        // _id is required for SimpleCursorAdapter
        cursor = db.query("history", new String[] { "_id", "date", "onTime",
                "note" }, "eventId = " + eventId, null, null, null, "date");

        String[] from = new String[] { "date" };
        int[] to = new int[] { R.id.happened };

        RowAdapter cursorAdapter = new RowAdapter(this, R.layout.row, cursor,
                from, to);

        setListAdapter(cursorAdapter);

        // only working method to set to bottom
        // this does not work:    lv.setStackFromBottom(true);
        lv.setSelection(cursorAdapter.getCount() - 1);

        // calculate stats
        String sql;

        sql = "select recur from event where _Id = " + eventId;
        cursor = db.rawQuery(sql, null);
        int nRecur = 0;

        // if recurring show percentage
        if (cursor.moveToFirst())
        {
            nRecur = cursor.getInt(0);
        }

        if (nRecur > 0) {

            sql = "select round(sum(onTime) * 100.0 / count(*),2) from history where eventId = "
                    + eventId;
            cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst()) {

                onTime.setText(getString(R.string.percent_on_time) + " " + cursor.getFloat(0) + " %");
            }
        }
        else
        {
            onTime.setText("");
            onTime.setVisibility(View.GONE);
        }

        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH) + 1;

        // mMonth need to prepend...
        String sMonth = String.format("%02d", mMonth);

        sql = "select count(*) from history where eventId = " + eventId
                + " and strftime('%Y', date) = '" + mYear + "'";

        cursor = db.rawQuery(sql, null);

        int nTimesThisYear = 0;
        int nTimesThisMonth = 0;

        if (cursor.moveToFirst()) {
            nTimesThisYear = cursor.getInt(0);
            timesThisYear.setText(getString(R.string.times_this_year) + " " + nTimesThisYear);
        }

        sql = "select count(*) from history where eventId = " + eventId
                + " and strftime('%Y', date) = '" + mYear
                + "' and strftime('%m', date) = '" + sMonth + "'";

        cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            nTimesThisMonth = cursor.getInt(0);
            timesThisMonth.setText(getString(R.string.times_this_month) + " " + nTimesThisMonth);
        }

        intervals();
    }

    private void intervals()
    {
        int max_interval = 0, min_interval = 0, avg_interval = 0;
        String sql = "with intervals as ( ";
        sql += " SELECT julianday( LEAD(date, 1, 0) OVER(ORDER BY date ASC) ) - julianday(date) as days_diff";
        sql += " FROM history where eventId = " + eventId;
        sql += " order by date)";
        String max_sql = sql + "select max(days_diff) from intervals where days_diff > 0";
        String min_sql = sql + "select min(days_diff) from intervals where days_diff > 0";
        String avg_sql = sql + "select avg(days_diff) from intervals where days_diff > 0";

        try {
            sql = max_sql;
            cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst()) {
                max_interval = cursor.getInt(0);
            }

            sql = min_sql;
            cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst()) {
                min_interval = cursor.getInt(0);
            }
            sql = avg_sql;
            cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst()) {
                avg_interval = cursor.getInt(0);
            }

           // tvInterval.setText(getString(R.string.interval), max_interval, min_interval, avg_interval );

            String msg = getString(R.string.interval, max_interval, min_interval, avg_interval);
            tvInterval.setText(msg);

        }
        catch (SQLException ex)
        {
            Log.wtf("failed", "sql failed:" + sql);
        }

        if (max_interval + min_interval + avg_interval == 0)
            tvInterval.setVisibility(View.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String sTheme = preferences.getString("theme", "0");
        int theme = Integer.parseInt(sTheme);

        if (theme == 1) { // dark
            setTheme(R.style.AppDialogTheme2);

        } else {// 0 light

            setTheme(R.style.AppDialogTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);

        addButton = (Button) findViewById(R.id.addButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        onTime = (TextView) findViewById(R.id.onTime);
        timesThisYear = (TextView) findViewById(R.id.timesThisYear);
        timesThisMonth = (TextView) findViewById(R.id.timesThisMonth);
        tvInterval = (TextView) findViewById(R.id.interval);

        Intent intent = getIntent();
        eventId = intent.getLongExtra("eventId", 0);
        catId = intent.getLongExtra("catId", 0);

        db = (new DatabaseHelper(this)).getWritableDatabase();

        // get event Name to set title

        cursor = db.query("event", new String[] { "event" },
                "_id = " + eventId, null, null, null, null);

        cursor.moveToFirst();
        event = cursor.getString(0);

        setTitle(getString(R.string.history) + " : " + event);

        // allow click
        lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        registerForContextMenu(lv);

        addButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                newList();
            }

        });

        cancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cancel();
            }

        });

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                editItem(position, id);
            }
        });

        listData();

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        AdapterView.AdapterContextMenuInfo menuInfo;
        menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {

            case MENU_EDIT:
                editItem(menuInfo.position, menuInfo.id);
                break;

            case MENU_REMOVE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.remove_item);
                builder.setMessage(R.string.are_you_sure);
                builder.setPositiveButton(R.string.yes, yesNoDialogClickListener);
                builder.setNegativeButton(R.string.no, yesNoDialogClickListener);
                builder.show();

                removeId = menuInfo.id;
                break;
        }

        return true;
    }

    DialogInterface.OnClickListener yesNoDialogClickListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    int rc = db.delete("history", "_id=" + removeId, null);

                    if (rc == 0)
                        showToast("remove failed");

                    listData();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        }
    };

    public class RowAdapter extends SimpleCursorAdapter {

        private Cursor c;
        private Context context;

        public RowAdapter(Context context, int layout, Cursor c, String[] from,
                          int[] to) {
            super(context, layout, c, from, to);

            this.context = context;
            this.c = c;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row, parent, false);

            c.moveToPosition(position);

            TextView textView2 = (TextView) rowView.findViewById(R.id.happened);
            textView2.setText(c.getString(1));

            ImageView noteImage = (ImageView) rowView
                    .findViewById(R.id.haveNote);

            if (c.getString(3) != null) {
                if (c.getString(3).length() > 0) {
                    noteImage.setImageDrawable(getResources().getDrawable(
                            R.drawable.note));

                } else {
                    noteImage.setImageDrawable(null);
                }
            }

            if (c.getInt(2) == 0) {

            } else {

                textView2.setTextColor(context.getResources().getColor(
                        R.color.holo_green_dark));
            }

            return rowView;
        }
    }

    String mode;

    private void newList() {

        Intent intent = new Intent(HistoryActivity.this, EditHistory.class);

        mode = "Add";
        intent.putExtra("mode", mode);
        intent.putExtra("eventId", eventId);
        intent.putExtra("event", event);

        startActivityForResult(intent, ADD_HISTORY_ACTIVITY);
    }

    void editItem(int position, final long historyId) {

        Cursor c = (Cursor) lv.getItemAtPosition(position);
        final String date = c.getString(1);
        int onTime = c.getInt(2);
        String note = c.getString(3);

        Intent intent = new Intent(HistoryActivity.this, EditHistory.class);

        mode = "Edit";
        intent.putExtra("mode", mode);
        intent.putExtra("historyId", historyId);
        intent.putExtra("event", event);
        intent.putExtra("date", date);
        intent.putExtra("onTime", onTime);
        intent.putExtra("note", note);

        startActivityForResult(intent, EDIT_HISTORY_ACTIVITY);

    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String note;
        String dateText;
        boolean isOnTime;
        int timeVal = 0;
        ContentValues values = new ContentValues();

        switch (requestCode) {

            case ADD_HISTORY_ACTIVITY:

                if (resultCode == RESULT_OK) {

                    dateText = data.getStringExtra("date");
                    isOnTime = data.getBooleanExtra("onTime", false);
                    note = data.getStringExtra("note");

                    if (isOnTime)
                        timeVal = 1;

                    values.put("eventId", eventId);
                    values.put("catId", catId);
                    values.put("date", dateText);
                    values.put("onTime", timeVal);
                    values.put("note", note);

                    //  Log.wtf("insert", "eventId " + eventId + " dateText "
                    //          + dateText + " onTime " + timeVal + " note " + note);

                    if (db.insert("history", "history", values) == -1) {
                        // unique constrain
                        showToast(event + " happened on " + dateText
                                + " was already noted.");
                    }
                    listData();
                }

                break;
            case EDIT_HISTORY_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    dateText = data.getStringExtra("date");
                    isOnTime = data.getBooleanExtra("onTime", false);
                    note = data.getStringExtra("note");

                    if (isOnTime)
                        timeVal = 1;

                    long historyId = data.getLongExtra("historyId", 0);

                    values.put("date", dateText);
                    values.put("note", note);
                    values.put("onTime", timeVal);

                    // Log.wtf("update", "note is " + note);

                    if (db.update("history", values, "_id =" + historyId, null) == -1) {
                        showToast("sorry update failed");
                    }

                    listData();
                }

                break;
        }
    }

    public void onBackPressed() {
        super.onBackPressed();
        cursor.close();
        // db.close();
    }

    // don't restart when phone change orientation.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
