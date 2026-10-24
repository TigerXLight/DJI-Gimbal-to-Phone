package com.androiddjibridge.core;

/** Routes normalized gimbal events to the selected camera adapter. */
public final class BridgeController implements ControllerEventSink {
    private volatile CameraAdapter selectedAdapter;

    public BridgeController(CameraAdapter selectedAdapter) {
        this.selectedAdapter = selectedAdapter;
    }

    public void setSelectedAdapter(CameraAdapter adapter) {
        CameraAdapter old = selectedAdapter;
        selectedAdapter = adapter;
        if (old != null && old != adapter) {
            old.release();
        }
        if (adapter != null) {
            adapter.prepare();
        }
    }

    public CameraAdapter getSelectedAdapter() {
        return selectedAdapter;
    }

    @Override
    public boolean onControllerEvent(ControllerEvent event) {
        CameraAdapter adapter = selectedAdapter;
        if (adapter == null || event == null) {
            return false;
        }
        return adapter.handleControllerEvent(event);
    }
}
