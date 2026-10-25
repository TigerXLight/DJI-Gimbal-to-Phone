package com.androiddjibridge.om8;

import java.util.Arrays;

/**
 * Receive-only parser for the OM8 DUML status frame observed on FFF4.
 * It validates the frame structure and routing before exposing the
 * evidence-backed passive control fields. It does not send commands or infer
 * an unmeasured side-wheel rotation field.
 */
public final class Om8DumlStatusParser {
    public static final int FRAME_MARKER = 0x55;
    public static final int SENDER = 0x27;
    public static final int RECEIVER = 0x02;
    public static final int FLAGS = 0x40;
    public static final int COMMAND_SET = 0x04;
    public static final int COMMAND_ID = 0x57;
    public static final int M_BUTTON_STATE = 0x03;
    public static final int TRIGGER_STATE_SINGLE = 0x11;
    public static final int TRIGGER_STATE_DOUBLE = 0x21;
    public static final int TRIGGER_STATE_61 = 0x61;
    public static final int TRIGGER_STATE_71 = 0x71;
    public static final int SWITCH_STATE_SINGLE = 0x20;
    public static final int SWITCH_STATE_DOUBLE = 0x40;
    public static final int SIDE_WHEEL_PRESS_STATE = 0x01;
    public static final int RECORD_BIT = 0x08;

    private static final int LENGTH_OFFSET = 1;
    private static final int SENDER_OFFSET = 4;
    private static final int RECEIVER_OFFSET = 5;
    private static final int FLAGS_OFFSET = 8;
    private static final int COMMAND_SET_OFFSET = 9;
    private static final int COMMAND_ID_OFFSET = 10;
    private static final int PAYLOAD_OFFSET = 11;
    private static final int CRC_LENGTH = 2;
    private static final int REQUIRED_PAYLOAD_LENGTH = 7;
    private static final int MIN_FRAME_LENGTH = PAYLOAD_OFFSET
            + REQUIRED_PAYLOAD_LENGTH + CRC_LENGTH;

    private Om8DumlStatusParser() {
    }

    public static Result parse(byte[] notification) {
        if (notification == null) {
            return Result.invalid("null-notification");
        }
        if (notification.length < 2) {
            return Result.invalid("notification-too-short-for-length");
        }
        int declaredLength = notification[LENGTH_OFFSET] & 0xFF;
        if (declaredLength < MIN_FRAME_LENGTH) {
            return Result.invalid("frame-length-too-short=" + declaredLength);
        }
        if (declaredLength > notification.length) {
            return Result.invalid("truncated-frame declared=" + declaredLength
                    + " received=" + notification.length);
        }

        byte[] frame = Arrays.copyOf(notification, declaredLength);
        if ((frame[0] & 0xFF) != FRAME_MARKER) {
            return Result.invalid("bad-marker");
        }
        if ((frame[SENDER_OFFSET] & 0xFF) != SENDER) {
            return Result.invalid("unexpected-sender");
        }
        if ((frame[RECEIVER_OFFSET] & 0xFF) != RECEIVER) {
            return Result.invalid("unexpected-receiver");
        }
        if ((frame[FLAGS_OFFSET] & 0xFF) != FLAGS) {
            return Result.invalid("unexpected-flags");
        }
        if ((frame[COMMAND_SET_OFFSET] & 0xFF) != COMMAND_SET) {
            return Result.invalid("unexpected-command-set");
        }
        if ((frame[COMMAND_ID_OFFSET] & 0xFF) != COMMAND_ID) {
            return Result.invalid("unexpected-command-id");
        }

        int payloadLength = declaredLength - PAYLOAD_OFFSET - CRC_LENGTH;
        if (payloadLength < REQUIRED_PAYLOAD_LENGTH) {
            return Result.invalid("payload-too-short=" + payloadLength);
        }
        int joystickX = signedLittleEndian16(frame, PAYLOAD_OFFSET);
        int joystickY = signedLittleEndian16(frame, PAYLOAD_OFFSET + 2);
        int modeByte = frame[PAYLOAD_OFFSET + 4] & 0xFF;
        int triggerByte = modeByte;
        int switchByte = frame[PAYLOAD_OFFSET + 5] & 0xFF;
        int buttonByte = frame[PAYLOAD_OFFSET + 6] & 0xFF;
        return Result.valid(frame, joystickX, joystickY, modeByte, triggerByte,
                switchByte, buttonByte);
    }

    private static int signedLittleEndian16(byte[] value, int offset) {
        int unsigned = (value[offset] & 0xFF) | ((value[offset + 1] & 0xFF) << 8);
        return unsigned >= 0x8000 ? unsigned - 0x10000 : unsigned;
    }

    public static final class Result {
        private final boolean valid;
        private final String reason;
        private final byte[] frame;
        private final int joystickX;
        private final int joystickY;
        private final int modeByte;
        private final int triggerByte;
        private final int switchByte;
        private final int buttonByte;

        private Result(boolean valid,
                       String reason,
                       byte[] frame,
                       int joystickX,
                       int joystickY,
                       int modeByte,
                       int triggerByte,
                       int switchByte,
                       int buttonByte) {
            this.valid = valid;
            this.reason = reason;
            this.frame = frame;
            this.joystickX = joystickX;
            this.joystickY = joystickY;
            this.modeByte = modeByte;
            this.triggerByte = triggerByte;
            this.switchByte = switchByte;
            this.buttonByte = buttonByte;
        }

        private static Result valid(byte[] frame,
                                    int joystickX,
                                    int joystickY,
                                    int modeByte,
                                    int triggerByte,
                                    int switchByte,
                                    int buttonByte) {
            return new Result(true, "", frame, joystickX, joystickY, modeByte,
                    triggerByte, switchByte, buttonByte);
        }

        private static Result invalid(String reason) {
            return new Result(false, reason, null, 0, 0, 0, 0, 0, 0);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        public byte[] getFrame() {
            return frame == null ? null : frame.clone();
        }

        /** Signed little-endian joystick X value from payload bytes 0..1. */
        public int getJoystickX() {
            return joystickX;
        }

        /** Signed little-endian joystick Y value from payload bytes 2..3. */
        public int getJoystickY() {
            return joystickY;
        }

        /** Raw payload byte 4. Its observed values include the M/trigger states. */
        public int getModeAndTriggerState() {
            return modeByte;
        }

        /** Alias retaining the evidence terminology used by the capture reports. */
        public int getTriggerState() {
            return triggerByte;
        }

        /** Raw payload byte 5. */
        public int getSwitchState() {
            return switchByte;
        }

        /** Raw payload byte 6, including side-wheel press and Record bits. */
        public int getButtonState() {
            return buttonByte;
        }

        public boolean isMButtonState() {
            return valid && modeByte == M_BUTTON_STATE;
        }

        public boolean isObservedTriggerState() {
            return valid && (triggerByte == TRIGGER_STATE_SINGLE
                    || triggerByte == TRIGGER_STATE_DOUBLE
                    || triggerByte == TRIGGER_STATE_61
                    || triggerByte == TRIGGER_STATE_71);
        }

        public boolean isSwitchSingleState() {
            return valid && switchByte == SWITCH_STATE_SINGLE;
        }

        public boolean isSwitchDoubleState() {
            return valid && switchByte == SWITCH_STATE_DOUBLE;
        }

        public boolean isSideWheelPressState() {
            return valid && (buttonByte & SIDE_WHEEL_PRESS_STATE) != 0;
        }

        /**
         * Applies a deadzone to the measured joystick axes. Callers should use
         * this for state transitions; exact observed magnitudes are not an API.
         */
        public boolean isJoystickNeutral(int deadzone) {
            if (!valid || deadzone < 0) {
                return false;
            }
            return Math.abs((long) joystickX) <= deadzone
                    && Math.abs((long) joystickY) <= deadzone;
        }

        public int getRecordByte() {
            return buttonByte;
        }

        public boolean isRecordButtonDown() {
            return valid && (buttonByte & RECORD_BIT) != 0;
        }
    }
}
