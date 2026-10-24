package com.androiddjibridge.core;

/** Pure auto-connect, association, and bounded-retry regression tests. */
public final class AutoConnectPolicySelfTest {
    public static void main(String[] args) {
        require(AutoConnectPolicy.decide(
                        false, true, GimbalDriver.ConnectionState.STOPPED)
                == AutoConnectPolicy.Decision.NO_ASSOCIATION,
                "no association must not start a connection");
        require(AutoConnectPolicy.decide(
                        true, false, GimbalDriver.ConnectionState.DISCONNECTED)
                == AutoConnectPolicy.Decision.BLUETOOTH_UNAVAILABLE,
                "Bluetooth off must not start a connection");
        require(AutoConnectPolicy.decide(
                        true, true, GimbalDriver.ConnectionState.DISCONNECTED)
                == AutoConnectPolicy.Decision.START,
                "associated available device permits start");
        require(AutoConnectPolicy.decide(
                        true, true, GimbalDriver.ConnectionState.CONNECTING)
                == AutoConnectPolicy.Decision.ALREADY_ACTIVE,
                "duplicate presence must not create a second owner");
        require(AutoConnectPolicy.decide(
                        true, true, GimbalDriver.ConnectionState.READY)
                == AutoConnectPolicy.Decision.ALREADY_ACTIVE,
                "ready driver is already active");

        require(ReconnectBackoff.delayForAttempt(0) == 0L, "attempt zero has no delay");
        require(ReconnectBackoff.delayForAttempt(1) == 1_000L, "first retry delay");
        require(ReconnectBackoff.delayForAttempt(2) == 2_000L, "second retry delay");
        require(ReconnectBackoff.delayForAttempt(6) == 32_000L
                        || ReconnectBackoff.delayForAttempt(6) == 30_000L,
                "bounded retry schedule");

        AssociatedDevice device = new AssociatedDevice(
                AssociatedDevice.CURRENT_SCHEMA_VERSION,
                "OM8-test",
                "AA:BB:CC:DD:EE:FF",
                42L);
        AssociatedDevice observed = device.withObservation("", -1L);
        require(observed.getAssociationId() == 42L, "association id persists");
        require(observed.getDisplayName().equals("OM8-test"), "device name persists");
        require(observed.getLastKnownAddress().equals("AA:BB:CC:DD:EE:FF"),
                "empty observation address preserves last known address");
        expectFailure(() -> new AssociatedDevice(
                        1, "Not-a-camera", "", AssociatedDevice.NO_ASSOCIATION_ID),
                "non-OM8 identity rejected");

        System.out.println("AutoConnectPolicySelfTest: PASS");
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
