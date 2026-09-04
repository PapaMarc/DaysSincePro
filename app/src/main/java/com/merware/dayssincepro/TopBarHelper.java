package com.merware.dayssincepro;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

final class TopBarHelper {

    private TopBarHelper() {
    }

    static void setupCenteredBackToolbar(AppCompatActivity activity,
                                         int toolbarId,
                                         int titleResId) {
        MaterialToolbar toolbar = (MaterialToolbar) activity.findViewById(toolbarId);
        if (titleResId != 0) {
            toolbar.setTitle(titleResId);
        }

        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> activity.finish());
    }
}
