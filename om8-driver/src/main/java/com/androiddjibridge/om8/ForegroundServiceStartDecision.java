package com.androiddjibridge.om8;

/** Pure permission decision logic for the connected-device foreground service. */
public final class ForegroundServiceStartDecision {
    public enum Result {
        ALLOW,
        BLUETOOTH_RUNTIME_PERMISSION_REQUIRED,
        CONNECTED_DEVICE_FGS_PERMISSION_REQUIRED
    }

    private ForegroundServiceStartDecision() {
    }

    public static Result evaluate(int apiLevel,
                                  boolean scanGranted,
                                  boolean connectGranted,
                                  boolean connectedDeviceFgsGranted) {
        if (apiLevel >= 31 && (!scanGranted || !connectGranted)) {
            return Result.BLUETOOTH_RUNTIME_PERMISSION_REQUIRED;
        }
        if (apiLevel >= 34 && !connectedDeviceFgsGranted) {
            return Result.CONNECTED_DEVICE_FGS_PERMISSION_REQUIRED;
        }
        return Result.ALLOW;
    }
}
