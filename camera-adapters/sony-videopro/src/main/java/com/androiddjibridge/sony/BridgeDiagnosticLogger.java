package com.androiddjibridge.sony;

import android.content.Context;
import android.os.SystemClock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small append-only diagnostic log used during hardware validation. */
public final class BridgeDiagnosticLogger {
    private final File logFile;
    private final SimpleDateFormat wallClock =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    public BridgeDiagnosticLogger(Context context) {
        File root = context.getExternalFilesDir("diagnostics");
        if (root == null) {
            root = context.getFilesDir();
        }
        if (!root.exists()) {
            //noinspection ResultOfMethodCallIgnored
            root.mkdirs();
        }
        logFile = new File(root, "android_dji_om_bridge.log");
    }

    public synchronized void log(String tag, String message) {
        String line = String.format(
                Locale.US,
                "%s monotonicMs=%d [%s] %s%n",
                wallClock.format(new Date()),
                SystemClock.elapsedRealtime(),
                tag,
                message);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(line);
        } catch (IOException ignored) {
            // Diagnostics must never take down the BLE controller.
        }
    }

    public synchronized void packet(String direction, byte[] payload) {
        log(direction, toHex(payload));
    }

    public File getLogFile() {
        return logFile;
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "<null>";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02X ", value & 0xFF));
        }
        return builder.toString().trim();
    }
}
