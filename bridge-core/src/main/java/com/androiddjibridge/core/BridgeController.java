package com.androiddjibridge.core;

/** Routes normalized gimbal events to the selected camera adapter. */
public final class BridgeController implements ControllerEventSink {
    private volatile CameraAdapter selectedAdapter;
    private volatile CameraProfileRegistry profileRegistry;
    private volatile CameraProfile selectedProfile;
    private final MappingEngine mappingEngine;
    private volatile GimbalDriver.ConnectionState gimbalConnectionState =
            GimbalDriver.ConnectionState.STOPPED;

    public BridgeController(CameraAdapter selectedAdapter) {
        this(CameraProfileRegistry.empty(), null, new MappingEngine(), selectedAdapter);
    }

    public BridgeController(
            CameraProfileRegistry profileRegistry,
            CameraProfile selectedProfile,
            CameraAdapter selectedAdapter) {
        this(profileRegistry, selectedProfile, new MappingEngine(), selectedAdapter);
    }

    public BridgeController(
            CameraProfileRegistry profileRegistry,
            CameraProfile selectedProfile,
            MappingEngine mappingEngine,
            CameraAdapter selectedAdapter) {
        if (profileRegistry == null) {
            throw new IllegalArgumentException("profileRegistry is required");
        }
        if (mappingEngine == null) {
            throw new IllegalArgumentException("mappingEngine is required");
        }
        if (selectedProfile != null && !profileRegistry.contains(selectedProfile)) {
            throw new IllegalArgumentException("selected profile is not registered");
        }
        this.profileRegistry = profileRegistry;
        this.selectedProfile = selectedProfile;
        this.mappingEngine = mappingEngine;
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

    public MappingEngine getMappingEngine() {
        return mappingEngine;
    }

    public boolean selectProfile(String stableId) {
        CameraProfile profile = profileRegistry.findByStableId(stableId).orElse(null);
        if (profile == null) {
            return false;
        }
        selectedProfile = profile;
        return true;
    }

    /** Replaces the immutable registry after app-private learned profiles are loaded. */
    public synchronized void replaceProfileRegistry(CameraProfileRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("profileRegistry is required");
        }
        CameraProfile current = selectedProfile;
        CameraProfile replacement = current == null
                ? null
                : registry.findByStableId(current.getStableId()).orElse(null);
        profileRegistry = registry;
        selectedProfile = replacement;
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
        MappingDecision decision = mappingEngine.resolve(event);
        if (!decision.isRecognizedTrigger()) {
            return false;
        }
        if (!decision.hasAction()) {
            // RECORD_RELEASE is intentionally a recognized no-op by default.
            return true;
        }
        CameraAction action = decision.getAction();
        if (!adapter.supports(action)) {
            return false;
        }
        return adapter.execute(action, event);
    }
}
