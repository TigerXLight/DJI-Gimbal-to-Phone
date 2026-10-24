package com.androiddjibridge.core;

public final class ControllerGesturePolicySelfTest {
    public static void main(String[] args) {
        ControllerGesturePolicy policy = new ControllerGesturePolicy(500_000_000L,
                300_000_000L);
        if (!policy.isLongPress(0L, 500_000_000L)
                || !policy.isDoublePress(1_000_000_000L, 1_200_000_000L)
                || !policy.defaultRecordPressIsImmediate()) {
            throw new AssertionError("gesture policy regression");
        }
        if (policy.isDoublePress(1_000_000_000L, 1_400_000_001L)) {
            throw new AssertionError("double press window regression");
        }
    }
}
