package com.androiddjibridge.app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.androiddjibridge.om8.Om8BleListenerService;

/** Starts the existing connected-device GATT service from an allowed lifecycle signal. */
public final class AutoConnectServiceStarter {
    private static final String TAG = "Om8AutoConnect";

    private AutoConnectServiceStarter() {
    }

    public static boolean start(Context context, String reason) {
        if (context == null) {
            return false;
        }
        Intent intent = new Intent(context, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_AUTO_START_LISTENER)
                .putExtra(Om8BleListenerService.EXTRA_START_REASON,
                        reason == null ? "unspecified" : reason);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            Log.i(TAG, "automatic OM8 service start submitted reason=" + reason);
            return true;
        } catch (SecurityException | IllegalStateException error) {
            Log.w(TAG, "automatic OM8 service start rejected reason=" + reason
                    + " error=" + error.getClass().getSimpleName(), error);
            return false;
        }
    }
}
