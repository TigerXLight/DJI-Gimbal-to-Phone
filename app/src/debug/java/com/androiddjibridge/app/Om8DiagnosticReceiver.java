package com.androiddjibridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.androiddjibridge.om8.Om8BleListenerService;

/** Debug-only explicit ADB bridge for the receive-only OM8 listener. */
public final class Om8DiagnosticReceiver extends BroadcastReceiver {
    private static final String TAG = "Om8DiagnosticReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!Om8BleListenerService.ACTION_START_LISTENER.equals(action)
                && !Om8BleListenerService.ACTION_STOP_LISTENER.equals(action)) {
            return;
        }
        Intent serviceIntent = new Intent(context, Om8BleListenerService.class)
                .setAction(action);
        try {
            if (Om8BleListenerService.ACTION_START_LISTENER.equals(action)
                    && Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.i(TAG, "forwarded action=" + action);
        } catch (SecurityException error) {
            Log.i(TAG, "could not start OM8 listener: " + error.getMessage());
        }
    }
}
