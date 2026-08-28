package com.merware.dayssincepro;


/* http://stackoverflow.com/questions/11815831/saving-listview-simple-list-item-multiple-choice-checkbox-state-using-an-array-a */

// based on grocery app 10/13/2013

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.content.Context;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.util.Log;

public class CategoriesActivity extends ListActivity {

    SimpleCursorAdapter categoryAdapter;
    protected SQLiteDatabase db;
    private ListView lv;
    Button okButton;
    Button addButton;
    Button doneButton;
    Button all_clearButton; // all or clear
    Boolean isClearButton = true;

    SharedPreferences preferences;

    static final private int MENU_EDIT = Menu.FIRST;
    static final private int MENU_REMOVE = Menu.FIRST + 1;
    static final private int MENU_EXPORT = Menu.FIRST + 2;
    static final private int MENU_IMPORT = Menu.FIRST + 3;

    private static final int EXPLORE_ACTIVITY = 5;

    long removeId;
    long exportId;
    long importId;

    private long[] data = null;

    long newItemId = -1;
    int selectedPosition = -1;
    String selectedCategory = "";

    ArrayList<String> selectedCategories = new ArrayList<String>();
    Cursor cursor = null;

    int checkCount = 0;

    private String categories;

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
        setContentView(R.layout.categories);

        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        registerForContextMenu(lv);

        addButton = (Button) findViewById(R.id.addButton);
        addButton.setOnClickListener(addListener);

        doneButton = (Button) findViewById(R.id.doneButton);
        doneButton.setOnClickListener(doneListener);

        okButton = (Button) findViewById(R.id.buttonOK);
        okButton.setOnClickListener(okListener);

        all_clearButton = (Button) findViewById(R.id.all_clearButton);
        all_clearButton.setOnClickListener(all_clearListener);

        listData();

        // check to see which ones were selected from preference

        String categories = preferences.getString("CategoryIds", "");
        String[] items = categories.replaceAll("\\[", "").replaceAll("\\]", "")
                .split(",");

        data = new long[items.length];
        int i;

        for (i = 0; i < items.length; i++) {
            try {
                data[i] = Long.parseLong(items[i].trim());
            } catch (NumberFormatException nfe) {
            }
        }

        // initial list
        checkCount = 0;

        for (i = 0; i < lv.getCount(); i++) {
            long theRowId = lv.getItemIdAtPosition(i);
            cursor = (Cursor) lv.getItemAtPosition(i);

            if (inData(theRowId)) {
                lv.setItemChecked(i, true);
                selectedCategories.add(cursor.getString(1));
                checkCount++;

            } else {
                lv.setItemChecked(i, false);
            }
        }

        updateTitle();

        // if nothing is selected set button to all
        if (checkCount == 0)
            setAllButton();

        if (checkCount == lv.getCount()) {
            setClearButton();
        }

        // if no categories, remove the button
        if (lv.getCount() == 0) {

            all_clearButton.setVisibility(View.INVISIBLE);
        } else {
            all_clearButton.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onListItemClick(ListView listview, View view, int position,
                                   long id) {

        // isItemChecked() return opposite of what it should do.
        Boolean isChecked = !lv.isItemChecked(position);

        // showToast(position + " isChecked" + isChecked);

        cursor = (Cursor) lv.getItemAtPosition(position);
        selectedCategory = cursor.getString(1); // 0 is _id

        if (isChecked) {
            lv.setItemChecked(position, false);
            checkCount--;
            selectedCategories.remove(selectedCategory);

        } else {
            lv.setItemChecked(position, true);
            checkCount++;
            selectedCategories.add(selectedCategory);
        }

        updateTitle();
    }

    private boolean inData(long target) {

        for (int j = 0; j < data.length; j++) {
            if (data[j] == target)
                return true;
        }

        // brand new item should also check.
        if (newItemId != -1 && target == newItemId) {
            return true;
        }

        return false;
    }

    private void listData() {

        String option = preferences.getString("category_sort_order", "0");
        int iOption = Integer.parseInt(option);
        String orderBy = null;

        switch (iOption) {
            case 0:
                orderBy = null;
                break;
            case 1:
                orderBy = "category ASC";
                break;
            case 2:
                orderBy = "category DESC";
                break;
        }

        // _id is required for SimpleCursorAdapter
        Cursor cursor = db.query("category", new String[]{"_id", "category",
                "type"}, null, null, null, null, orderBy);

        String[] from = new String[]{"category", "type"};
        int[] to = new int[]{android.R.id.text1};

        categoryAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_multiple_choice, cursor,
                from, to);

        startManagingCursor(cursor);
        setListAdapter(categoryAdapter);

    }

    void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private OnClickListener okListener = new OnClickListener() {
        public void onClick(View v) {

            data = lv.getCheckedItemIds();

            if (data.length == 0 && lv.getCount() > 0) {

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        CategoriesActivity.this);
                builder.setTitle(R.string.no_category_chosen);
                builder.setMessage(R.string.uncategorized_only);
                builder.setPositiveButton(R.string.yes, yesNoDialogClickListenerOK);
                builder.setNegativeButton(R.string.no, yesNoDialogClickListenerOK);
                builder.show();
            } else {
                exitDialog();
            }
        }
    };

    private void exitDialog() {

        // save categories id data to preference

        String dataArr = Arrays.toString(data);

        if (data.length == 0) {
            // showToast("none checked");
            dataArr = "[0]";
        }

        // showToast(dataArr);

        Editor ed = preferences.edit();
        ed.putString("CategoryIds", dataArr);

        // showToast("all checked: " + dataArr);

        // get all categories text and save that also to preference

        String joined = TextUtils.join(", ", selectedCategories);

        if (data.length == 0) {
            joined = getString(R.string.uncategorized);
        }

        ed.putString(getString(R.string.categories), joined);
        ed.commit();

        // showToast("joined: " + joined);

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        data = lv.getCheckedItemIds();
    }

    private void reApplyChecked() {
        // re-apply checked

        for (int i = 0; i < lv.getCount(); i++) {
            long theRowId = lv.getItemIdAtPosition(i);

            if (inData(theRowId)) {

                lv.setItemChecked(i, true);
                // showToast("reapply set true for " + i);
            } else {
                lv.setItemChecked(i, false);
                // showToast("reapply set false for " + i);
            }
        }

        if (lv.getCount() >= 2) {
            all_clearButton.setVisibility(View.VISIBLE);
        } else {
            all_clearButton.setVisibility(View.INVISIBLE);
        }

        updateTitle();

    }

    @Override
    protected void onResume() {
        super.onResume();
        reApplyChecked();
    }

    private OnClickListener doneListener = new OnClickListener() {
        public void onClick(View v) {
            setResult(RESULT_CANCELED, null);
            finish();
        }
    };

    private void setAllButton() {
        isClearButton = false;
        all_clearButton.setText(R.string.all);
    }

    private void setClearButton() {
        isClearButton = true;
        all_clearButton.setText(R.string.clear);
    }

    private void updateTitle() {

        checkCount = this.selectedCategories.size();

        if (checkCount == 1)
            this.setTitle(getString(R.string.show) + " " + checkCount + " " + getString(R.string.category));
        else
            this.setTitle(getString(R.string.show) + " " + checkCount + " " + getString(R.string.categories));

    }

    private OnClickListener all_clearListener = new OnClickListener() {
        public void onClick(View v) {

            if (isClearButton) {

                // clear
                selectedCategories.clear();

                for (int i = 0; i < lv.getCount(); i++) {
                    lv.setItemChecked(i, false);
                }

                setAllButton();

            } else {

                for (int i = 0; i < lv.getCount(); i++) {
                    lv.setItemChecked(i, true);

                    cursor = (Cursor) lv.getItemAtPosition(i);
                    selectedCategory = cursor.getString(1); // 0 is _id

                    selectedCategories.add(selectedCategory);
                }

                isClearButton = true;
                all_clearButton.setText(R.string.clear);
            }

            updateTitle();

        }
    };

    private OnClickListener addListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    CategoriesActivity.this);
            builder.setTitle(R.string.add_a_category);

            // Set up the input
            final EditText input = new EditText(CategoriesActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton(R.string.OK,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String newCategory = input.getText().toString();
                            ContentValues values = new ContentValues();
                            values.put("category", newCategory);
                            values.put("type", 0);
                            newItemId = db.insert("category", "category",
                                    values);

                            // maybe should add selected as well
                            selectedCategories.add(newCategory);

                            data = lv.getCheckedItemIds();
                            listData();

                            reApplyChecked();

                        }
                    });
            builder.setNegativeButton(R.string.Cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

            builder.show();
        }

    };

    void editItem(int position, final long id) {
        Cursor c = (Cursor) lv.getItemAtPosition(position);
        final String name = c.getString(1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_category);

        // Set up the input
        final EditText input = new EditText(this);
        builder.setView(input);
        input.setText(name);

        // Set up the buttons
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String newName = input.getText().toString();

                // seek and remove what was edited if was selected
                if (selectedCategories.contains(name)) {
                    selectedCategories.remove(name);
                    updateTitle();
                }

                ContentValues args = new ContentValues();
                args.put("category", newName);
                args.put("type", 0);
                db.update("category", args, "_id =" + id, null);

                listData();
            }
        });
        builder.setNegativeButton(R.string.Cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        builder.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, MENU_EDIT, Menu.NONE + 1, R.string.edit);
        menu.add(1, MENU_REMOVE, Menu.NONE + 2, R.string.remove);
        menu.add(2, MENU_EXPORT, Menu.NONE + 3, R.string.export_category);
        menu.add(3, MENU_IMPORT, Menu.NONE + 4, R.string.import_csv);
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
                builder.setTitle(R.string.remove_category);
                builder.setMessage(R.string.remove_cat_msg);
                builder.setPositiveButton(R.string.yes, yesNoDialogClickListener);
                builder.setNegativeButton(R.string.no, yesNoDialogClickListener);
                builder.show();

                removeId = menuInfo.id;
                selectedPosition = menuInfo.position;
                break;
            case MENU_EXPORT:
                exportId = menuInfo.id;
                selectedPosition = menuInfo.position;
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle(R.string.export_category);
                builder2.setMessage(getString(R.string.permission_are_you_sure));
                builder2.setPositiveButton(R.string.yes, exportCSVListener);
                builder2.setNegativeButton(R.string.no, exportCSVListener);
                builder2.show();
                break;
            case MENU_IMPORT:
                importId = menuInfo.id;
                selectedPosition = menuInfo.position;
                AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
                builder3.setTitle(R.string.import_word);

                cursor = (Cursor) lv.getItemAtPosition(selectedPosition);
                selectedCategory = cursor.getString(1); // 0 is _id
                String msg = getString(R.string.import_csv) +  " to " + selectedCategory + ".\n" + getString(R.string.are_you_sure);

                builder3.setMessage(msg);
                builder3.setPositiveButton(R.string.yes, importCSVListener);
                builder3.setNegativeButton(R.string.no, importCSVListener);
                builder3.show();
                break;
        }

        return true;
    }

    DialogInterface.OnClickListener yesNoDialogClickListenerOK = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    exitDialog();

                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    DialogInterface.OnClickListener yesNoDialogClickListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    db.delete("category", "_id=" + removeId, null);
                    db.delete("event", "catId=" + removeId, null);
                    db.delete("history", "catId=" + removeId, null);

                    cursor = (Cursor) lv.getItemAtPosition(selectedPosition);
                    selectedCategory = cursor.getString(1); // 0 is _id

                    // showToast("delete this: " + selectedCategory);
                    selectedCategories.remove(selectedCategory);

                    data = lv.getCheckedItemIds();
                    listData();
                    reApplyChecked();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        }
    };


    private final int BACKUP_PERMISSION = 1;
    private void beforeBackup()
    {
        // If Nougat ask for permission
        // given Manifest permissions still need to ask
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, BACKUP_PERMISSION);
        }
        else
        {
            Log.wtf("dsp", "Permission already granted");
            doExportCSV();
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
                    doExportCSV();
                }
                else
                {
                    Log.wtf("dsp", "permission denied");
                }
                break;
        }
    }

    DialogInterface.OnClickListener exportCSVListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    {
                        doExportCSV();  // don't even ask
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

    DialogInterface.OnClickListener importCSVListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked

                    try {
                        explore();
                    }
                    catch (Exception exp)
                    {
                        Log.wtf("import", exp.getMessage());
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        }
    };


    private String mydir = "DaysSincePro";

    private void doExportCSV()
    {

        String sql;
        try {
            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();

            File directory;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                directory = getExternalFilesDir(null);
            }
            else {

                // Set the output folder on the SD card
                directory = new File(Environment.getExternalStorageDirectory() + "/" + mydir);

                if (!directory.exists()) {
                    directory.mkdirs();
                }
            }

            cursor = (Cursor) lv.getItemAtPosition(selectedPosition);
            selectedCategory = cursor.getString(1); // 0 is _id

            String csvFileName = directory.getPath() + "/" + selectedCategory + ".csv";

            OutputStream myOutput = new FileOutputStream(csvFileName);
            final PrintStream printStream = new PrintStream(myOutput);


            sql = "select event, date, recur from event where catId = " + exportId;
            cursor = db.rawQuery(sql, null);


            while (cursor.moveToNext()) {

                // should be just one row
                String event = cursor.getString(0);
                String date = cursor.getString(1);
                int recur = cursor.getInt(2);

                printStream.println("'" + event + "','" + date + "'," + recur);

                Log.wtf("dsp", "'" + event + "','" + date + "'," + recur);
            }

            // Close and clear the streams
            printStream.flush();
            printStream.close();

            myOutput.flush();
            myOutput.close();
            showToast(selectedCategory + ".csv saved");

            addFileToMediaStore(csvFileName, getApplicationContext());

        } catch (FileNotFoundException e) {
            showToast("write CSV failed.");

            Log.wtf("export", e.getMessage());

        }
        catch (IOException e)
        {
            showToast("Write CSV failed IO failed ");
            Log.e("IO" + "failed", e.getMessage());
        }
        catch (Exception e)
        {
            showToast("Sorry, export failed. Please check App Storage permissions.");
        }
    }

    void explore() {
        Intent intent = new Intent(this, FileExplore.class);
        startActivityForResult(intent, EXPLORE_ACTIVITY);
    }

    private void doImportCSV(String filename)
    {

        try {
            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();

            File f = new File(filename);

            BufferedReader b = new BufferedReader(new FileReader(f));
            String sql = "";
            String data = "";
            int n = 0;

            int numTokens = 0;

            String event;
            String sDate;
            Date date;
            int nRecur = 0;

            while ((data = b.readLine()) != null) {

                // validate data here later

                String[] tokens = data.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

                numTokens = tokens.length;

                if (numTokens < 2)
                {
                    showToast("invalid input " + data);
                    break;
                }

                // input should be quoted string followed by a quoted date

                tokens[0] = tokens[0].trim();

                if (tokens[0].startsWith("'") && tokens[0].endsWith("'"))
                {
                    event = tokens[0].substring(1, tokens[0].length()-1);
                }
                else
                {
                    event = tokens[0];
                }

                // input date should be stripped of quotes so can be parsed as a string

                tokens[1] = tokens[1].trim();
                if (tokens[1].startsWith("'") && tokens[1].endsWith("'"))
                {
                    sDate = tokens[1].substring(1, tokens[1].length()-1);
                }
                else
                {
                    sDate = tokens[1];
                }

                try {
                    if (numTokens == 3) {

                        tokens[2] = tokens[2].trim();

                        nRecur = Integer.parseInt(tokens[2]);
                    }
                }
                catch (NumberFormatException npe)
                {
                    Log.wtf("import", "invalid recur " + tokens[2]);
                }
                // exported with quotes, people making csv should not need to add quotes.
                // what if other formats?


                try {
                    date = new SimpleDateFormat("yyyy-MM-dd").parse(sDate);
                } catch (ParseException pe) {
                    Log.wtf("import", "invalid date (" + sDate + ")");
                    continue;
                }
               

                sql = "insert into event (catId, event, date, recur) values (";

                sql += importId;
                sql += ",";

                sql += "'";
                sql += event;
                sql += "'";
                sql += ",";
                sql += "'";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sql += sdf.format(date);
                sql += "'";
                sql += ",";
                sql += nRecur;
                sql += ")";
                n++;

                Log.wtf("import", sql);

                try {
                    db.execSQL(sql);
                }
                catch (SQLiteException se)
                {
                    showToast(se.getMessage());
                    Log.wtf("sql", se.getMessage());
                }
            }
            showToast("Imported " + n +  " events.");
        }

        catch (FileNotFoundException fnfe)
        {
            showToast(fnfe.getMessage());
        }
        catch (IOException ioe)
        {
            showToast(ioe.getMessage());
        }
    }


    public static final void addFileToMediaStore(final String path,Context context) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(path);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onDestroy() {
        if (cursor != null)
            cursor.close();
        // db is the shared DatabaseHelper singleton - don't close it here, other
        // screens still hold references to it and rely on it staying open.

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EXPLORE_ACTIVITY:

                switch (resultCode) {
                    case Activity.RESULT_OK:

                        String filename = data.getStringExtra("filename");

                        doImportCSV(filename);

                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }
}

