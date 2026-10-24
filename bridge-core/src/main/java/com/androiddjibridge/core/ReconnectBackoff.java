package com.androiddjibridge.core;

/** Bounded exponential retry schedule shared by lifecycle policy tests. */
public final class ReconnectBackoff {
    public static final long BASE_DELAY_MS = 1_000L;
    public static final long MAX_DELAY_MS = 30_000L;
    private static final int MAX_EXPONENT = 5;

    private ReconnectBackoff() {
    }

    public static long delayForAttempt(int attempt) {
        if (attempt <= 0) {
            return 0L;
        }
        int exponent = Math.min(attempt - 1, MAX_EXPONENT);
        return Math.min(BASE_DELAY_MS << exponent, MAX_DELAY_MS);
    }
}
