package com.androiddjibridge.om8;

/** Keeps expected idle traffic quiet while retaining unusual parser failures. */
public final class Om8LoggingPolicy {
    private Om8LoggingPolicy() {
    }

    public static boolean shouldLogHeartbeat(boolean rawDiagnosticsEnabled) {
        return rawDiagnosticsEnabled;
    }

    public static boolean shouldLogParserRejection(
            boolean rawDiagnosticsEnabled, String reason) {
        return rawDiagnosticsEnabled || !isRoutineParserRejection(reason);
    }

    public static boolean isRoutineParserRejection(String reason) {
        if (reason == null) {
            return false;
        }
        return reason.startsWith("frame-length-too-short=")
                || reason.startsWith("unexpected-sender")
                || reason.startsWith("unexpected-receiver")
                || reason.startsWith("unexpected-flags")
                || reason.startsWith("unexpected-command-set")
                || reason.startsWith("unexpected-command-id");
    }
}
