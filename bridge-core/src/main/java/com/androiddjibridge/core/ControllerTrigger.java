package com.androiddjibridge.core;

/** Normalized controller triggers understood by the mapping layer. */
public enum ControllerTrigger {
    RECORD_PRESS,
    RECORD_RELEASE,
    RECORD_SINGLE_PRESS,
    RECORD_DOUBLE_PRESS,
    RECORD_LONG_PRESS,
    RECORD_HOLD_START,
    RECORD_HOLD_END,
    SLIDER_UP,
    SLIDER_DOWN,
    SLIDER_PRESS,
    SLIDER_AXIS,
    SLIDER_HOLD,
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
            case RECORD_SINGLE_PRESS: return RECORD_SINGLE_PRESS;
            case RECORD_DOUBLE_PRESS: return RECORD_DOUBLE_PRESS;
            case RECORD_LONG_PRESS: return RECORD_LONG_PRESS;
            case RECORD_HOLD_START: return RECORD_HOLD_START;
            case RECORD_HOLD_END: return RECORD_HOLD_END;
            case SLIDER_UP: return SLIDER_UP;
            case SLIDER_DOWN: return SLIDER_DOWN;
            case SLIDER_PRESS: return SLIDER_PRESS;
            case SLIDER_AXIS: return SLIDER_AXIS;
            case SLIDER_HOLD: return SLIDER_HOLD;
            case ZOOM_IN: return ZOOM_IN;
            case ZOOM_OUT: return ZOOM_OUT;
            case FOCUS: return FOCUS;
            case JOYSTICK: return JOYSTICK;
            case MODE: return MODE;
            default: return null;
        }
    }
}
