package com.androiddjibridge.core;

/** Camera semantic actions. No controller or DJI-specific data is stored here. */
public enum CameraAction {
    NONE,
    CAPTURE_PHOTO,
    RECORD_TOGGLE,
    FOCUS,
    ZOOM_IN,
    ZOOM_OUT,
    SWITCH_LENS,
    ZOOM_AXIS,
    FOCUS_AXIS;

    public boolean isContinuous() {
        return this == ZOOM_AXIS || this == FOCUS_AXIS;
    }
}
