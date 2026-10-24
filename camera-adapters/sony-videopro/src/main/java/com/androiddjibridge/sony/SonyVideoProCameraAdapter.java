package com.androiddjibridge.sony;

import com.androiddjibridge.core.CameraAdapter;
import com.androiddjibridge.core.CameraAdapterState;
import com.androiddjibridge.core.ControllerEvent;

/** Sony Video Pro output adapter; all Accessibility safety remains in the service. */
public final class SonyVideoProCameraAdapter implements CameraAdapter {
    public static final String PACKAGE_NAME = "jp.co.sony.mc.videopro";

    private volatile CameraAdapterState state = CameraAdapterState.UNAVAILABLE;

    @Override
    public String id() {
        return "sony-videopro";
    }

    @Override
    public String displayName() {
        return "Sony Video Pro";
    }

    @Override
    public boolean isAvailable() {
        return VideoProKeyAccessibilityService.isConnected();
    }

    @Override
    public void prepare() {
        state = isAvailable() ? CameraAdapterState.READY : CameraAdapterState.UNAVAILABLE;
    }

    @Override
    public boolean handleControllerEvent(ControllerEvent event) {
        if (event == null) {
            state = CameraAdapterState.ACTION_REJECTED;
            return false;
        }
        if (event.getType() == ControllerEvent.Type.RECORD_UP) {
            // Release is normalized for future adapters but never toggles Sony.
            return true;
        }
        if (event.getType() != ControllerEvent.Type.RECORD_DOWN
                || !VideoProKeyAccessibilityService.isConnected()) {
            state = CameraAdapterState.ACTION_REJECTED;
            return false;
        }

        Om8SonyTriggerTiming timing = new Om8SonyTriggerTiming();
        timing.om8PressSequence = event.getSequence();
        timing.tBleCallbackEnterNanos = event.getCallbackEnteredAtNanos();
        timing.tEdgeDetectEndNanos = event.getEventDetectedAtNanos();
        timing.gattCallbackOnMainLooper = event.isCallbackOnMainLooper();
        timing.gattCallbackThreadId = event.getCallbackThreadId();
        timing.tBridgeRequestStartNanos = android.os.SystemClock.elapsedRealtimeNanos();
        timing.tSonyTriggerRequestedNanos = timing.tBridgeRequestStartNanos;
        boolean accepted = VideoProKeyAccessibilityService
                .requestSonyRecActionFromController(timing);
        timing.tBridgeRequestEndNanos = android.os.SystemClock.elapsedRealtimeNanos();
        state = accepted
                ? CameraAdapterState.ACTION_ACCEPTED
                : CameraAdapterState.ACTION_REJECTED;
        return accepted;
    }

    @Override
    public CameraAdapterState getState() {
        return state;
    }

    @Override
    public void release() {
        state = CameraAdapterState.UNAVAILABLE;
    }
}
