package com.androiddjibridge.core;

/** Normalized controller triggers understood by the mapping layer. */
public enum ControllerTrigger {
    RECORD_PRESS,
    RECORD_RELEASE,
    SLIDER_UP,
    SLIDER_DOWN,
    SLIDER_PRESS,
    SLIDER_AXIS,
    ZOOM_IN,
    ZOOM_OUT,
    FOCUS,
    JOYSTICK,
    MODE;

    public boolean isContinuous() {
        return this == SLIDER_AXIS;
    }

    public static ControllerTrigger from(ControllerEvent event) {
        if (event == null || event.getType() == null) {
            return null;
        }
        switch (event.getType()) {
            case RECORD_DOWN: return RECORD_PRESS;
            case RECORD_UP: return RECORD_RELEASE;
            case SLIDER_UP: return SLIDER_UP;
            case SLIDER_DOWN: return SLIDER_DOWN;
            case SLIDER_PRESS: return SLIDER_PRESS;
            case SLIDER_AXIS: return SLIDER_AXIS;
            case ZOOM_IN: return ZOOM_IN;
            case ZOOM_OUT: return ZOOM_OUT;
            case FOCUS: return FOCUS;
            case JOYSTICK: return JOYSTICK;
            case MODE: return MODE;
            default: return null;
        }
    }
}
