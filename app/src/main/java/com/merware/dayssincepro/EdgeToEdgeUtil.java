package com.merware.dayssincepro;

import android.view.View;
import android.view.Window;

import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Enables edge-to-edge with compatible system-bar styling and keeps app content clear
 * of the system bars.
 */
class EdgeToEdgeUtil {

    static void applyContentInsets(android.app.Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        int surfaceColor = activity.getColor(
                ThemeMode.isDark(activity) ? R.color.ui_surface_dark : R.color.ui_surface_light);
        boolean lightSystemBarBackground = ColorUtils.calculateLuminance(surfaceColor) > 0.5;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightSystemBarBackground);
        controller.setAppearanceLightNavigationBars(lightSystemBarBackground);

        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
