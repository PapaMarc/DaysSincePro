package com.merware.dayssincepro;

import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Enables edge-to-edge with compatible system-bar styling and keeps app content clear
 * of the system bars.
 */
class EdgeToEdgeUtil {

    static void applyContentInsets(ComponentActivity activity) {
        int surfaceColor = activity.getColor(
                ThemeMode.isDark(activity) ? R.color.ui_surface_dark : R.color.ui_surface_light);
        SystemBarStyle barStyle = ColorUtils.calculateLuminance(surfaceColor) > 0.5
                ? SystemBarStyle.light(surfaceColor, surfaceColor)
                : SystemBarStyle.dark(surfaceColor);
        EdgeToEdge.enable(activity, barStyle, barStyle);

        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
