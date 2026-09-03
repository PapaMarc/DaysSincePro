package com.merware.dayssincepro;

/* http://stackoverflow.com/questions/11815831/saving-listview-simple-list-item-multiple-choice-checkbox-state-using-an-array-a */

// based on grocery app 10/13/2013

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.app.Activity;
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
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.util.Log;

public class CategoriesActivity extends ListActivity {

    public static final String EXTRA_AUTO_OPEN_ADD_CATEGORY = "extra_auto_open_add_category";
    public static final String EXTRA_CREATED_CATEGORY_ID = "extra_created_category_id";
    private static final String PREF_HAS_EXPLICIT_FILTER_SELECTION = "has_explicit_filter_selection";

    SimpleCursorAdapter categoryAdapter;
    protected SQLiteDatabase db;
    private ListView lv;
    Button okButton;
    ImageButton addButton;
    Button doneButton;
    Button all_clearButton; // all or clear
    Boolean isClearButton = true;

    SharedPreferences preferences;

    static final private int MENU_EDIT = Menu.FIRST;
    static final private int MENU_REMOVE = Menu.FIRST + 1;
    static final private int MENU_EXPORT = Menu.FIRST + 2;
    static final private int MENU_IMPORT = Menu.FIRST + 3;

    private static final int REQUEST_EXPORT_CATEGORY_CSV_SAF = 20;
    private static final int REQUEST_IMPORT_CATEGORY_CSV_SAF = 21;

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
    private boolean autoOpenAddRequested = false;
    private boolean autoOpenAddConsumed = false;

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

        autoOpenAddRequested = getIntent().getBooleanExtra(EXTRA_AUTO_OPEN_ADD_CATEGORY, false);

        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showCategoryActionMenu(view, position, id);
                return true;
            }
        });

        addButton = (ImageButton) findViewById(R.id.addButton);
        if (theme == 1) {
            addButton.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        } else {
            addButton.setColorFilter(0xFF333333, PorterDuff.Mode.SRC_IN);
        }
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

    private void showCategoryActionMenu(View anchor, final int position, final long id) {
        PopupMenu popup = new PopupMenu(this, anchor);
        boolean isUncategorizedSynthetic = id == CategorySelectionPolicy.UNCATEGORIZED_CAT_ID;

        if (!isUncategorizedSynthetic) {
            popup.getMenu().add(0, MENU_EDIT, Menu.NONE + 1, R.string.edit);
            popup.getMenu().add(1, MENU_REMOVE, Menu.NONE + 2, R.string.remove);
        }
        popup.getMenu().add(2, MENU_EXPORT, Menu.NONE + 3, R.string.export_category);
        popup.getMenu().add(3, MENU_IMPORT, Menu.NONE + 4, R.string.import_csv);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onCategoryMenuAction(item.getItemId(), position, id);
            }
        });
        popup.show();
    }

    private boolean onCategoryMenuAction(int menuItemId, int position, long id) {
        switch (menuItemId) {
            case MENU_EDIT:
                editItem(position, id);
                return true;

            case MENU_REMOVE:
                if (id == CategorySelectionPolicy.UNCATEGORIZED_CAT_ID) {
                    showToast(getString(R.string.category_name_reserved));
                    return true;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.remove_category);
                builder.setMessage(R.string.remove_cat_msg);
                builder.setPositiveButton(R.string.yes, yesNoDialogClickListener);
                builder.setNegativeButton(R.string.no, yesNoDialogClickListener);
                builder.show();

                removeId = id;
                selectedPosition = position;
                return true;

            case MENU_EXPORT:
                exportId = id;
                selectedPosition = position;
                launchExportCategoryCsvPicker();
                return true;

            case MENU_IMPORT:
                importId = id;
                selectedPosition = position;
                launchImportCategoryCsvPicker();
                return true;

            default:
                return false;
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

    private long getUncategorizedEventCount() {
        Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM event WHERE catId = ?",
                new String[]{String.valueOf(CategorySelectionPolicy.UNCATEGORIZED_CAT_ID)});
        try {
            if (countCursor.moveToFirst()) {
                return countCursor.getLong(0);
            }
            return 0;
        } finally {
            countCursor.close();
        }
    }

    private void syncSelectedCategoriesFromChecked() {
        selectedCategories.clear();
        for (int i = 0; i < lv.getCount(); i++) {
            if (lv.isItemChecked(i)) {
                Cursor c = (Cursor) lv.getItemAtPosition(i);
                selectedCategories.add(c.getString(1));
            }
        }
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
        Cursor categoryCursor = db.query("category", new String[]{"_id", "category",
            "type"}, null, null, null, null, orderBy);

        Cursor cursor = categoryCursor;
        if (CategorySelectionPolicy.shouldIncludeSyntheticUncategorized(getUncategorizedEventCount())) {
            MatrixCursor synthetic = new MatrixCursor(new String[]{"_id", "category", "type"});
            synthetic.addRow(new Object[]{
                CategorySelectionPolicy.UNCATEGORIZED_CAT_ID,
                CategorySelectionPolicy.getUncategorizedDisplayLabel(),
                0
            });
            cursor = new MergeCursor(new Cursor[]{synthetic, categoryCursor});
        }

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
            exitDialog();
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
        ed.putBoolean(PREF_HAS_EXPLICIT_FILTER_SELECTION, true);
        ed.commit();

        // showToast("joined: " + joined);

        Intent intent = new Intent();
        if (newItemId > 0) {
            intent.putExtra(EXTRA_CREATED_CATEGORY_ID, newItemId);
        }
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

        if (autoOpenAddRequested && !autoOpenAddConsumed) {
            autoOpenAddConsumed = true;
            showAddCategoryDialog();
        }
    }

    private OnClickListener doneListener = new OnClickListener() {
        public void onClick(View v) {
            if (newItemId > 0) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_CREATED_CATEGORY_ID, newItemId);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED, null);
            }
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
        syncSelectedCategoriesFromChecked();
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

                selectedCategories.clear();
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
            showAddCategoryDialog();
        }

    };

    private void showAddCategoryDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(
                CategoriesActivity.this);
        builder.setTitle(R.string.add_a_category);

        // Set up the input
        final EditText input = new EditText(CategoriesActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(R.string.enter_category);

        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(R.string.OK,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newCategory = input.getText().toString().trim();
                        if (newCategory.length() == 0) {
                            showToast(getString(R.string.category_name_required));
                            return;
                        }

                        if (CategorySelectionPolicy.isReservedCategoryName(newCategory)) {
                            showToast(getString(R.string.category_name_reserved));
                            return;
                        }

                        if (categoryExistsByName(newCategory, -1L)) {
                            showToast(getString(R.string.category_name_exists));
                            return;
                        }

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

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
        input.requestFocus();
    }

    void editItem(int position, final long id) {
        if (id == CategorySelectionPolicy.UNCATEGORIZED_CAT_ID) {
            showToast(getString(R.string.category_name_reserved));
            return;
        }

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

                String newName = input.getText().toString().trim();
                if (newName.length() == 0) {
                    showToast(getString(R.string.category_name_required));
                    return;
                }

                if (CategorySelectionPolicy.isReservedCategoryName(newName)) {
                    showToast(getString(R.string.category_name_reserved));
                    return;
                }

                if (!CategorySelectionPolicy.areCategoryNamesEquivalent(newName, name)
                        && categoryExistsByName(newName, id)) {
                    showToast(getString(R.string.category_name_exists));
                    return;
                }

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

    private boolean categoryExistsByName(String categoryName, long excludeCategoryId) {
        String normalized = CategorySelectionPolicy.normalizeCategoryNameForLookup(categoryName);
        StringBuilder sql = new StringBuilder(
                "SELECT _id FROM category WHERE LOWER(TRIM(category)) = ?");

        String[] args;
        if (excludeCategoryId > 0) {
            sql.append(" AND _id <> ?");
            args = new String[]{normalized, String.valueOf(excludeCategoryId)};
        } else {
            args = new String[]{normalized};
        }

        Cursor matchCursor = db.rawQuery(sql.toString(), args);
        try {
            return matchCursor.moveToFirst();
        } finally {
            matchCursor.close();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        boolean isUncategorizedSynthetic = info != null
                && info.id == CategorySelectionPolicy.UNCATEGORIZED_CAT_ID;

        if (!isUncategorizedSynthetic) {
            menu.add(0, MENU_EDIT, Menu.NONE + 1, R.string.edit);
            menu.add(1, MENU_REMOVE, Menu.NONE + 2, R.string.remove);
        }
        menu.add(2, MENU_EXPORT, Menu.NONE + 3, R.string.export_category);
        menu.add(3, MENU_IMPORT, Menu.NONE + 4, R.string.import_csv);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        AdapterView.AdapterContextMenuInfo menuInfo;
        menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (menuInfo == null) {
            return false;
        }

        return onCategoryMenuAction(item.getItemId(), menuInfo.position, menuInfo.id);
    }

    private void launchExportCategoryCsvPicker() {
        cursor = (Cursor) lv.getItemAtPosition(selectedPosition);
        selectedCategory = cursor.getString(1); // 0 is _id
        String filename = CsvExporter.sanitizeFilename(selectedCategory) + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        CsvExporter.setDownloadsInitialUri(intent);
        startActivityForResult(intent, REQUEST_EXPORT_CATEGORY_CSV_SAF);
    }

    private void launchImportCategoryCsvPicker() {
        cursor = (Cursor) lv.getItemAtPosition(selectedPosition);
        selectedCategory = cursor.getString(1); // 0 is _id

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "text/plain"
        });
        CsvExporter.setDownloadsInitialUri(intent);
        startActivityForResult(intent, REQUEST_IMPORT_CATEGORY_CSV_SAF);
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

    private void handleExportCategoryCsvSaf(Uri uri) {
        if (uri == null) return;
        try {
            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                showToast("Failed to open output stream");
                return;
            }
            CsvExportResult result = CsvExporter.exportCategory(db, exportId, os);
            if (result.isSuccess()) {
                showToast(selectedCategory + ".csv saved (" + result.getRowsExported() + " events)");
            } else {
                showToast("Export failed: " + result.getErrorMessage());
                Log.e("DSP_EXPORT", "Export failed: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            Log.e("DSP_EXPORT", "Export error", e);
            showToast("Export failed: " + e.getMessage());
        }
    }

    private void handleImportCategoryCsvSaf(Uri uri) {
        if (uri == null) return;
        try {
            SQLiteDatabase db = DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            CsvImportResult result = CsvImporter.importMultipleCsvUris(this, db,
                    Collections.singletonList(uri), importId);

            if (result.isSuccess()) {
                showToast(result.getSummaryMessage());
                if (result.getCategoriesCreated() > 0 || result.getImportedCount() > 0) {
                    listData();
                    reApplyChecked();
                }
            } else {
                String firstErr = result.getErrors().isEmpty() ? "No events imported." : result.getErrors().get(0);
                showToast("Import error: " + firstErr);
                Log.e("DSP_IMPORT", "Import failed: " + firstErr);
            }
        } catch (Exception e) {
            Log.e("DSP_IMPORT", "Import error", e);
            showToast("Import failed: " + e.getMessage());
        }
    }

    public static final void addFileToMediaStore(final String path, Context context) {
        CsvExporter.addFileToMediaStore(context, path);
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

        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        switch (requestCode) {
            case REQUEST_EXPORT_CATEGORY_CSV_SAF:
                handleExportCategoryCsvSaf(data.getData());
                break;
            case REQUEST_IMPORT_CATEGORY_CSV_SAF:
                handleImportCategoryCsvSaf(data.getData());
                break;
        }
    }
}

