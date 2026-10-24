package com.androiddjibridge.core;

/** Receives normalized events from a selected gimbal driver. */
public interface ControllerEventSink {
    boolean onControllerEvent(ControllerEvent event);
}
