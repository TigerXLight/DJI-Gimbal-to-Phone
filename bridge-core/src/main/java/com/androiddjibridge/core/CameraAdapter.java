package com.androiddjibridge.core;

/** Application-specific output boundary for normalized controller events. */
public interface CameraAdapter {
    String id();

    String displayName();

    /**
     * Returns the immutable profile this adapter executes, when it has one.
     * A null value is allowed for test adapters and future non-profile adapters.
     */
    default CameraProfile profile() {
        return null;
    }

    boolean isAvailable();

    void prepare();

    /**
     * Whether this adapter can execute a semantic camera action. The default
     * preserves compatibility for profile-backed record adapters while making
     * unsupported actions fail closed.
     */
    default boolean supports(CameraAction action) {
        return action == CameraAction.RECORD_TOGGLE
                && profile() != null
                && profile().supports(CameraCapability.RECORD_TOGGLE);
    }

    /**
     * Executes a semantic action with the originating event available for
     * diagnostics. Implementations must not infer a different action.
     */
    default boolean execute(CameraAction action, ControllerEvent sourceEvent) {
        if (action != CameraAction.RECORD_TOGGLE
                || sourceEvent == null
                || sourceEvent.getType() != ControllerEvent.Type.RECORD_DOWN) {
            return false;
        }
        return handleControllerEvent(sourceEvent);
    }

    /** Legacy event entry point retained for adapter-level regression tests. */
    boolean handleControllerEvent(ControllerEvent event);

    CameraAdapterState getState();

    void release();
}
