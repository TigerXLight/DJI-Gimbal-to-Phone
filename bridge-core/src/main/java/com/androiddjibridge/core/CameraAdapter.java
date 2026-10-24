package com.androiddjibridge.core;

/** Application-specific output boundary for normalized controller events. */
public interface CameraAdapter {
    String id();

    String displayName();

    boolean isAvailable();

    void prepare();

    boolean handleControllerEvent(ControllerEvent event);

    CameraAdapterState getState();

    void release();
}
