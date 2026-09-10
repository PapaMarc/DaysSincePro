package com.merware.dayssincepro;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/**
 * Process-local developer tools state used for sideload diagnostics.
 */
public final class DeveloperToolsSession {

    private static final String TAG = "DSP_DEVTOOLS";
    private static final int MAX_BUFFER_LINES = 2000;
    private static final Object LOCK = new Object();

    private static boolean unlockedForSession = false;
    private static boolean loggingEnabled = false;
    private static boolean pseudoLangsEnabled = false;
    private static boolean sideloadBuild = false;
    private static final ArrayDeque<String> bufferedLines = new ArrayDeque<String>();

    private DeveloperToolsSession() {
        // Utility class.
    }

    static void initialize(String packageName) {
        synchronized (LOCK) {
            sideloadBuild = packageName != null && packageName.endsWith(".dev");
            if (!sideloadBuild) {
                unlockedForSession = false;
                loggingEnabled = false;
                pseudoLangsEnabled = false;
                bufferedLines.clear();
            }
        }
    }

    static boolean isAvailable() {
        synchronized (LOCK) {
            return sideloadBuild;
        }
    }

    static boolean isUnlocked() {
        synchronized (LOCK) {
            return unlockedForSession;
        }
    }

    static void unlockForSession() {
        synchronized (LOCK) {
            if (!sideloadBuild) {
                return;
            }
            unlockedForSession = true;
        }
    }

    static boolean isLoggingEnabled() {
        synchronized (LOCK) {
            return sideloadBuild && loggingEnabled;
        }
    }

    static void setLoggingEnabled(boolean enabled) {
        synchronized (LOCK) {
            if (!sideloadBuild) {
                return;
            }
            loggingEnabled = enabled;
        }
    }

    static boolean isPseudoLangsEnabled() {
        synchronized (LOCK) {
            return sideloadBuild && pseudoLangsEnabled;
        }
    }

    static void enablePseudoLangsForSession() {
        synchronized (LOCK) {
            if (!sideloadBuild) {
                return;
            }
            pseudoLangsEnabled = true;
        }
    }

    static void logSessionMarker(String message) {
        String line;
        synchronized (LOCK) {
            if (!sideloadBuild) {
                return;
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                    .format(new Date());
            line = timestamp + " | DeveloperToolsSession | " + message;

            bufferedLines.addLast(line);
            while (bufferedLines.size() > MAX_BUFFER_LINES) {
                bufferedLines.removeFirst();
            }
        }

        Log.i(TAG, line);
    }

    static void log(String source, String message) {
        String line;
        synchronized (LOCK) {
            if (!(sideloadBuild && loggingEnabled)) {
                return;
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                    .format(new Date());
            line = timestamp + " | " + source + " | " + message;

            bufferedLines.addLast(line);
            while (bufferedLines.size() > MAX_BUFFER_LINES) {
                bufferedLines.removeFirst();
            }
        }

        Log.i(TAG, line);
    }

    static String defaultDiagnosticsFilename() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        return "daysSince_Diags_" + timestamp + ".log";
    }

    static int getBufferedLineCount() {
        synchronized (LOCK) {
            return bufferedLines.size();
        }
    }

    static String buildDiagnosticsReport() {
        synchronized (LOCK) {
            StringBuilder sb = new StringBuilder();
            sb.append("# DaysSincePro Developer Diagnostics\n");
            sb.append("# tag=").append(TAG).append("\n");
            sb.append("# generated=")
                    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                            .format(new Date()))
                    .append("\n");
            sb.append("# bufferedLines=").append(bufferedLines.size()).append("\n");
            sb.append("\n");

            for (String line : bufferedLines) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}