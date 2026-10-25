package com.androiddjibridge.om8;

import android.content.Context;

/** App-private persistence for the explicit OM8 raw-capture diagnostic mode. */
public final class Om8DiagnosticsPreferences {
    private static final String PREFERENCES = "om8_diagnostics";
    private static final String ENABLED = "raw_enabled";

    private Om8DiagnosticsPreferences() {
    }

    public static boolean isEnabled(Context context) {
        return context != null
                && context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false);
    }

    public static boolean setEnabled(Context context, boolean enabled) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ENABLED, enabled)
                .commit();
    }
}
