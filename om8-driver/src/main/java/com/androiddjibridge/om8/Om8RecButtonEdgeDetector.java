package com.androiddjibridge.om8;

/** Stateless-event interface around the OM8 record-button rising edge. */
public final class Om8RecButtonEdgeDetector {
    public enum Event {
        NONE,
        PRESS,
        RELEASE
    }

    private boolean down;

    public Event update(boolean newDown) {
        Event event;
        if (!down && newDown) {
            event = Event.PRESS;
        } else if (down && !newDown) {
            event = Event.RELEASE;
        } else {
            event = Event.NONE;
        }
        down = newDown;
        return event;
    }

    public boolean isDown() {
        return down;
    }
}
