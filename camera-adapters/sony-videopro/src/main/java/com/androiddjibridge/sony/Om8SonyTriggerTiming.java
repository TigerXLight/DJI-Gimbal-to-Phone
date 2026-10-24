package com.androiddjibridge.sony;

/**
 * Monotonic timestamps carried across the in-process OM8-to-Sony bridge.
 * Values remain unset (-1) when a safety rejection happens before that stage.
 */
public final class Om8SonyTriggerTiming {
    /** Diagnostic-only identity for one OM8 rising edge and its Sony outcome. */
    public long om8PressSequence = -1L;
    public long tBleCallbackEnterNanos = -1L;
    public long tDumlParseStartNanos = -1L;
    public long tDumlParseEndNanos = -1L;
    public long tEdgeDetectStartNanos = -1L;
    public long tEdgeDetectEndNanos = -1L;
    public long tBridgeRequestStartNanos = -1L;
    public long tBridgeRequestEndNanos = -1L;
    public long tSonyTriggerRequestedNanos = -1L;

    public long tSonyBridgeReceivedNanos = -1L;
    public long tSonyMainDispatchRequestedNanos = -1L;
    public long tSonyMainDispatchBeginNanos = -1L;
    public boolean gattCallbackOnMainLooper;
    public long gattCallbackThreadId = -1L;
    public boolean sonyBridgeAlreadyOnMainLooper;
    public boolean mainDispatchSkipped;

    public boolean cachedNodeAvailable;
    public boolean cachedNodeRefreshResult;
    public boolean cachedNodeFastPathUsed;
    public boolean freshRootFallbackUsed;
    public long tCachedNodeRefreshStartNanos = -1L;
    public long tCachedNodeRefreshEndNanos = -1L;

    public boolean preparedNodeAvailable;
    public boolean preparedNodeFastPathUsed;
    public boolean preparedNodeGenerationValid;
    public boolean preparedNodeWindowValid;
    public long preparedNodeAgeNanos = -1L;
    public boolean safetySnapshotAvailable;
    public boolean safetySnapshotValid;
    public boolean safetySnapshotFastPathUsed;
    public boolean safetySnapshotGenerationValid;
    public boolean safetySnapshotWindowValid;
    public long safetySnapshotAgeNanos = -1L;
    public long tSafetySnapshotCheckStartNanos = -1L;
    public long tSafetySnapshotCheckEndNanos = -1L;
    public boolean interactiveCheckPerformed;
    public boolean keyguardCheckPerformed;
    public boolean foregroundPackageCheckPerformed;
    public boolean preClickNodeRefreshPerformed;
    public long preClickNodeRefreshNanos;
    public boolean preparedNodeActionResult;
    public boolean postClickRearmAttempted;
    public boolean postClickRearmResult;
    public int postClickRearmAttempt;
    public String postClickRearmFailureReason = "";
    public long tPostClickRearmRequestNanos = -1L;
    public long tPostClickRearmBeginNanos = -1L;
    public long tPostClickRearmReadyNanos = -1L;
    public long tPostClickRearmStartNanos = -1L;
    public long tPostClickRearmEndNanos = -1L;

    public long tForegroundCheckStartNanos = -1L;
    public long tForegroundCheckEndNanos = -1L;
    public long tInteractiveCheckStartNanos = -1L;
    public long tInteractiveCheckEndNanos = -1L;
    public long tKeyguardCheckStartNanos = -1L;
    public long tKeyguardCheckEndNanos = -1L;
    public long tForegroundPackageCheckStartNanos = -1L;
    public long tForegroundPackageCheckEndNanos = -1L;
    public long tPreparedGenerationCheckStartNanos = -1L;
    public long tPreparedGenerationCheckEndNanos = -1L;
    public long tPreparedWindowCheckStartNanos = -1L;
    public long tPreparedWindowCheckEndNanos = -1L;
    public long tRootQueryStartNanos = -1L;
    public long tRootQueryEndNanos = -1L;
    public long tRecordingNodeQueryStartNanos = -1L;
    public long tRecordingNodeQueryEndNanos = -1L;
    /** Legacy aliases retained for timing-log compatibility; they now refer to post-click lookup. */
    public long tRecStatusQueryStartNanos = -1L;
    public long tRecStatusQueryEndNanos = -1L;
    public long tPostClickRecStatusQueryStartNanos = -1L;
    public long tPostClickRecStatusQueryEndNanos = -1L;
    public long tNodeValidationStartNanos = -1L;
    public long tNodeValidationEndNanos = -1L;
    public long tPerformActionStartNanos = -1L;
    public long tPerformActionEndNanos = -1L;
    public long tSonyVerifiedStateNanos = -1L;
}
