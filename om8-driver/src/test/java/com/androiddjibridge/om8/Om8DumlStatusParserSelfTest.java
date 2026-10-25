package com.androiddjibridge.om8;

/** Regression vectors for OM8 DUML routing, record bit, and edge semantics. */
public final class Om8DumlStatusParserSelfTest {
    public static void main(String[] args) {
        byte[] normal = hex("55 15 04 A9 27 02 B1 03 40 04 57 00 00 00 00 01 00 00 00 00 00");
        byte[] pressed = hex("55 15 04 A9 27 02 B2 03 40 04 57 00 00 00 00 01 00 08 00 00 00");
        require(Om8DumlStatusParser.parse(normal).isValid(), "normal status must parse");
        require(!Om8DumlStatusParser.parse(normal).isRecordButtonDown(), "normal must be released");
        require(Om8DumlStatusParser.parse(pressed).isValid(), "pressed status must parse");
        require(Om8DumlStatusParser.parse(pressed).isRecordButtonDown(), "pressed must be down");

        byte[] verifiedControls = normal.clone();
        verifiedControls[11] = (byte) 0x7B;
        verifiedControls[12] = (byte) 0xFD; // -645; do not depend on an exact sentinel.
        verifiedControls[13] = 0x34;
        verifiedControls[14] = 0x12; // 0x1234.
        verifiedControls[15] = (byte) Om8DumlStatusParser.TRIGGER_STATE_61;
        verifiedControls[16] = (byte) Om8DumlStatusParser.SWITCH_STATE_DOUBLE;
        verifiedControls[17] = (byte) Om8DumlStatusParser.SIDE_WHEEL_PRESS_STATE;
        Om8DumlStatusParser.Result controls = Om8DumlStatusParser.parse(verifiedControls);
        require(controls.getJoystickX() == -645, "signed joystick X decode");
        require(controls.getJoystickY() == 0x1234, "signed joystick Y decode");
        require(controls.getTriggerState() == Om8DumlStatusParser.TRIGGER_STATE_61,
                "trigger state is preserved without over-interpreting it");
        require(controls.isObservedTriggerState(), "observed trigger state");
        require(controls.isSwitchDoubleState(), "switch double state");
        require(controls.isSideWheelPressState(), "side-wheel press state");
        byte[] mButton = normal.clone();
        mButton[15] = (byte) Om8DumlStatusParser.M_BUTTON_STATE;
        require(Om8DumlStatusParser.parse(mButton).isMButtonState(),
                "M button state");
        require(!controls.isJoystickNeutral(128), "joystick deadzone must detect movement");
        require(Om8DumlStatusParser.parse(normal).isJoystickNeutral(128),
                "idle joystick must be within deadzone");

        byte[] wrong = pressed.clone();
        wrong[10] = 0x56;
        require(!Om8DumlStatusParser.parse(wrong).isValid(), "wrong command must reject");
        byte[] truncated = pressed.clone();
        truncated[1] = 0x16;
        require(!Om8DumlStatusParser.parse(truncated).isValid(), "truncated frame must reject");

        Om8RecButtonEdgeDetector detector = new Om8RecButtonEdgeDetector();
        require(detector.update(false) == Om8RecButtonEdgeDetector.Event.NONE, "initial release");
        require(detector.update(true) == Om8RecButtonEdgeDetector.Event.PRESS, "rising edge");
        require(detector.update(true) == Om8RecButtonEdgeDetector.Event.NONE, "held state no duplicate");
        require(detector.update(false) == Om8RecButtonEdgeDetector.Event.RELEASE, "falling edge");
        require(detector.update(true) == Om8RecButtonEdgeDetector.Event.PRESS, "second press");
        System.out.println("Om8DumlStatusParserSelfTest: PASS");
    }

    private static byte[] hex(String value) {
        String compact = value.replace(" ", "");
        byte[] result = new byte[compact.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(compact.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
