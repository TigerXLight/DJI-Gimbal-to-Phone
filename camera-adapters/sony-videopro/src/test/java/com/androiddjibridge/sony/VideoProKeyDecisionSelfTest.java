package com.androiddjibridge.sony;

public final class VideoProKeyDecisionSelfTest {
    private static final String VIDEO_PRO = "jp.co.sony.mc.videopro";

    public static void main(String[] args) {
        require(VideoProKeyDecision.isValid(24, 0, 0, VIDEO_PRO, VIDEO_PRO), "volume up valid");
        require(VideoProKeyDecision.isValid(25, 0, 0, VIDEO_PRO, VIDEO_PRO), "volume down valid");
        require(!VideoProKeyDecision.isValid(24, 1, 0, VIDEO_PRO, VIDEO_PRO), "up rejected");
        require(!VideoProKeyDecision.isValid(25, 1, 0, VIDEO_PRO, VIDEO_PRO), "down rejected");
        require(!VideoProKeyDecision.isValid(24, 0, 1, VIDEO_PRO, VIDEO_PRO), "repeat rejected");
        require(!VideoProKeyDecision.isValid(25, 0, 0, VIDEO_PRO, "com.example.other"), "other app rejected");
        require(VideoProKeyDecision.isValid(24, 0, 0, VIDEO_PRO, VIDEO_PRO, true, false), "unlocked valid");
        require(!VideoProKeyDecision.isValid(24, 0, 0, VIDEO_PRO, VIDEO_PRO, false, false), "screen off rejected");
        require(!VideoProKeyDecision.isValid(24, 0, 0, VIDEO_PRO, VIDEO_PRO, true, true), "keyguard rejected");
        System.out.println("VideoProKeyDecisionSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
