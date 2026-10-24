package com.androiddjibridge.om8;

import java.util.Arrays;

/**
 * Minimal, receive-only parser for the OM8 DUML status frame observed on FFF4.
 * It validates the frame structure and routing before exposing the record bit.
 */
public final class Om8DumlStatusParser {
    public static final int FRAME_MARKER = 0x55;
    public static final int SENDER = 0x27;
    public static final int RECEIVER = 0x02;
    public static final int FLAGS = 0x40;
    public static final int COMMAND_SET = 0x04;
    public static final int COMMAND_ID = 0x57;
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
        int recByte = frame[PAYLOAD_OFFSET + 6] & 0xFF;
        return Result.valid(frame, recByte);
    }

    public static final class Result {
        private final boolean valid;
        private final String reason;
        private final byte[] frame;
        private final int recordByte;

        private Result(boolean valid, String reason, byte[] frame, int recordByte) {
            this.valid = valid;
            this.reason = reason;
            this.frame = frame;
            this.recordByte = recordByte;
        }

        private static Result valid(byte[] frame, int recordByte) {
            return new Result(true, "", frame, recordByte);
        }

        private static Result invalid(String reason) {
            return new Result(false, reason, null, 0);
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

        public int getRecordByte() {
            return recordByte;
        }

        public boolean isRecordButtonDown() {
            return valid && (recordByte & RECORD_BIT) != 0;
        }
    }
}
