package com.androiddjibridge.core;

/** Snapshot of the three independent conditions needed for complete bridge readiness. */
public final class BridgeReadiness {
    public enum State {
        GIMBAL_NOT_READY,
        CAMERA_PROFILE_NOT_SELECTED,
        CAMERA_ADAPTER_UNAVAILABLE,
        PROFILE_ADAPTER_MISMATCH,
        READY
    }

    private final State state;
    private final GimbalDriver.ConnectionState gimbalState;
    private final CameraProfile profile;

    private BridgeReadiness(
            State state,
            GimbalDriver.ConnectionState gimbalState,
            CameraProfile profile) {
        this.state = state;
        this.gimbalState = gimbalState;
        this.profile = profile;
    }

    public static BridgeReadiness evaluate(
            GimbalDriver.ConnectionState gimbalState,
            CameraProfile profile,
            CameraAdapter adapter) {
        if (gimbalState != GimbalDriver.ConnectionState.READY) {
            return new BridgeReadiness(State.GIMBAL_NOT_READY, gimbalState, profile);
        }
        if (profile == null) {
            return new BridgeReadiness(
                    State.CAMERA_PROFILE_NOT_SELECTED, gimbalState, null);
        }
        if (adapter == null) {
            return new BridgeReadiness(
                    State.CAMERA_ADAPTER_UNAVAILABLE, gimbalState, profile);
        }
        CameraProfile adapterProfile = adapter.profile();
        if (adapterProfile != null
                && !profile.getStableId().equals(adapterProfile.getStableId())) {
            return new BridgeReadiness(
                    State.PROFILE_ADAPTER_MISMATCH, gimbalState, profile);
        }
        if (!adapter.isAvailable()) {
            return new BridgeReadiness(
                    State.CAMERA_ADAPTER_UNAVAILABLE, gimbalState, profile);
        }
        return new BridgeReadiness(State.READY, gimbalState, profile);
    }

    public State getState() {
        return state;
    }

    public boolean isReady() {
        return state == State.READY;
    }

    public GimbalDriver.ConnectionState getGimbalState() {
        return gimbalState;
    }

    public CameraProfile getProfile() {
        return profile;
    }
}
