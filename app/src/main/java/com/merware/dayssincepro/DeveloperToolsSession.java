package com.merware.dayssincepro;

import android.util.Log;

/**
 * Process-local developer tools state used for sideload diagnostics.
 */
public final class DeveloperToolsSession {

    private static final String TAG = "DSP_DEVTOOLS";

    private static boolean unlockedForSession = false;
    private static boolean loggingEnabled = false;
    private static boolean sideloadBuild = false;

    private DeveloperToolsSession() {
        // Utility class.
    }

    static void initialize(String packageName) {
        sideloadBuild = packageName != null && packageName.endsWith(".dev");
        if (!sideloadBuild) {
            unlockedForSession = false;
            loggingEnabled = false;
        }
    }

    static boolean isAvailable() {
        return sideloadBuild;
    }

    static boolean isUnlocked() {
        return unlockedForSession;
    }

    static void unlockForSession() {
        if (!isAvailable()) {
            return;
        }
        unlockedForSession = true;
    }

    static boolean isLoggingEnabled() {
        return isAvailable() && loggingEnabled;
    }

    static void setLoggingEnabled(boolean enabled) {
        if (!isAvailable()) {
            return;
        }
        loggingEnabled = enabled;
    }

    static void log(String source, String message) {
        if (!isLoggingEnabled()) {
            return;
        }
        Log.i(TAG, source + " | " + message);
    }
}