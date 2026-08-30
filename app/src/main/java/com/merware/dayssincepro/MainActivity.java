package com.merware.dayssincepro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

public class MainActivity extends AppCompatActivity implements
        ActionBar.TabListener,  SearchView.OnQueryTextListener {

    SharedPreferences preferences;
    SectionsPagerAdapter mSectionsPagerAdapter;

    ViewPager mViewPager;

    boolean notifyOptionBefore = false;

    AlarmHelper alarmHelp;

    private static final int REQUEST_EXPORT_DB_SAF = 10;
    private static final int REQUEST_EXPORT_CSV_SAF = 11;
    private static final int REQUEST_RESTORE_DB_SAF = 12;
    private static final int REQUEST_IMPORT_CSV_SAF = 13;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        alarmHelp = new AlarmHelper(this);

        String sTheme = preferences.getString("theme", "0");
        int theme = Integer.parseInt(sTheme);

        if (theme == 1) { // dark
            setTheme(R.style.AppTheme2);
        } else {// 0 light
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdgeUtil.applyContentInsets(this);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager
                .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        actionBar.setSelectedNavigationItem(position);
                    }
                });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }

        boolean notifyOptionJustNow = preferences.getBoolean("noti", false);

        nthCreate++;

        if (nthCreate == 1) // for the first time in forever hack
        {
            if (notifyOptionJustNow) {
                //showToast("on first create");
                alarmHelp.setAlarm(1);
            }
        }

        // set to a tab based on preference
        String tabStyle = preferences.getString("tab_style", "0");
        int iTab = Integer.parseInt(tabStyle);

        if (iTab != 0)
            mViewPager.setCurrentItem(iTab);
    }

    MenuItem searchMenuItem;
    SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Draws a divider between the menu's <group> sections in the overflow popup.
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setGroupDividerEnabled(true);
        }

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.
                getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int itemId = item.getItemId();

        if (itemId == R.id.menu_daysdiff) {

            Intent intentDD = new Intent(this, DaysDiffActivity.class);
            startActivityForResult(intentDD, DAYSDIFF_ACTIVITY);

        } else if (itemId == R.id.menu_about) {
            AboutDialog.create(this, "Alex Mak", "2.x Aug 2015 --> v3.0.2 Sep 30 2016 --> v3.1.5 Nov 23, 2023",
                    "").show();

        } else if (itemId == R.id.action_add) {

            // either tabs have the same functions.
            // daysSinceFragment.addItem();
            addItem();

        } else if (itemId == R.id.action_open) {
            category();

        } else if (itemId == R.id.action_settings) {
            settings();

        } else if (itemId == R.id.menu_export_db) {
            launchExportDbPicker();

        } else if (itemId == R.id.menu_export_csv) {
            launchExportCsvPicker();

        } else if (itemId == R.id.menu_import_db) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.restore_from_database);
            builder.setMessage(getString(R.string.replace_data) + " " + getString(R.string.are_you_sure));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    launchRestoreDbPicker();
                }
            });
            builder.setNegativeButton(R.string.no, null);
            builder.show();

        } else if (itemId == R.id.menu_import_csv) {
            launchImportCsvPicker();

        } else if (itemId == R.id.menu_notify) {
            alarmHelp.setAlarm(1);

        } else if (itemId == R.id.action_search) {
            // no-op
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchExportDbPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.sqlite3");
        intent.putExtra(Intent.EXTRA_TITLE, "daysSince.db");
        CsvExporter.setDownloadsInitialUri(intent);
        startActivityForResult(intent, REQUEST_EXPORT_DB_SAF);
    }

    private void launchExportCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "daysSince.csv");
        CsvExporter.setDownloadsInitialUri(intent);
        startActivityForResult(intent, REQUEST_EXPORT_CSV_SAF);
    }

    private void launchRestoreDbPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.sqlite3",
                "application/x-sqlite3",
                "application/octet-stream"
        });
        CsvExporter.setDownloadsInitialUri(intent);
        startActivityForResult(intent, REQUEST_RESTORE_DB_SAF);
    }

    private void launchImportCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "text/plain"
        });
        CsvExporter.setDownloadsInitialUri(intent);
        startActivityForResult(intent, REQUEST_IMPORT_CSV_SAF);
    }


    long chosenID = 0;
    void category() {
        Intent intent = new Intent(this, CategoriesActivity.class);
        intent.putExtra("catId", chosenID); // pass along last chosen category
        startActivityForResult(intent, CATEGORY_ACTIVITY);
    }

    private long[] data = null;

    private String categories;

    void addItem() {
        Intent intent = new Intent(this, EditEventActivity.class);

        categories = preferences.getString("CategoryIds", "");
        categories = categories.replaceAll("\\[", "").replaceAll("\\]", "");

        String[] items = categories.split(",");

        data = new long[items.length];
        int i;

        for (i = 0; i < items.length; i++) {
            try {
                data[i] = Long.parseLong(items[i].trim());
            } catch (NumberFormatException nfe) {
            }
        }

        if (items.length >= 1)
            chosenID = data[items.length-1];

        //   showToast("chosen cat id " + chosenID + " items " + items.length);

        intent.putExtra("catId", chosenID); // last chosen id
        intent.putExtra("mode", "Add");
        startActivityForResult(intent, ADD_ACTIVITY);
    }

    private static final int CONFIG_ACTIVITY = 4;
    private static final int EXPLORE_ACTIVITY = 5;
    private static final int CATEGORY_ACTIVITY = 6;
    private static final int ADD_ACTIVITY = 7;
    private static final int DAYSDIFF_ACTIVITY = 8;

    static int nthCreate = 0;

    void settings() {
        Intent intent = new Intent(this, PrefActivity.class);

        notifyOptionBefore = preferences.getBoolean("noti", false);

        startActivityForResult(intent, CONFIG_ACTIVITY);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab,
                              FragmentTransaction fragmentTransaction) {

        mViewPager.setCurrentItem(tab.getPosition());

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab,
                                FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab,
                                FragmentTransaction fragmentTransaction) {
    }

    // don't restart when phone change orientation.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void showToast(String s) {
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    DaysSinceFragment daysSinceFragment;
    SinceLastFragment sinceLastFragment;
    DaysUntilFragment daysUntilFragment;

    // package-private + static so it can be unit tested without an Activity/pager instance.
    // Skips any tab fragment not yet instantiated by the pager (i.e. never visited/scrolled to).
    static void refreshTabs(PastFutureListFragment daysSince, PastFutureListFragment sinceLast,
                            PastFutureListFragment daysUntil) {
        if (daysSince != null)
            daysSince.listData();
        if (sinceLast != null)
            sinceLast.listData();
        if (daysUntil != null)
            daysUntil.listData();
    }

    // package-private + static so it can be unit tested without an Activity/pager instance.
    // Skips any tab fragment not yet instantiated by the pager (i.e. never visited/scrolled to).
    static void dispatchSearch(PastFutureListFragment daysSince, PastFutureListFragment sinceLast,
                               PastFutureListFragment daysUntil, String query) {
        if (daysSince != null)
            daysSince.listDataAjax(query);
        if (sinceLast != null)
            sinceLast.listDataAjax(query);
        if (daysUntil != null)
            daysUntil.listDataAjax(query);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.

            Fragment fragment = null;

            switch (position) {
                case 0:
                    daysSinceFragment = new DaysSinceFragment(); // only empty constructor is
                    // allowed otherwise runtime crash on rotate
                    fragment = daysSinceFragment;

                    break;
                case 1:
                    sinceLastFragment = new SinceLastFragment();
                    fragment = sinceLastFragment;
                    break;
                case 2:
                    daysUntilFragment = new DaysUntilFragment();
                    fragment = daysUntilFragment;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.dayssince);
                case 1:
                    return getString(R.string.sincelast);
                case 2:
                    return getString(R.string.daysuntil);
            }
            return null;
        }
    }

    private File getLiveDbFile() {
        return getDatabasePath(DatabaseHelper.DATABASE_NAME);
    }

    private void handleExportDbSaf(Uri uri) {
        if (uri == null) return;
        File tempSnapshot = null;
        try {
            tempSnapshot = File.createTempFile("daysSince_export", ".db", getCacheDir());
            if (tempSnapshot.exists()) {
                tempSnapshot.delete();
            }

            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.execSQL("VACUUM INTO ?", new Object[]{tempSnapshot.getAbsolutePath()});

            try (InputStream in = new FileInputStream(tempSnapshot);
                 OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    showToast(getString(R.string.backup_fail));
                    return;
                }
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.backup_database_success);
            builder.setCancelable(true);
            builder.setMessage(getString(R.string.backup_success));
            builder.setNeutralButton(android.R.string.ok, null);
            builder.show();

        } catch (Exception e) {
            Log.e("DSP_EXPORT_DB", "Failed to export db", e);
            showToast(getString(R.string.backup_fail) + ": " + e.getMessage());
        } finally {
            if (tempSnapshot != null && tempSnapshot.exists()) {
                tempSnapshot.delete();
            }
        }
    }

    private void handleExportCsvSaf(Uri uri) {
        if (uri == null) return;
        try {
            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) {
                showToast("Failed to open output stream");
                return;
            }
            CsvExportResult result = CsvExporter.exportAllCategories(db, out);
            if (result.isSuccess()) {
                showToast("daysSince.csv saved (" + result.getRowsExported() + " events)");
            } else {
                showToast("Export failed: " + result.getErrorMessage());
                Log.e("DSP_EXPORT_CSV", "CSV export failed: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            Log.e("DSP_EXPORT_CSV", "CSV export failed", e);
            showToast("Export failed: " + e.getMessage());
        }
    }

    private void handleRestoreDbSaf(Uri uri) {
        if (uri == null) return;
        try {
            // Verify SQLite header magic string before overwriting database
            try (InputStream testIn = getContentResolver().openInputStream(uri)) {
                if (testIn == null) {
                    showToast(getString(R.string.restore_fail));
                    return;
                }
                byte[] byteArr = new byte[6];
                int read = testIn.read(byteArr);
                if (read < 6 || !Arrays.equals(byteArr, "SQLite".getBytes())) {
                    showToast("Sorry, invalid database file.");
                    return;
                }
            }

            // Close shared database connection before replacing file
            DatabaseHelper.closeInstance();

            File liveDbFile = getLiveDbFile();
            File dbDir = liveDbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs();
            }

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(liveDbFile)) {
                if (in == null) {
                    showToast(getString(R.string.restore_fail));
                    return;
                }
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }

            showToast(getString(R.string.restore_success));
            setTitle(R.string.all_categories);

            SharedPreferences.Editor ed = preferences.edit();
            ed.putString("CategoryIds", "");
            ed.putString("Categories", "");
            ed.commit();

            // Restart Activity to re-bind fresh SQLite helpers and reload fragments
            finish();
            Intent restartIntent = new Intent(this, MainActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(restartIntent);

        } catch (Exception e) {
            Log.e("DSP_RESTORE_DB", "Database restore failed", e);
            showToast(getString(R.string.restore_fail) + ": " + e.getMessage());
        }
    }

    private void handleImportCsvSaf(Intent data) {
        if (data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            ClipData clip = data.getClipData();
            for (int i = 0; i < clip.getItemCount(); i++) {
                ClipData.Item item = clip.getItemAt(i);
                if (item != null && item.getUri() != null) {
                    uris.add(item.getUri());
                }
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }

        if (uris.isEmpty()) return;

        try {
            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            CsvImportResult result = CsvImporter.importMultipleCsvUris(this, db, uris, 0);

            if (result.isSuccess()) {
                showToast(result.getSummaryMessage());
                refreshTabs(daysSinceFragment, sinceLastFragment, daysUntilFragment);
            } else {
                String firstErr = result.getErrors().isEmpty() ? "No events imported." : result.getErrors().get(0);
                showToast("Import error: " + firstErr);
                Log.e("DSP_IMPORT_CSV", "CSV import failed: " + firstErr);
            }
        } catch (Exception e) {
            Log.e("DSP_IMPORT_CSV", "CSV import error", e);
            showToast("Import failed: " + e.getMessage());
        }
    }

    private boolean inData(long target) {

        for (int j = 0; j < data.length; j++) {
            if (data[j] == target)
                return true;
        }

        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_EXPORT_DB_SAF:
                if (data != null) handleExportDbSaf(data.getData());
                break;
            case REQUEST_EXPORT_CSV_SAF:
                if (data != null) handleExportCsvSaf(data.getData());
                break;
            case REQUEST_RESTORE_DB_SAF:
                if (data != null) handleRestoreDbSaf(data.getData());
                break;
            case REQUEST_IMPORT_CSV_SAF:
                if (data != null) handleImportCsvSaf(data);
                break;

            case ADD_ACTIVITY:
                boolean isFuture;
                long nRecur;
                int notifyHour = 0;
                int notifyMinute = 0;

                if (data != null) {
                    notifyHour = data.getIntExtra("notifyHour", 0);
                    notifyMinute = data.getIntExtra("notifyMinute", 0);

                    long id = data.getLongExtra("id", 0);

                    isFuture = data.getBooleanExtra("future", false);
                    nRecur = data.getLongExtra("nRecur", 0);

                    int kind = mViewPager.getCurrentItem();

                    if (kind == 0) {
                        if (isFuture) {
                            showToast(getString(R.string.msg_until));
                        }
                    } else {
                        if (!isFuture && nRecur == 0)
                            showToast(getString(R.string.msg_since));
                    }

                    refreshTabs(daysSinceFragment, sinceLastFragment, daysUntilFragment);

                    chosenID = data.getLongExtra("catId", 0);

                    if (!inData(chosenID)) {
                        showToast(getString(R.string.not_chosen));
                    }

                    if (notifyHour == 0 && notifyMinute == 0) {
                        // don't bother to send a new alarm, use what's there.
                    } else {
                        showToast("ADD new now go set alarm for id " + id + " at "
                                + notifyHour + " " + notifyMinute);
                        alarmHelp.setAlarm(id, notifyHour, notifyMinute);
                    }
                }
                break;
            case CONFIG_ACTIVITY:

                boolean notifyOptionJustNow = preferences.getBoolean("noti", false);

                if (notifyOptionJustNow && !notifyOptionBefore) {
                    alarmHelp.setAlarm(0);
                    notifyOptionBefore = true;
                }

                if (!notifyOptionJustNow) {
                    alarmHelp.cancelAlarm();
                }

                finish();
                // reset to apply theme
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                break;

            case CATEGORY_ACTIVITY:
                // reset title
                String text = preferences.getString("Categories", "");

                if (text == null || text.isEmpty()) {
                    text = getString(R.string.uncategorized);
                }

                setTitle(text);
                refreshTabs(daysSinceFragment, sinceLastFragment, daysUntilFragment);
                break;
        }

    }


    @Override
    public boolean onQueryTextSubmit(String query) {

        dispatchSearch(daysSinceFragment, sinceLastFragment, daysUntilFragment, query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (newText == null)
            return false;

        if (newText.length() == 0) {
            // from Crashes and ANRs
            if (daysSinceFragment != null)
                daysSinceFragment.unsetSearchText();
            if (sinceLastFragment != null)
                sinceLastFragment.unsetSearchText();
            if (daysUntilFragment != null)
                daysUntilFragment.unsetSearchText();

            refreshTabs(daysSinceFragment, sinceLastFragment, daysUntilFragment);
            return true;
        }

        dispatchSearch(daysSinceFragment, sinceLastFragment, daysUntilFragment, newText);
        return true;
    }

    @Override
    public void onBackPressed() {

        if(isTaskRoot()) {

            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.exit))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        }
        else
        {
            super.onBackPressed();
        }
    }

}