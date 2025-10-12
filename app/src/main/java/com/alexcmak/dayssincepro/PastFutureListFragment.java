package com.alexcmak.dayssincepro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.ListFragment;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Date;

import android.database.sqlite.SQLiteDatabase;

/**
 * root class of DaysSinceFragment and DaysUntilFragment
 * Created by Alex on 12/12/2014.
 */

public class PastFutureListFragment extends ListFragment {

    private ListView lv;
    Context context;
    protected SQLiteDatabase db;
    private SimpleDate now;
    private long removeId;
    private String systemDateFormat;

    private static final int EDIT_ACTIVITY = 1;
    private static final int HISTORY_ACTIVITY = 4;

    AlarmHelper alarmHelp;
    public PastFutureListFragment() {
    }

    void showToast(String s) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show();
    }

    void showDialog(String s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Look");
        builder.setMessage(s);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        // allow click
        lv = getListView();

        lv.setTextFilterEnabled(true);
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                editItem(position, id);
            }
        });

        db = (new DatabaseHelper(context)).getWritableDatabase();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String text = preferences.getString("Categories", "");

        if (text == "") {
            text = getString(R.string.uncategorized);
        }

        getActivity().setTitle(text);

        listData();
    }

    TabKind kind;

    public void setKind(TabKind kind)
    {
        this.kind = kind;
        //   Log.wtf("dsa", "set kind to be " + kind);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.tab, container, false);

        context = rootView.getContext();

        alarmHelp = new AlarmHelper(context);

        return rootView;
    }

    SharedPreferences preferences;

    private String categories;
    private boolean isRestored = false;

    public void listData() {

        if (searchText != "") {
            listDataAjax(searchText);
            return;
        }

        String option = preferences.getString("event_sort_order", "0");
        int iOption = Integer.parseInt(option);
        String orderBy = null;

        String sql = "";

        try {

            orderBy = getOrderBy();

            now = new SimpleDate(new Date());

            String today = now.getDate(SimpleDate.DateStyle.YMD);

            Cursor cursor;

            sql = "select _id, catID, event, date, recur, end_date, date(date, '+' || recur || ' day') as nextdate from event ";

            String whereClause = "where ";

       //     Log.wtf("dsp", "listData() Kind is " + kind);

            if (kind == TabKind.DaysSince)
                whereClause += "date <= '" + today + "'";
            else {
                whereClause += "(date > '" + today + "' or recur > 0)";
            }

            categories = preferences.getString("CategoryIds", "");
            categories = categories.replaceAll("\\[", "").replaceAll("\\]", "");

            String[] items = categories.split(",");
            // showToast("cat: " + categories);

     //       Log.wtf("dsp", "categories is [" + categories + "]");

            if (categories.length() > 0 ) {
                whereClause += " and catID in (" + categories + ")";
            } else {
                if (isRestored) {
                    //       setTitle(R.string.all_categories);
                    isRestored = false;
                } else {
                    //whereClause = "";
                    //     setTitle(R.string.uncategorized);
                }
            }

            sql = sql + whereClause + " order by " + orderBy;

            //    showToast(sql);
            cursor = db.rawQuery(sql, null);

            String[] from = new String[]{"event", "date"}; // columns

            int[] to = new int[]{R.id.eventView, R.id.dateView};

            systemDateFormat = DateFormat.GetSystemDateFormat(context);

            // Log.wtf("past future", "system date format " + systemDateFormat);

            MyEventAdapter eventAdapter;
            eventAdapter = new MyEventAdapter(context, R.layout.event_item,
                    cursor, from, to, systemDateFormat, kind);

            setListAdapter(eventAdapter);

        } catch (Exception e) {
            showToast("Sorry, database problems." +e.getMessage());
            showDialog(sql);
        }
    }

    private String getOrderBy()
    {
        String option = preferences.getString("event_sort_order", "0");
        int iOption = Integer.parseInt(option);
        String orderBy = null;

        switch (iOption) {
            case 0:
                orderBy = null;
                break;
            case 1:
                orderBy = "event ASC";
                break;
            case 2:
                orderBy = "event DESC";
                break;
            case 3:
                orderBy = "date ASC";
                break;
            case 4:
                orderBy = "date DESC";
                break;
        }

        return orderBy;
    }

    String searchText = "";

    public void unsetSearchText()
    {
        searchText = "";
    }

    /**
     *
     * @param str - part of an event name regardless of category
     */
    public void listDataAjax(String str) {
        String sql = "";
        String orderBy = null;
        str = str.toUpperCase();
        searchText = str;

        try {
            orderBy = getOrderBy();

            now = new SimpleDate(new Date());

            String today = now.getDate(SimpleDate.DateStyle.YMD);
            String dateCondition = "date <= '" + today + "'";

            Cursor cursor;

            // use upper() for case insensitive

            sql = "select _id, catID, event, date, recur, end_date, date(date, '+' || recur || ' day') as nextdate from event ";
            String whereClause = "where ";

            whereClause = whereClause + " event like '%" + str + "%' and ";

            if (kind == TabKind.DaysSince)
                whereClause = whereClause + dateCondition;
            else {
                // buggy
                //whereClause = whereClause + "nextdate > '" + today + "'";
                whereClause = whereClause + "date > '" + today + "'";
            }

            sql = sql + whereClause + " order by " + orderBy;

            //   showToast(sql);

            cursor = db.rawQuery(sql, null);

            String[] from = new String[]{"event", "date"}; // columns

            int[] to = new int[]{R.id.eventView, R.id.dateView};

            MyEventAdapter eventAdapter;

            systemDateFormat = DateFormat.GetSystemDateFormat(context);

            // Log.wtf("past future", "ajax system date format " + systemDateFormat);
            eventAdapter = new MyEventAdapter(context, R.layout.event_item,
                    cursor, from, to, systemDateFormat, kind);

            setListAdapter(eventAdapter);

            // hmm.
            //    startManagingCursor(cursor);

        } catch (Exception e) {
            //   showToast("Sorry, database problems." + e.getMessage());
            showDialog(e.getMessage());
        }
    }

    // fill data when tab is redrawn.
    // when a dialog (i.e. EditActivity) uncover the activity, this method will be called.
    @Override
    public void onResume() {

        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);
        if (visible && isResumed())
        {
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            //onResume();

            listData();
        }
    }

    void editItem(int position, long id) {

        Cursor c = (Cursor) lv.getItemAtPosition(position);
        Intent intent = new Intent(context, EditEventActivity.class);
        intent.putExtra("id", id);

        // showToast("OK, catId on edit from cursor is " + c.getLong(1));

        intent.putExtra("catId", c.getLong(1));
        intent.putExtra("event", c.getString(2));
        intent.putExtra("date", c.getString(3));
        intent.putExtra("recur", c.getString(4)); // hmm
        intent.putExtra("mode", "Edit");
        intent.putExtra("end_date", c.getString(5));

       // showToast("Call Edit Activity!");

        startActivityForResult(intent, EDIT_ACTIVITY);
    }

    void todayItem(int position, long id) {

        ContentValues args = new ContentValues();
        SimpleDate now = new SimpleDate(new Date());
        args.put("date", now.getDate(SimpleDate.DateStyle.YMD));

        db.update("event", args, "_id = " + id, null);

        //    Log.wtf("update", "update to " +  now.getDate(SimpleDate.DateStyle.YMD) + "  for id" + id);

        listData();

        if (kind == TabKind.DaysUntil) {
            showToast(getString(R.string.untilToSince));
        }
    }

    private void chooseDayItemDate(int position, long id, SimpleDate d) {
        // showToast("days from is " + daysFrom);

        ContentValues args = new ContentValues();
        args.put("date", d.getDate(SimpleDate.DateStyle.YMD));

        // Log.wtf("future", "update to this: " + d.getDate(SimpleDate.DateStyle.YMD) + " for id " + id);

        db.update("event", args, "_id = " + id, null);
        listData();

        if (kind == TabKind.DaysUntil && d.getDate().before(now.getDate())) {
            showToast(getString(R.string.untilToSince));
        }
    }

    private void chooseDayItem(int position, long id, int daysFrom) {
        // showToast("days from is " + daysFrom);
        Calendar now = Calendar.getInstance();

        // negative daysFrom means future (as in To Happen Tomorrow)
        now.add(Calendar.DAY_OF_YEAR, -daysFrom);

        SimpleDate yesterday = new SimpleDate(now.getTime());

        ContentValues args = new ContentValues();
        args.put("date", yesterday.getDate(SimpleDate.DateStyle.YMD));

        db.update("event", args, "_id = " + id, null);
        listData();

        if (kind == TabKind.DaysUntil) {
            showToast(getString(R.string.untilToSince));
        }

        if (kind == TabKind.DaysSince && daysFrom == -1)
        {
            // tomorrow
            showToast(getString(R.string.sincetoUntil));
        }
    }

    void yesterdayItem(int position, long id) {
        chooseDayItem(position, id, 1);
    }

    void tomorrowItem(int position, long id) {
        chooseDayItem(position, id, -1);
    }

    static final private int MENU_YESTERDAY = Menu.FIRST;
    static final private int MENU_TODAY = Menu.FIRST + 1;
    static final private int MENU_TOMORROW = Menu.FIRST + 2;
    static final private int MENU_EARLIER= Menu.FIRST + 3;


    private static final int GROUP1 = 10;
    private static final int SUBMENU0 = GROUP1 + 1;
    private static final int SUBMENU1 = GROUP1 + 2;
    private static final int SUBMENU2 = GROUP1 + 3;
    private static final int SUBMENU3 = GROUP1 + 4;
    private static final int SUBMENU4 = GROUP1 + 5;
    private static final int SUBMENU5 = GROUP1 + 6;
    private static final int SUBMENU6 = GROUP1 + 7;

    static final private int MENU_REMOVE = Menu.FIRST + 4;
    static final private int MENU_SKIP = Menu.FIRST + 5;
    static final private int MENU_HISTORY = Menu.FIRST + 6;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        //   showToast("right click menu, kind is " + kind);

     //   menu.setHeaderTitle(R.string.event);
        menu.add(0, MENU_YESTERDAY, Menu.NONE + 1, R.string.yesterday);
        menu.add(1, MENU_TODAY, Menu.NONE + 2, R.string.today);
        menu.add(2, MENU_TOMORROW, Menu.NONE + 3, R.string.tomorrow);

        SubMenu weekDayMenu = menu.addSubMenu(GROUP1, MENU_EARLIER,  Menu.NONE + 4, R.string.last_week);
        weekDayMenu.add(GROUP1, SUBMENU0, 1, R.string.sunday);
        weekDayMenu.add(GROUP1, SUBMENU1, 2, R.string.monday);
        weekDayMenu.add(GROUP1, SUBMENU2, 3, R.string.tuesday);
        weekDayMenu.add(GROUP1, SUBMENU3, 4, R.string.wednesday);
        weekDayMenu.add(GROUP1, SUBMENU4, 5, R.string.thursday);
        weekDayMenu.add(GROUP1, SUBMENU5, 6, R.string.friday);
        weekDayMenu.add(GROUP1, SUBMENU6, 7, R.string.saturday);

        menu.add(4, MENU_REMOVE, Menu.NONE + 5, R.string.remove);
        menu.add(5, MENU_SKIP, Menu.NONE + 6, R.string.skip);
        menu.add(6, MENU_HISTORY, Menu.NONE + 7, R.string.history);

    }

    private long parentMenuId = 0;
    private int parentMenuPos = 0;

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        // this is the remedy to have only one fragment call this at a time.
        if (getUserVisibleHint()) {

            super.onContextItemSelected(item);
            AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysDiff = 0;
            long eventId;

            if (menuInfo != null) {
                parentMenuId = menuInfo.id;
                parentMenuPos = menuInfo.position;
                eventId = menuInfo.id;
            }
            else
            {
                eventId = parentMenuId;
            }

            switch (item.getItemId()) {

                case MENU_YESTERDAY:
                    logHappened(eventId, dateLastHappened(eventId));
                    yesterdayItem(menuInfo.position, menuInfo.id);
                    break;
                case MENU_TODAY:
                    logHappened(eventId, dateLastHappened(eventId));
                    todayItem(menuInfo.position, menuInfo.id);
                    break;
                case MENU_TOMORROW:
                    logHappened(eventId, dateLastHappened(eventId));
                    tomorrowItem(menuInfo.position, menuInfo.id);
                    break;
                case MENU_REMOVE:
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.remove_event);
                    builder.setMessage(R.string.are_you_sure);
                    builder.setPositiveButton(R.string.yes, yesNoDialogClickListener);
                    builder.setNegativeButton(R.string.no, yesNoDialogClickListener);
                    builder.show();

                    removeId = menuInfo.id;
                    break;
                // case Menu.NONE + 3:
                //    logHappened(eventId, dateLastHappened(eventId));
                //    break;
                case SUBMENU0:

                    switch (dayOfWeek) // if today is
                    {
                        case Calendar.SATURDAY:
                            daysDiff = 6;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 5;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 4;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 3;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 2;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 1;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 7;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);

                    break;
                case SUBMENU1: // Monday
                    switch (dayOfWeek) {
                        case Calendar.SATURDAY:
                            daysDiff = 5;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 4;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 3;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 2;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 1;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 7;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 6;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);

                    break;
                case SUBMENU2: // last Tuesday
                    switch (dayOfWeek) {
                        case Calendar.SATURDAY:
                            daysDiff = 4;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 3;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 2;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 1;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 7;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 6;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 5;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);

                    break;
                case SUBMENU3: // Wednesday
                    switch (dayOfWeek) {
                        case Calendar.SATURDAY:
                            daysDiff = 3;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 2;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 1;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 7;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 6;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 5;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 4;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);
                    break;
                case SUBMENU4: // Thursday
                    switch (dayOfWeek) {
                        case Calendar.SATURDAY:
                            daysDiff = 2;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 1;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 7;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 6;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 5;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 4;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 3;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);

                    break;
                case SUBMENU5:
                    switch (dayOfWeek) {
                        case Calendar.SATURDAY:
                            daysDiff = 1;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 7;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 6;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 5;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 4;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 3;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 2;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);

                    break;
                case SUBMENU6:
                    switch (dayOfWeek) {
                        case Calendar.SATURDAY:
                            daysDiff = 7;
                            break;
                        case Calendar.FRIDAY:
                            daysDiff = 6;
                            break;
                        case Calendar.THURSDAY:
                            daysDiff = 5;
                            break;
                        case Calendar.WEDNESDAY:
                            daysDiff = 4;
                            break;
                        case Calendar.TUESDAY:
                            daysDiff = 3;
                            break;
                        case Calendar.MONDAY:
                            daysDiff = 2;
                            break;
                        case Calendar.SUNDAY:
                            daysDiff = 1;
                            break;
                    }
                    chooseDayItem(parentMenuPos, parentMenuId, daysDiff);

                    break;
                case MENU_SKIP:

                    // find how many days until next event should happen.

                    String sql = "select date, recur, date(date, '+' || recur || ' day') as nextdate from event where _id = " + menuInfo.id;

                    Cursor cursor = db.rawQuery(sql, null);

                    if (cursor.moveToFirst())
                    {
                        String nextdate = cursor.getString(2);
                        showToast("skip until next date " + nextdate);
                        chooseDayItemDate(menuInfo.position, menuInfo.id, new SimpleDate(nextdate));
                    }
                    break;

                case MENU_HISTORY:

                    // if not there already, log happened, so at least there
                    // will be one entry.
                    logHappened(eventId, dateLastHappened(eventId));
                    Intent intent = new Intent(context, HistoryActivity.class);

                    long catId = getCatIdFromEvent(menuInfo.id);

                    intent.putExtra("eventId", menuInfo.id);
                    intent.putExtra("catId", catId);
                    startActivityForResult(intent, HISTORY_ACTIVITY);
                    break;
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    DialogInterface.OnClickListener yesNoDialogClickListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    db.delete("event", "_id=" + removeId, null);

                    listData();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        }
    };

    long getCatIdFromEvent(long eventId) {
        Cursor cursor = db.query("event", new String[] { "_id", "catId" },
                "_id = " + eventId, null, null, null, null);

        cursor.moveToFirst();
        int catId = cursor.getInt(1);
        cursor.close();
        return catId;
    }

    private String dateLastHappened(long eventId) {
        Cursor cursor = db.query("event", new String[] { "_id", "date" },
                "_id = " + eventId, null, null, null, null);

        cursor.moveToFirst();

        return cursor.getString(1);
    }

    /**
     *
     * @param eventId
     *            - event ID
     * @param dateText
     *            - the date the event happened
     *
     *            The constraint will block the db insert if date is already
     *            there for the id.
     */
    void logHappened(long eventId, String dateText) {
        // determine is onTime or not
        int nEstDays = 0;
        int timeVal = 1;
        DaysSinceCalculations dsc1 = new DaysSinceCalculations(
                dateLastHappened(eventId));

        // get nrecur and see if it's on time.
        // db.query(distinct, table, columns, selection, selectionArgs, groupBy,
        // having, orderBy, limit, cancellationSignal)

        Cursor cursor = db.query("event", new String[] { "_id", "recur" },
                "_id = " + eventId, null, null, null, null);

        cursor.moveToFirst();

        nEstDays = cursor.getInt(1);

        if (nEstDays == 0) {
            timeVal = 1;
            //showToast("on time");
        } else {

            if (dsc1.getDaysSinceEvent() > nEstDays) {
                // not on time
                timeVal = 0;

                //	showToast("not on time");
            } else {
                // showToast("look sir " + dsc1.getDaysSinceEvent());

            }
        }

        ContentValues values = new ContentValues();

        long catId = getCatIdFromEvent(eventId);

        values.put("eventId", eventId);
        values.put("catId", catId);
        values.put("date", dateText);
        values.put("onTime", timeVal);
        // no note here

        try {
            if (db.insert("history", "history", values) == -1) {
                // unique constrain
            }
        }
        catch (SQLiteConstraintException sqlce)
        {
            // ignore problem here
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int notifyHour = 0;
        int notifyMinute = 0;

        switch (requestCode) {

            case EDIT_ACTIVITY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        long id = data.getLongExtra("id", 1);

                        String event = data.getStringExtra("event");
                        String date = data.getStringExtra("date");
                        long nRecur = data.getLongExtra("nRecur", 0);
                        long catId = data.getLongExtra("catId", 0);

                        notifyHour = data.getIntExtra("notifyHour", 0);
                        notifyMinute = data.getIntExtra("notifyMinute", 0);

                        String endDate = data.getStringExtra("end_date");

                        // update db
                        ContentValues args = new ContentValues();
                        args.put("event", event);
                        args.put("date", date);
                        args.put("recur", nRecur);
                        args.put("catId", catId);
                        args.put("end_date", endDate);

                        db.update("event", args, "_id = " + id, null);

                        boolean isFuture;

                        isFuture = data.getBooleanExtra("future", false);

                        if (kind == TabKind.DaysSince) {
                            if (isFuture) {
                                showToast(getString(R.string.sincetoUntil));
                            }
                        } else {
                            if (!isFuture && nRecur == 0) {
                                showToast(getString(R.string.untilToSince));
                            }
                        }

                        // showToast("edit now go set alarm for " + notifyHour + " " +
                        // notifyMinute);

                        alarmHelp.setAlarm(id, notifyHour, notifyMinute);

                        listData();

                        break;
                    case Activity.RESULT_CANCELED:

                        break;
                }
                break;
        }
    }

}