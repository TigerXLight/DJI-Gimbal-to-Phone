package com.androiddjibridge.core;

/** Pure timing policy for future controller gesture decoders. It does not decode OM8 bytes. */
public final class ControllerGesturePolicy {
    private final long longPressNanos;
    private final long doublePressWindowNanos;

    public ControllerGesturePolicy(long longPressNanos, long doublePressWindowNanos) {
        if (longPressNanos <= 0L || doublePressWindowNanos <= 0L) {
            throw new IllegalArgumentException("gesture windows must be positive");
        }
        this.longPressNanos = longPressNanos;
        this.doublePressWindowNanos = doublePressWindowNanos;
    }

    public boolean isLongPress(long downNanos, long upNanos) {
        return upNanos >= downNanos && upNanos - downNanos >= longPressNanos;
    }

    public boolean isDoublePress(long firstUpNanos, long secondDownNanos) {
        return secondDownNanos >= firstUpNanos
                && secondDownNanos - firstUpNanos <= doublePressWindowNanos;
    }

    /** The proven OM8 default remains immediate: no double-press timeout is inserted. */
    public boolean defaultRecordPressIsImmediate() {
        return true;
    }
}
