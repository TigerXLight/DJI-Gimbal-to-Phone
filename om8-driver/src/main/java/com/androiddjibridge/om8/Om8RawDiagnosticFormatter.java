package com.androiddjibridge.om8;

import java.util.Locale;

/** Deterministic, complete-byte formatting for explicit OM8 FFF4 capture mode. */
public final class Om8RawDiagnosticFormatter {
    private static final int SENDER_OFFSET = 4;
    private static final int RECEIVER_OFFSET = 5;
    private static final int FLAGS_OFFSET = 8;
    private static final int COMMAND_SET_OFFSET = 9;
    private static final int COMMAND_ID_OFFSET = 10;
    private static final int PAYLOAD_OFFSET = 11;
    private static final int CRC_LENGTH = 2;

    private Om8RawDiagnosticFormatter() {
    }

    public static String raw(long timestampNanos, String characteristic, byte[] value) {
        String name = characteristic == null ? "unknown" : characteristic;
        int length = value == null ? 0 : value.length;
        return "OM8_RAW_FFF4 timestampNanos=" + timestampNanos
                + " characteristic=" + name
                + " length=" + length
                + " value=" + toHex(value);
    }

    public static String parsed(Om8DumlStatusParser.Result result) {
        if (result == null || !result.isValid()) {
            return "OM8_PARSED_FFF4 valid=false";
        }
        byte[] frame = result.getFrame();
        if (frame == null || frame.length <= PAYLOAD_OFFSET + CRC_LENGTH) {
            return "OM8_PARSED_FFF4 valid=false reason=missing-frame";
        }
        return String.format(
                Locale.US,
                "OM8_PARSED_FFF4 valid=true frameLength=%d sender=0x%02X "
                        + "receiver=0x%02X flags=0x%02X cmdSet=0x%02X cmdId=0x%02X payload=%s",
                frame.length,
                frame[SENDER_OFFSET] & 0xFF,
                frame[RECEIVER_OFFSET] & 0xFF,
                frame[FLAGS_OFFSET] & 0xFF,
                frame[COMMAND_SET_OFFSET] & 0xFF,
                frame[COMMAND_ID_OFFSET] & 0xFF,
                toHex(frame, PAYLOAD_OFFSET, frame.length - CRC_LENGTH)
                        + String.format(
                        Locale.US,
                        " joystickX=%d joystickY=%d modeTrigger=0x%02X switch=0x%02X "
                                + "buttons=0x%02X recordDown=%s wheelPress=%s",
                        result.getJoystickX(),
                        result.getJoystickY(),
                        result.getModeAndTriggerState(),
                        result.getSwitchState(),
                        result.getButtonState(),
                        result.isRecordButtonDown(),
                        result.isSideWheelPressState()));
    }

    public static String toHex(byte[] value) {
        return value == null ? "<null>" : toHex(value, 0, value.length);
    }

    private static String toHex(byte[] value, int start, int endExclusive) {
        if (value == null || start >= endExclusive) {
            return "";
        }
        StringBuilder builder = new StringBuilder((endExclusive - start) * 3);
        for (int index = start; index < endExclusive; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(String.format(Locale.US, "%02X", value[index] & 0xFF));
        }
        return builder.toString();
    }
}
