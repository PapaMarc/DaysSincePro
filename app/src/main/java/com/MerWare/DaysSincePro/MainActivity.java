package com.MerWare.DaysSincePro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Arrays;

import android.util.Log;

public class MainActivity extends AppCompatActivity implements
        ActionBar.TabListener,  SearchView.OnQueryTextListener {

    SharedPreferences preferences;
    SectionsPagerAdapter mSectionsPagerAdapter;

    ViewPager mViewPager;

    boolean notifyOptionBefore = false;

    AlarmHelper alarmHelp;

    File InternalStorageDirectory;
    String Days_Since_DB = ""; // full path of daysSince.db
    String Days_Since_DB_Internal = "";


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

        if (iTab == 1)
            mViewPager.setCurrentItem(1);

        // for backup and restore
        SetDBName();
    }

    MenuItem searchMenuItem;
    SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

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

        } else if (itemId == R.id.menu_export) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.backup_database);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setMessage(R.string.no_permission_are_you_sure);
            }
            else {
                builder.setMessage(R.string.permission_are_you_sure);
            }


            builder.setPositiveButton(R.string.yes, backupListener);
            builder.setNegativeButton(R.string.no, backupListener);
            builder.show();

        } else if (itemId == R.id.menu_import) {

            // if folder is empty another message
            File[] files = InternalStorageDirectory.listFiles();
            if (files.length == 0)
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.restore_from_database)
                        .setMessage(Days_Since_DB_Internal + " is not found.")
                        .setCancelable(false)
                        .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
            else {

                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle(R.string.restore_from_database);
                builder2.setMessage(getString(R.string.replace_data) + " "
                        + getString(R.string.are_you_sure));
                builder2.setPositiveButton(R.string.yes, restoreListener);
                builder2.setNegativeButton(R.string.no, restoreListener);
                builder2.show();
            }

        } else if (itemId == R.id.menu_notify) {
            alarmHelp.setAlarm(1);

        } else if (itemId == R.id.action_search) {
            // no-op
        }

        return super.onOptionsItemSelected(item);
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
    DaysUntilFragment daysUntilFragment;

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
                    daysUntilFragment = new DaysUntilFragment();
                    fragment = daysUntilFragment;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.dayssince);
                case 1:
                    return getString(R.string.daysuntil);
            }
            return null;
        }
    }


    DialogInterface.OnClickListener backupListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked

                    Log.wtf("DSP", "Andorid SDK version is " + Build.VERSION.SDK_INT);
                    Log.wtf("DSP", "Andorid SDK R is " + Build.VERSION_CODES.R);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    {
                        doExportDB();  // don't even ask
                    }
                    else
                    {
                        // old way that still worked with API 30
                        beforeBackup(); // its call back will call doExportDB()
                    }

                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        }
    };

    private void ToastAndLog(String description, String msg)
    {
        showToast(description + ": " + msg);
        Log.wtf("dsp", description + ": " +  msg);
    }

    private void ToastAndLog(String msg)
    {
        showToast(msg);
        Log.wtf("dsp", msg);
    }

    private String mydir = "DaysSincePro";
    private String dbName = "/data/data/com.MerWare.DaysSincePro/databases/alex_db";
    private final int BACKUP_PERMISSION = 1;


    private void beforeBackup()
    {
       // If Noguat ask for permission
        // given Manifest permissions still need to ask
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, BACKUP_PERMISSION);
        }
        else
        {
            Log.wtf("dsp", "Permission already granted");
            doExportDB();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode)
        {
            case BACKUP_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    doExportDB();
                }
                else
                {
                    Log.wtf("dsp", "permission denied");
                }
                break;
            default:
                Log.wtf("dsp", "request code is " + requestCode);
                break;
        }
    }

    void doExportDB() {

        InputStream myInput;

        try {

            myInput = new FileInputStream(dbName);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            {
                if (!InternalStorageDirectory.exists()) {
                    InternalStorageDirectory.mkdirs();
                }
            }

            // Set the output file stream up:

            OutputStream myOutput = new FileOutputStream(InternalStorageDirectory.getPath() + "/daysSince.db");

            // Close db before trying to export
            SQLiteDatabase db;
            db = (new DatabaseHelper(getApplicationContext())).getWritableDatabase();
            db.close();

            // Transfer bytes from the input file to the output file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            // Close and clear the streams

            myOutput.flush();
            myOutput.close();
            myInput.close();

          //  showToast(getString(R.string.backup_success));


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.backup_database_success);
            builder.setCancelable(true);

            builder.setMessage("Database exported to " + Days_Since_DB_Internal);
            builder.setNeutralButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            builder.show();


        } catch (FileNotFoundException e) {
            showToast(getString(R.string.backup_fail));
            //	Log.e("file not found", "failed", e);

            showToast(e.getMessage());
            Log.wtf("FNF", e.getMessage());

        } catch (IOException e) {
            showToast(getString(R.string.backup_fail));
            showToast(e.getMessage());
            Log.wtf("IOE", e.getMessage());
        }

    }


    DialogInterface.OnClickListener restoreListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    explore();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        }
    };


    void explore() {
        Intent intent = new Intent(this, FileExplore.class);
        startActivityForResult(intent, EXPLORE_ACTIVITY);
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

        switch (requestCode) {

            case ADD_ACTIVITY:
                switch (resultCode) {

                    case Activity.RESULT_OK:
                        boolean isFuture;
                        long nRecur;
                        int notifyHour = 0;
                        int notifyMinute = 0;

                        notifyHour = data.getIntExtra("notifyHour", 0);
                        notifyMinute = data.getIntExtra("notifyMinute", 0);

                        long id = data.getLongExtra("id", 0);

                        isFuture = data.getBooleanExtra("future", false);
                        nRecur = data.getLongExtra("nRecur", 0);

                        int kind = mViewPager.getCurrentItem();
                        //showToast("Look I am on tab: " + mViewPager.getCurrentItem());

                        if (kind == 0) {
                            if (isFuture) {
                                showToast(getString(R.string.msg_until));
                            }
                        } else {
                            if (!isFuture && nRecur == 0)
                                showToast(getString(R.string.msg_since));
                        }

                        daysSinceFragment.listData();
                        daysUntilFragment.listData();

                        chosenID = data.getLongExtra("catId", 0);

                        if (!inData(chosenID)) {
                            showToast(getString(R.string.not_chosen));
                        }

                        // save to reference

                        if (notifyHour == 0 && notifyMinute == 0) {
                            // don't bother to send a new alarm, use what's there.
                        } else {
                            showToast("ADD new now go set alarm for id " + id + " at "
                                    + notifyHour + " " + notifyMinute);
                            alarmHelp.setAlarm(id, notifyHour, notifyMinute);
                        }

                        break;
                    case Activity.RESULT_CANCELED:
                        break;
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

            case EXPLORE_ACTIVITY:

                switch (resultCode) {
                    case Activity.RESULT_OK:

                        String filename = data.getStringExtra("filename");

                        doImportDB(filename);

                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
            case CATEGORY_ACTIVITY:

                switch (resultCode) {
                    case Activity.RESULT_OK:

                        // reset title
                        String text = preferences.getString("Categories", "");

                        if (text == "") {
                            text = getString(R.string.uncategorized);
                        }

                        setTitle(text);
                        // must do it here
                        if (daysSinceFragment != null)
                            daysSinceFragment.listData();

                        if (daysUntilFragment != null)
                            daysUntilFragment.listData();

                        break;
                    case Activity.RESULT_CANCELED:

                        break;
                }
        }

    }

    // Read a few bytes make sure it says SQLite
    boolean isSQLiteFile(String filename) throws FileNotFoundException,
            IOException {

        FileInputStream fis = new FileInputStream(filename);

        int numOfBytes = 6; // SQLite
        byte byteArr[] = new byte[numOfBytes];

        fis.read(byteArr);

        String str = new String("SQLite");
        byte byteArr2[] = str.getBytes();

        fis.close();

        if (Arrays.equals(byteArr, byteArr2)) {
            return true;
        }
        return false;
    }


    void doImportDB(String filename) {

        try {

            if (!isSQLiteFile(filename)) {
                showToast("Sorry, invalid database file: " + filename);
                return;
            }

            FileOutputStream myOutput = new FileOutputStream(dbName);

            InputStream myInputs = new FileInputStream(filename);

            // Transfer bytes from the input file to the output file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInputs.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Close and clear the streams
            myOutput.flush();
            myOutput.close();
            myInputs.close();

            showToast(getString(R.string.restore_success));
            setTitle(R.string.all_categories);

            SharedPreferences.Editor ed = preferences.edit();
            ed.putString("CategoryIds", "");
            ed.putString("Categories", "");
            ed.commit();

        } catch (FileNotFoundException e) {
            showToast(getString(R.string.restore_fail));
            //		Log.e("file not found", "failed", e);

        } catch (IOException e) {
            showToast(getString(R.string.restore_fail));

            //		Log.e("IO exception", "failed", e);
        }
    }


    @Override
    public boolean onQueryTextSubmit(String query) {

        daysSinceFragment.listDataAjax(query);
        daysUntilFragment.listDataAjax(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        // from Crashes and ANRs
        if (daysSinceFragment == null || daysUntilFragment == null)
            return false;

        if (newText == null)
            return false;

        if (newText.length() == 0) {
            daysSinceFragment.unsetSearchText();
            daysUntilFragment.unsetSearchText();

            daysSinceFragment.listData();
            daysUntilFragment.listData();
            return true;
        }

        daysSinceFragment.listDataAjax(newText);
        daysUntilFragment.listDataAjax(newText);
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

    private void SetDBName()
    {
        InternalStorageDirectory = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            // new android only have access to this folder
            InternalStorageDirectory = getExternalFilesDir(null);
        }
        else {
            // classic way add a folder
            // Set the output folder on the SDcard
            InternalStorageDirectory = new File(Environment.getExternalStorageDirectory() + "/" + mydir);
        }

        if (InternalStorageDirectory != null) {
            Days_Since_DB = InternalStorageDirectory.getPath() + "/daysSince.db";
            Days_Since_DB_Internal = Days_Since_DB.replace("/storage/emulated/0", "Internal Storage");
        }

    }


}