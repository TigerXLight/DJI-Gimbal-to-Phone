package com.androiddjibridge.core;

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
        System.out.println("BridgeControllerSelfTest: PASS");
    }

    private static final class RecordingAdapter implements CameraAdapter {
        private ControllerEvent lastEvent;
        private int calls;

        @Override public String id() { return "test"; }
        @Override public String displayName() { return "Test"; }
        @Override public boolean isAvailable() { return true; }
        @Override public void prepare() { }
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
