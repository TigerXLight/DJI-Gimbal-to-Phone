package com.androiddjibridge.core;

/** Process-local registry connecting the driver layer to the selected adapter. */
public final class BridgeRuntime {
    private static volatile BridgeController controller;
    private static volatile ForegroundPackageListener foregroundPackageListener;

    private BridgeRuntime() {
    }

    public static void install(BridgeController bridgeController) {
        controller = bridgeController;
    }

    public static BridgeController controller() {
        return controller;
    }

    public static boolean dispatch(ControllerEvent event) {
        BridgeController current = controller;
        return current != null && current.onControllerEvent(event);
    }

    public static void installForegroundPackageListener(ForegroundPackageListener listener) {
        foregroundPackageListener = listener;
    }

    public static void foregroundPackageChanged(String packageName) {
        ForegroundPackageListener listener = foregroundPackageListener;
        if (listener != null) {
            listener.onForegroundPackageChanged(packageName == null ? "" : packageName);
        }
    }
}
