package com.androiddjibridge.core;

/** Immutable result of resolving one normalized controller event. */
public final class MappingDecision {
    private final boolean recognizedTrigger;
    private final ControllerTrigger trigger;
    private final CameraAction action;

    private MappingDecision(
            boolean recognizedTrigger, ControllerTrigger trigger, CameraAction action) {
        this.recognizedTrigger = recognizedTrigger;
        this.trigger = trigger;
        this.action = action;
    }

    public static MappingDecision recognized(ControllerTrigger trigger, CameraAction action) {
        if (trigger == null || action == null) {
            throw new IllegalArgumentException("trigger and action are required");
        }
        return new MappingDecision(true, trigger, action);
    }

    public static MappingDecision unknown() {
        return new MappingDecision(false, null, CameraAction.NONE);
    }

    public boolean isRecognizedTrigger() {
        return recognizedTrigger;
    }

    public ControllerTrigger getTrigger() {
        return trigger;
    }

    public CameraAction getAction() {
        return action;
    }

    public boolean hasAction() {
        return action != CameraAction.NONE;
    }
}
