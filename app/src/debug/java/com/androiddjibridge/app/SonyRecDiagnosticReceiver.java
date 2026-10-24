package com.androiddjibridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.androiddjibridge.sony.VideoProKeyAccessibilityService;

/** Debug-only explicit ADB bridge to the live accessibility service. */
public final class SonyRecDiagnosticReceiver extends BroadcastReceiver {
    public static final String ACTION_TEST_SONY_REC_CLICK =
            VideoProKeyAccessibilityService.ACTION_TEST_SONY_REC_CLICK;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_TEST_SONY_REC_CLICK.equals(intent.getAction())) {
            return;
        }
        VideoProKeyAccessibilityService.requestDiagnosticRecClick();
    }
}
