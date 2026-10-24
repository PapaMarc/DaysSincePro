package com.merware.dayssincepro;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Since targetSdk 35 (Android 15) enforces edge-to-edge display, the window content
 * draws under the system bars by default. This pads the content root by the system
 * bar insets so the ActionBar/list content isn't drawn under the status/nav bars.
 */
class EdgeToEdgeUtil {

    static void applyContentInsets(Activity activity) {
        applyStatusBarStyle(activity);

        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private static void applyStatusBarStyle(Activity activity) {
        Window window = activity.getWindow();
        boolean darkTheme = ThemeMode.isDark(activity);
        int statusBarColor = activity.getColor(
                darkTheme ? R.color.ui_surface_dark : R.color.ui_surface_light);

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            // Use light icons on dark bars and dark icons on light bars.
            boolean useDarkIcons = ColorUtils.calculateLuminance(statusBarColor) > 0.5;
            controller.setAppearanceLightStatusBars(useDarkIcons);
        }
    }
}
