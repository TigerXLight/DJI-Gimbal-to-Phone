package com.androiddjibridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.androiddjibridge.sony.VideoProKeyAccessibilityService;

/**
 * Debug-only, DUMP-protected trigger for the temporary Photography Pro probe.
 * The service performs all foreground and device-safety checks before sending
 * its package-scoped CAMERA_BUTTON pair.
 */
public final class PhotographyProCameraButtonProbeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !VideoProKeyAccessibilityService.ACTION_TEST_PHOTOPRO_CAMERA_BUTTON_PROBE
                .equals(intent.getAction())) {
            return;
        }
        VideoProKeyAccessibilityService.requestPhotographyProCameraButtonProbe();
    }
}
