package com.androiddjibridge.sony;

import com.androiddjibridge.core.GenericAccessibilityDecision;

/** Fail-closed generic learned-profile action decision regression. */
public final class GenericAccessibilityDecisionSelfTest {
    public static void main(String[] args) {
        require(GenericAccessibilityDecision.canAttemptClick(
                        "com.example.camera", "com.example.camera", true, false, 1),
                "unique exact target is eligible");
        require(!GenericAccessibilityDecision.canAttemptClick(
                        "com.example.camera", "com.example.camera", true, false, 0),
                "zero matches rejected");
        require(!GenericAccessibilityDecision.canAttemptClick(
                        "com.example.camera", "com.example.camera", true, false, 2),
                "ambiguous matches rejected");
        require(!GenericAccessibilityDecision.canAttemptClick(
                        "com.example.camera", "com.other.camera", true, false, 1),
                "wrong foreground rejected");
        require(!GenericAccessibilityDecision.canAttemptClick(
                        "com.example.camera", "com.example.camera", false, false, 1),
                "non-interactive rejected");
        require(!GenericAccessibilityDecision.canAttemptClick(
                        "com.example.camera", "com.example.camera", true, true, 1),
                "keyguard rejected");
        System.out.println("GenericAccessibilityDecisionSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
