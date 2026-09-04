package com.merware.dayssincepro;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CREATED_CATEGORY_ID = "extra_created_category_id";

    private EditText categoryInput;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String themeValue = ThemeMode.getThemeValue(this);
        setTheme(ThemeMode.miniAScreenThemeResId(themeValue));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_category);
        EdgeToEdgeUtil.applyContentInsets(this);
        TopBarHelper.setupCenteredBackToolbar(this, R.id.mini_a_toolbar, R.string.add_a_category);

        db = DatabaseHelper.getInstance(this).getWritableDatabase();

        categoryInput = (EditText) findViewById(R.id.createCategoryInput);
        categoryInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        categoryInput.setHint(R.string.enter_category);

        Button okButton = (Button) findViewById(R.id.createCategoryOk);
        Button cancelButton = (Button) findViewById(R.id.createCategoryCancel);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCategory();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        categoryInput.requestFocus();
        categoryInput.post(new Runnable() {
            @Override
            public void run() {
                if (getWindow() != null) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(categoryInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void submitCategory() {
        String newCategory = categoryInput.getText().toString().trim();
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

        long createdId = db.insert("category", "category", values);
        if (createdId <= 0) {
            showToast(getString(R.string.category_create_failed));
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_CREATED_CATEGORY_ID, createdId);
        setResult(RESULT_OK, result);
        finish();
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

        Cursor c = db.rawQuery(sql.toString(), args);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
