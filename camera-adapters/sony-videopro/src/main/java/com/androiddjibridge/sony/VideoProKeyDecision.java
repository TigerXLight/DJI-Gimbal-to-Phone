package com.androiddjibridge.sony;

/**
 * Pure decision logic for the log-only Sony Video Pro key observer.
 *
 * Android key-code values are kept here so this helper can be regression-tested
 * without an Android runtime. It deliberately has no state, so two separate
 * non-repeating ACTION_DOWN events are never deduplicated.
 */
public final class VideoProKeyDecision {
    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int KEYCODE_VOLUME_DOWN = 25;
    public static final int ACTION_DOWN = 0;
    public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";

    private VideoProKeyDecision() {
    }

    public static boolean isVolumeKey(int keyCode) {
        return keyCode == KEYCODE_VOLUME_UP || keyCode == KEYCODE_VOLUME_DOWN;
    }

    public static boolean isVideoProForeground(String configuredPackage,
                                                String foregroundPackage) {
        if (configuredPackage == null || configuredPackage.trim().isEmpty()
                || foregroundPackage == null) {
            return false;
        }
        return configuredPackage.trim().equals(foregroundPackage.trim());
    }

    /**
     * Resolves an accessibility event into the effective foreground
     * application. System UI is an overlay signal, not an application switch.
     */
    public static String resolveEffectiveForegroundPackage(
            String currentEffectivePackage,
            String eventPackage,
            String applicationWindowPackage,
            boolean interactive,
            boolean keyguardLocked) {
        String current = normalize(currentEffectivePackage);
        String event = normalize(eventPackage);
        String applicationWindow = normalize(applicationWindowPackage);

        if (!interactive || keyguardLocked) {
            return "";
        }
        if (!event.isEmpty() && !isSystemUiPackage(event)) {
            return event;
        }
        if (!applicationWindow.isEmpty() && !isSystemUiPackage(applicationWindow)) {
            return applicationWindow;
        }
        return current;
    }

    public static boolean isSystemUiPackage(String packageName) {
        String normalized = normalize(packageName);
        return SYSTEM_UI_PACKAGE.equals(normalized) || "android".equals(normalized);
    }

    public static boolean isValid(int keyCode,
                                  int action,
                                  int repeatCount,
                                  String configuredPackage,
                                  String foregroundPackage) {
        return isValid(keyCode, action, repeatCount, configuredPackage,
                foregroundPackage, true, false);
    }

    public static boolean isValid(int keyCode,
                                  int action,
                                  int repeatCount,
                                  String configuredPackage,
                                  String foregroundPackage,
                                  boolean interactive,
                                  boolean keyguardLocked) {
        return isVolumeKey(keyCode)
                && action == ACTION_DOWN
                && repeatCount == 0
                && interactive
                && !keyguardLocked
                && isVideoProForeground(configuredPackage, foregroundPackage);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
