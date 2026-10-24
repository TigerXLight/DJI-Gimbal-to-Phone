package com.androiddjibridge.core;

/** Application-specific output boundary for normalized controller events. */
public interface CameraAdapter {
    String id();

    String displayName();

    /**
     * Returns the immutable profile this adapter executes, when it has one.
     * A null value is allowed for test adapters and future non-profile adapters.
     */
    default CameraProfile profile() {
        return null;
    }

    boolean isAvailable();

    void prepare();

    boolean handleControllerEvent(ControllerEvent event);

    CameraAdapterState getState();

    void release();
}
