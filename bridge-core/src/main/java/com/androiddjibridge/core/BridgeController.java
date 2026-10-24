package com.androiddjibridge.core;

/** Routes normalized gimbal events to the selected camera adapter. */
public final class BridgeController implements ControllerEventSink {
    private volatile CameraAdapter selectedAdapter;
    private final CameraProfileRegistry profileRegistry;
    private volatile CameraProfile selectedProfile;
    private volatile GimbalDriver.ConnectionState gimbalConnectionState =
            GimbalDriver.ConnectionState.STOPPED;

    public BridgeController(CameraAdapter selectedAdapter) {
        this(CameraProfileRegistry.empty(), null, selectedAdapter);
    }

    public BridgeController(
            CameraProfileRegistry profileRegistry,
            CameraProfile selectedProfile,
            CameraAdapter selectedAdapter) {
        if (profileRegistry == null) {
            throw new IllegalArgumentException("profileRegistry is required");
        }
        if (selectedProfile != null && !profileRegistry.contains(selectedProfile)) {
            throw new IllegalArgumentException("selected profile is not registered");
        }
        this.profileRegistry = profileRegistry;
        this.selectedProfile = selectedProfile;
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

    public CameraProfileRegistry getProfileRegistry() {
        return profileRegistry;
    }

    public CameraProfile getSelectedProfile() {
        return selectedProfile;
    }

    public boolean selectProfile(String stableId) {
        CameraProfile profile = profileRegistry.findByStableId(stableId).orElse(null);
        if (profile == null) {
            return false;
        }
        selectedProfile = profile;
        return true;
    }

    public void clearSelectedProfile() {
        selectedProfile = null;
    }

    public void setGimbalConnectionState(GimbalDriver.ConnectionState state) {
        gimbalConnectionState = state == null
                ? GimbalDriver.ConnectionState.DISCONNECTED
                : state;
    }

    public GimbalDriver.ConnectionState getGimbalConnectionState() {
        return gimbalConnectionState;
    }

    public BridgeReadiness getReadiness() {
        return BridgeReadiness.evaluate(
                gimbalConnectionState, selectedProfile, selectedAdapter);
    }

    public boolean isBridgeReady() {
        return getReadiness().isReady();
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
