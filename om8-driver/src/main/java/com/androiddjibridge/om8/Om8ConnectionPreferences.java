package com.androiddjibridge.om8;

import android.content.Context;

/** User intent that distinguishes an explicit disconnect from auto-recovery. */
public final class Om8ConnectionPreferences {
    private static final String PREFERENCES = "om8_connection_preferences";
    private static final String MANUAL_DISCONNECT = "manual_disconnect";

    private Om8ConnectionPreferences() {
    }

    public static boolean isManualDisconnect(Context context) {
        return context != null && context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(MANUAL_DISCONNECT, false);
    }

    public static void setManualDisconnect(Context context, boolean value) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean(MANUAL_DISCONNECT, value).apply();
    }
}
