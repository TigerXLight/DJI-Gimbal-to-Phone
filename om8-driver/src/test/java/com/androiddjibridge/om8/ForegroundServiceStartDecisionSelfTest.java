package com.androiddjibridge.om8;

public final class ForegroundServiceStartDecisionSelfTest {
    public static void main(String[] args) {
        require(ForegroundServiceStartDecision.evaluate(31, true, true, true)
                == ForegroundServiceStartDecision.Result.ALLOW, "API 31 allow");
        require(ForegroundServiceStartDecision.evaluate(31, false, true, true)
                == ForegroundServiceStartDecision.Result.BLUETOOTH_RUNTIME_PERMISSION_REQUIRED,
                "missing scan blocks");
        require(ForegroundServiceStartDecision.evaluate(34, true, true, false)
                == ForegroundServiceStartDecision.Result.CONNECTED_DEVICE_FGS_PERMISSION_REQUIRED,
                "missing FGS permission blocks");
        require(ForegroundServiceStartDecision.evaluate(30, false, false, false)
                == ForegroundServiceStartDecision.Result.ALLOW, "pre-31 compatibility");
        System.out.println("ForegroundServiceStartDecisionSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
