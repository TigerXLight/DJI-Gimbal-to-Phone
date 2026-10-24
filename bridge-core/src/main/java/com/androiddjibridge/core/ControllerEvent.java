package com.androiddjibridge.core;

/** A normalized event emitted by a gimbal driver. */
public final class ControllerEvent {
    public enum Type {
        RECORD_DOWN,
        RECORD_UP,
        ZOOM_IN,
        ZOOM_OUT,
        FOCUS,
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

    public ControllerEvent(Type type,
                           String sourceDriverId,
                           long sequence,
                           long callbackEnteredAtNanos,
                           long eventDetectedAtNanos,
                           boolean callbackOnMainLooper,
                           long callbackThreadId) {
        this.type = type;
        this.sourceDriverId = sourceDriverId == null ? "" : sourceDriverId;
        this.sequence = sequence;
        this.callbackEnteredAtNanos = callbackEnteredAtNanos;
        this.eventDetectedAtNanos = eventDetectedAtNanos;
        this.callbackOnMainLooper = callbackOnMainLooper;
        this.callbackThreadId = callbackThreadId;
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
}
