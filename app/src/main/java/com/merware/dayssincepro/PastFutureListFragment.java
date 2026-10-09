package com.merware.dayssincepro;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Date;

import android.database.sqlite.SQLiteDatabase;

/**
 * root class of DaysSinceFragment and DaysUntilFragment
 * Created by Alex on 12/12/2014.
 */

public class PastFutureListFragment extends ListFragment {

    private static final String PREF_CATEGORY_IDS = "CategoryIds";
    private static final String PREF_CATEGORIES_LABEL = "Categories";

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
        AlertDialog.Builder builder = DialogThemeHelper.themedBuilder(context);
        builder.setTitle(R.string.generic_error_title);
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

        db = DatabaseHelper.getInstance(context).getWritableDatabase();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String text = preferences.getString(PREF_CATEGORIES_LABEL, "");

        if (text == "") {
            text = getString(R.string.uncategorized);
        }

        getActivity().setTitle(text);
        DeveloperToolsSession.logTrackA("PastFutureListFragment",
            "onActivityCreated kind=" + kind + " title=\"" + text + "\"");

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

        if (!searchText.isEmpty()) {
                DeveloperToolsSession.logTrackA("PastFutureListFragment",
                    "listData route=search kind=" + kind
                            + " fragment=" + debugFragmentId()
                            + " query=\"" + searchText + "\"");
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
            maybeClearStaleOneTimePlannedDates(today);

            Cursor cursor;

                sql = "select _id, catID, event, date, recur, end_date, date(date, '+' || recur || ' day') as nextdate, details, planned_date, notify_lead_days, notify_enabled, "
                    + "(select max(h.date) from history h where h.eventId = event._id and h.date <= '" + today + "') as last_happened_date, "
                    + "(select max(h.date) from history h where h.eventId = event._id and h.onTime = 0 and h.date <= '" + today + "') as last_off_time_date "
                    + "from event ";

            String whereClause = "where ";

       //     Log.wtf("dsp", "listData() Kind is " + kind);

            if (kind == TabKind.DaysSince || kind == TabKind.SinceLast)
                whereClause += "date <= '" + today + "'";
            else {
                whereClause += "(date > '" + today + "'"
                        + " or (recur = 0 and planned_date > '" + today + "')"
                        + " or (recur > 0 and (end_date is null or end_date > '" + today + "')))";
            }

            categories = preferences.getString(PREF_CATEGORY_IDS, "");
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

                DeveloperToolsSession.logTrackA("PastFutureListFragment",
                    "listData begin route=normal kind=" + kind
                        + " fragment=" + debugFragmentId()
                        + " categoryIds=" + categories
                        + " where=\"" + whereClause + "\"");

            //    showToast(sql);
            cursor = db.rawQuery(sql, null);
                String firstRowProbe = firstRowProbe(cursor);
                DeveloperToolsSession.logTrackA("PastFutureListFragment",
                    "listData end route=normal kind=" + kind
                        + " fragment=" + debugFragmentId()
                        + " categoryIds=" + categories
                        + " rows=" + cursor.getCount()
                        + " firstRow=" + firstRowProbe);

            String[] from = new String[]{"event", "date"}; // columns

            int[] to = new int[]{R.id.eventView, R.id.dateView};

            systemDateFormat = DateFormat.GetSystemDateFormat(context);

            // Log.wtf("past future", "system date format " + systemDateFormat);

            MyEventAdapter eventAdapter;
            eventAdapter = new MyEventAdapter(context, R.layout.event_item,
                    cursor, from, to, systemDateFormat, kind);

            setListAdapter(eventAdapter);

        } catch (Exception e) {
            showToast(getString(R.string.past_future_database_problem_with_reason,
                    e.getMessage()));
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
        if (str == null) {
            return;
        }
        str = str.trim();
        searchText = str;

        try {
            orderBy = getOrderBy();
            maybeClearStaleOneTimePlannedDates(HistoryDateRules.todayIsoDate());

            Cursor cursor;

            sql = buildSearchSql(orderBy);

                DeveloperToolsSession.logTrackA("PastFutureListFragment",
                    "listDataAjax begin route=search kind=" + kind
                        + " fragment=" + debugFragmentId()
                        + " query=\"" + str + "\""
                        + " categoryFilterApplied=false");

            //   showToast(sql);

            cursor = db.rawQuery(sql, new String[]{"%" + str + "%"});
                String firstRowProbe = firstRowProbe(cursor);
                DeveloperToolsSession.logTrackA("PastFutureListFragment",
                    "listDataAjax end route=search kind=" + kind
                        + " fragment=" + debugFragmentId()
                        + " query=\"" + str + "\""
                        + " rows=" + cursor.getCount()
                        + " firstRow=" + firstRowProbe);

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

    static String buildSearchSql(String orderBy) {
        return "select _id, catID, event, date, recur, end_date, date(date, '+' || recur || ' day') as nextdate, details, planned_date, notify_lead_days, notify_enabled, "
            + "(select max(h.date) from history h where h.eventId = event._id and h.date <= date('now', 'localtime')) as last_happened_date, "
            + "(select max(h.date) from history h where h.eventId = event._id and h.onTime = 0 and h.date <= date('now', 'localtime')) as last_off_time_date "
            + "from event "
                + "where UPPER(event) like UPPER(?) order by " + orderBy;
    }

    static boolean shouldResetLastNotifiedDate(String existingDate,
                                               long existingRecur,
                                               String updatedDate,
                                               long updatedRecur) {
        if (existingDate == null || updatedDate == null) {
            return false;
        }
        return !existingDate.equals(updatedDate) || existingRecur != updatedRecur;
    }

    private String firstRowProbe(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            return "empty";
        }

        int originalPosition = cursor.getPosition();
        String probe = "empty";
        if (cursor.moveToFirst()) {
            long eventId = cursor.getLong(0);
            long catId = cursor.getLong(1);
            probe = "eventId=" + eventId + ",catId=" + catId;
        }
        cursor.moveToPosition(originalPosition);
        return probe;
    }

    private String debugFragmentId() {
        return getClass().getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(this));
    }

    private void maybeClearStaleOneTimePlannedDates(String todayIso) {
        if (db == null || todayIso == null) {
            return;
        }

        ContentValues args = new ContentValues();
        args.putNull("planned_date");

        int cleared = db.update(
                "event",
                args,
                "recur = 0 and planned_date is not null and planned_date <= ?",
                new String[]{todayIso}
        );

        if (cleared > 0) {
            showToast(getString(R.string.planned_date_cleared_info));
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

    private int tabIndexForKind() {
        if (kind == TabKind.SinceLast) {
            return 1;
        }
        if (kind == TabKind.DaysUntil) {
            return 2;
        }
        return 0;
    }

    private boolean isContextActionTargetFragment() {
        if (!isAdded()) {
            return false;
        }

        Activity host = getActivity();
        if (!(host instanceof MainActivity)) {
            return true;
        }

        MainActivity mainActivity = (MainActivity) host;
        return mainActivity.getCurrentTabIndex() == tabIndexForKind();
    }

    private void requestTabsRefresh() {
        Activity host = getActivity();
        if (host instanceof MainActivity) {
            ((MainActivity) host).refreshAllTabsFromChild();
            return;
        }
        listData();
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
        intent.putExtra("details", c.getString(7));
        int notifyLeadDaysCol = c.getColumnIndex("notify_lead_days");
        int notifyEnabledCol = c.getColumnIndex("notify_enabled");
        if (notifyLeadDaysCol >= 0 && !c.isNull(notifyLeadDaysCol)) {
            intent.putExtra("notify_lead_days", c.getInt(notifyLeadDaysCol));
        } else {
            intent.putExtra("notify_lead_days", -1);
        }
        if (notifyEnabledCol >= 0 && !c.isNull(notifyEnabledCol)) {
            intent.putExtra("notify_enabled", c.getInt(notifyEnabledCol) != 0);
        } else {
            intent.putExtra("notify_enabled", true);
        }

       // showToast("Call Edit Activity!");

        startActivityForResult(intent, EDIT_ACTIVITY);
    }

    static final private int MENU_REMOVE = Menu.FIRST;
    static final private int MENU_HISTORY = Menu.FIRST + 1;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, MENU_REMOVE, Menu.NONE + 1, R.string.remove);
        menu.add(1, MENU_HISTORY, Menu.NONE + 2, R.string.history);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!isContextActionTargetFragment()) {
            return false;
        }

        super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo menuInfo =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null) {
            return false;
        }

        switch (item.getItemId()) {
            case MENU_REMOVE:
                AlertDialog.Builder builder = DialogThemeHelper.themedBuilder(context);
                builder.setTitle(R.string.remove_event);
                builder.setMessage(R.string.are_you_sure);
                builder.setPositiveButton(R.string.yes, yesNoDialogClickListener);
                builder.setNegativeButton(R.string.no, yesNoDialogClickListener);
                builder.show();
                removeId = menuInfo.id;
                break;
            case MENU_HISTORY:
                Intent intent = new Intent(context, HistoryActivity.class);
                long catId = getCatIdFromEvent(menuInfo.id);
                intent.putExtra("eventId", menuInfo.id);
                intent.putExtra("catId", catId);
                startActivityForResult(intent, HISTORY_ACTIVITY);
                break;
            default:
                return false;
        }

        return true;
    }

    DialogInterface.OnClickListener yesNoDialogClickListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    db.beginTransaction();
                    try {
                        db.delete("history", "eventId=?", new String[]{String.valueOf(removeId)});
                        db.delete("event", "_id=?", new String[]{String.valueOf(removeId)});
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }

                    requestTabsRefresh();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int notifyHour = 0;
        int notifyMinute = 0;

        switch (requestCode) {

            case HISTORY_ACTIVITY:
                requestTabsRefresh();
                break;

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
                        String details = data.getStringExtra("details");
                        int notifyLeadDays = data.getIntExtra("notify_lead_days", -1);
                        boolean notifyEnabled = data.getBooleanExtra("notify_enabled", true);

                        // update db
                        ContentValues args = new ContentValues();
                        args.put("event", event);
                        args.put("date", date);
                        args.put("recur", nRecur);
                        args.put("catId", catId);
                        args.put("end_date", endDate);
                        args.put("details", details);
                        args.put("notify_enabled", notifyEnabled ? 1 : 0);
                        if (notifyLeadDays < 0) {
                            args.putNull("notify_lead_days");
                        } else {
                            args.put("notify_lead_days", notifyLeadDays);
                        }

                        Cursor existing = db.query("event", new String[] { "date", "recur" },
                                "_id = ?", new String[] { String.valueOf(id) },
                                null, null, null);
                        if (existing.moveToFirst()) {
                            String existingDate = existing.getString(0);
                            long existingRecur = existing.getLong(1);
                            if (shouldResetLastNotifiedDate(existingDate, existingRecur, date, nRecur)) {
                                args.putNull("last_notified_date");
                            }
                        }
                        existing.close();

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

                        requestTabsRefresh();

                        break;
                    case Activity.RESULT_CANCELED:

                        break;
                }
                break;
        }
    }

}