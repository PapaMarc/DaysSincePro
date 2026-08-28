package com.merware.dayssincepro;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Since targetSdk 35 (Android 15) enforces edge-to-edge display, the window content
 * draws under the system bars by default. This pads the content root by the system
 * bar insets so the ActionBar/list content isn't drawn under the status/nav bars.
 */
class EdgeToEdgeUtil {

    static void applyContentInsets(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
