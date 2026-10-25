package com.androiddjibridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.androiddjibridge.om8.Om8ConnectionPreferences;

/** Wakes the associated-device foreground service after normal Android boot. */
public final class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "Om8BootRecovery";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (new CompanionAssociationStore(context).load() == null) {
            Log.i(TAG, "boot recovery skipped: no associated OM8");
            return;
        }
        if (Om8ConnectionPreferences.isManualDisconnect(context)) {
            Log.i(TAG, "boot recovery skipped: manual disconnect is active");
            return;
        }
        boolean started = AutoConnectServiceStarter.start(context, "boot-completed");
        Log.i(TAG, "boot recovery service start submitted=" + started);
    }
}
