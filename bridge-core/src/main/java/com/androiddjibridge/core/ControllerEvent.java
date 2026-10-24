package com.androiddjibridge.core;

/** A normalized event emitted by a gimbal driver. */
public final class ControllerEvent {
    public enum Type {
        RECORD_DOWN,
        RECORD_UP,
        ZOOM_IN,
        ZOOM_OUT,
        FOCUS,
        SLIDER_UP,
        SLIDER_DOWN,
        SLIDER_PRESS,
        SLIDER_AXIS,
        JOYSTICK,
        MODE
    }

    private final Type type;
    private final String sourceDriverId;
    private final long sequence;
    private final long callbackEnteredAtNanos;
    private final long eventDetectedAtNanos;
    private final boolean callbackOnMainLooper;
    private final long callbackThreadId;
    private final double axisValue;

    public ControllerEvent(Type type,
                           String sourceDriverId,
                           long sequence,
                           long callbackEnteredAtNanos,
                           long eventDetectedAtNanos,
                           boolean callbackOnMainLooper,
                           long callbackThreadId) {
        this(type, sourceDriverId, sequence, callbackEnteredAtNanos, eventDetectedAtNanos,
                callbackOnMainLooper, callbackThreadId, 0.0d);
    }

    public ControllerEvent(Type type,
                           String sourceDriverId,
                           long sequence,
                           long callbackEnteredAtNanos,
                           long eventDetectedAtNanos,
                           boolean callbackOnMainLooper,
                           long callbackThreadId,
                           double axisValue) {
        this.type = type;
        this.sourceDriverId = sourceDriverId == null ? "" : sourceDriverId;
        this.sequence = sequence;
        this.callbackEnteredAtNanos = callbackEnteredAtNanos;
        this.eventDetectedAtNanos = eventDetectedAtNanos;
        this.callbackOnMainLooper = callbackOnMainLooper;
        this.callbackThreadId = callbackThreadId;
        this.axisValue = axisValue;
    }

    public Type getType() {
        return type;
    }

    public String getSourceDriverId() {
        return sourceDriverId;
    }

    public long getSequence() {
        return sequence;
    }

    public long getCallbackEnteredAtNanos() {
        return callbackEnteredAtNanos;
    }

    public long getEventDetectedAtNanos() {
        return eventDetectedAtNanos;
    }

    public boolean isCallbackOnMainLooper() {
        return callbackOnMainLooper;
    }

    public long getCallbackThreadId() {
        return callbackThreadId;
    }

    /**
     * Normalized continuous input value. It is meaningful only for an axis event;
     * OM8 currently emits no measured slider axis event in this project.
     */
    public double getAxisValue() {
        return axisValue;
    }
}
