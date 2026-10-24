package com.androiddjibridge.core;

import java.util.Collections;

/** Pure routing regression for the generic driver-to-camera boundary. */
public final class BridgeControllerSelfTest {
    public static void main(String[] args) {
        RecordingAdapter adapter = new RecordingAdapter();
        BridgeController controller = new BridgeController(adapter);
        ControllerEvent down = new ControllerEvent(
                ControllerEvent.Type.RECORD_DOWN, "dji-om8", 1L, 10L, 11L, true, 7L);
        require(controller.onControllerEvent(down), "event must route");
        require(adapter.lastEvent == down, "same event must reach adapter");
        require(adapter.calls == 1, "one event must route once");
        require(!controller.onControllerEvent(null), "null event must reject");
        verifyReadinessStates();
        System.out.println("BridgeControllerSelfTest: PASS");
    }

    private static void verifyReadinessStates() {
        CameraProfile profile = CameraProfile.builder()
                .stableId("test-camera")
                .displayName("Test camera")
                .packageName("com.example.camera")
                .recordButtonResourceId("com.example.camera:id/recording")
                .recordButtonClassName("android.widget.ImageButton")
                .requiredAccessibilityActions(
                        Collections.singleton(CameraAccessibilityAction.CLICK))
                .capabilities(Collections.singleton(CameraCapability.RECORD_TOGGLE))
                .origin(CameraProfileOrigin.IMPORTED)
                .validationStatus(CameraProfileValidationStatus.UNTESTED)
                .build();
        CameraProfileRegistry registry = new CameraProfileRegistry(
                Collections.singleton(profile), profile.getStableId());

        BridgeController noProfile = new BridgeController(
                registry, null, new RecordingAdapter(true, profile));
        noProfile.setGimbalConnectionState(GimbalDriver.ConnectionState.READY);
        require(!noProfile.isBridgeReady(), "profile must be selected");
        require(noProfile.getReadiness().getState()
                        == BridgeReadiness.State.CAMERA_PROFILE_NOT_SELECTED,
                "missing profile readiness state");

        BridgeController unavailable = new BridgeController(
                registry, profile, new RecordingAdapter(false, profile));
        unavailable.setGimbalConnectionState(GimbalDriver.ConnectionState.READY);
        require(!unavailable.isBridgeReady(), "unavailable adapter must not be ready");
        require(unavailable.getReadiness().getState()
                        == BridgeReadiness.State.CAMERA_ADAPTER_UNAVAILABLE,
                "unavailable adapter readiness state");

        BridgeController ready = new BridgeController(
                registry, profile, new RecordingAdapter(true, profile));
        ready.setGimbalConnectionState(GimbalDriver.ConnectionState.READY);
        require(ready.isBridgeReady(), "all bridge components must be ready");
        require(ready.getReadiness().getState() == BridgeReadiness.State.READY,
                "ready readiness state");
    }

    private static final class RecordingAdapter implements CameraAdapter {
        private ControllerEvent lastEvent;
        private int calls;
        private final boolean available;
        private final CameraProfile profile;

        private RecordingAdapter() {
            this(true, null);
        }

        private RecordingAdapter(boolean available, CameraProfile profile) {
            this.available = available;
            this.profile = profile;
        }

        @Override public String id() { return "test"; }
        @Override public String displayName() { return "Test"; }
        @Override public boolean isAvailable() { return available; }
        @Override public CameraProfile profile() { return profile; }
        @Override public void prepare() { }
        @Override public boolean supports(CameraAction action) {
            return action == CameraAction.RECORD_TOGGLE;
        }
        @Override public boolean execute(CameraAction action, ControllerEvent event) {
            if (action != CameraAction.RECORD_TOGGLE) {
                return false;
            }
            return handleControllerEvent(event);
        }
        @Override public boolean handleControllerEvent(ControllerEvent event) {
            lastEvent = event;
            calls++;
            return true;
        }
        @Override public CameraAdapterState getState() { return CameraAdapterState.READY; }
        @Override public void release() { }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
