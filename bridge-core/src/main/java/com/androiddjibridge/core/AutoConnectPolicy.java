package com.androiddjibridge.core;

/** Pure policy for deciding whether a presence signal may start the driver. */
public final class AutoConnectPolicy {
    public enum Decision {
        NO_ASSOCIATION,
        BLUETOOTH_UNAVAILABLE,
        ALREADY_ACTIVE,
        START
    }

    private AutoConnectPolicy() {
    }

    public static Decision decide(
            boolean hasAssociation,
            boolean bluetoothEnabled,
            GimbalDriver.ConnectionState state) {
        if (!hasAssociation) {
            return Decision.NO_ASSOCIATION;
        }
        if (!bluetoothEnabled) {
            return Decision.BLUETOOTH_UNAVAILABLE;
        }
        if (state == GimbalDriver.ConnectionState.SCANNING
                || state == GimbalDriver.ConnectionState.CONNECTING
                || state == GimbalDriver.ConnectionState.DISCOVERING
                || state == GimbalDriver.ConnectionState.ENABLING_NOTIFICATIONS
                || state == GimbalDriver.ConnectionState.READY
                || state == GimbalDriver.ConnectionState.RETRY_WAIT) {
            return Decision.ALREADY_ACTIVE;
        }
        return Decision.START;
    }
}
