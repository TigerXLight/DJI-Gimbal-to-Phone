package com.androiddjibridge.core;

import java.util.EnumSet;

/** Generic lifecycle and capability boundary for a gimbal driver. */
public interface GimbalDriver {
    enum ConnectionState {
        STOPPED,
        DISCONNECTED,
        SCANNING,
        CONNECTING,
        DISCOVERING,
        ENABLING_NOTIFICATIONS,
        READY,
        RETRY_WAIT
    }

    enum Capability {
        RECORD,
        ZOOM,
        FOCUS,
        JOYSTICK,
        MODE
    }

    String id();

    String displayName();

    void start();

    void stop();

    ConnectionState connectionState();

    EnumSet<Capability> supportedCapabilities();

    void setEventSink(ControllerEventSink sink);
}
