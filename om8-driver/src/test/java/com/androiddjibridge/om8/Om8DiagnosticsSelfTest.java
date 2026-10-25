package com.androiddjibridge.om8;

/** Small JVM checks for raw preservation and normal-mode logging policy. */
public final class Om8DiagnosticsSelfTest {
    public static void main(String[] args) {
        byte[] bytes = new byte[]{0x55, 0x00, (byte) 0xFF};
        String raw = Om8RawDiagnosticFormatter.raw(123L, "FFF4", bytes);
        require(raw.contains("timestampNanos=123"), "timestamp missing");
        require(raw.contains("length=3"), "length missing");
        require(raw.contains("55 00 FF"), "raw bytes were not preserved");
        require("".equals(Om8RawDiagnosticFormatter.toHex(new byte[0])),
                "empty packet should format safely");
        require(Om8LoggingPolicy.shouldLogHeartbeat(true),
                "raw mode must log heartbeat diagnostics");
        require(!Om8LoggingPolicy.shouldLogHeartbeat(false),
                "normal mode must suppress heartbeat diagnostics");
        require(!Om8LoggingPolicy.shouldLogParserRejection(
                        false, "frame-length-too-short=18"),
                "routine rejection should be quiet");
        require(Om8LoggingPolicy.shouldLogParserRejection(false, "bad-marker"),
                "unusual rejection should remain visible");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
