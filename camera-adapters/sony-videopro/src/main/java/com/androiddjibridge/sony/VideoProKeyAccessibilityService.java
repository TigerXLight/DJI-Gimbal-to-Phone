package com.androiddjibridge.sony;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.AccessibilityTargetMatcher;
import com.androiddjibridge.core.CameraAccessibilityAction;
import com.androiddjibridge.core.CameraProfileCandidate;
import com.androiddjibridge.core.CameraProfileLearningSession;
import com.androiddjibridge.core.CameraProfileOrigin;
import com.androiddjibridge.core.ControllerEvent;
import com.androiddjibridge.core.GenericAccessibilityDecision;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Observes the Xperia volume key without consuming it. Video Pro therefore
 * remains responsible for its own REC/STOP action.
 */
public class VideoProKeyAccessibilityService extends AccessibilityService {
    private static final String TAG = "VideoProKeyAccessibilityService";
    public static final String ACTION_TEST_SONY_REC_CLICK =
            "com.androiddjibridge.TEST_SONY_REC_CLICK";
    public static final String ACTION_TEST_PHOTOPRO_CAMERA_BUTTON_PROBE =
            "com.androiddjibridge.TEST_PHOTOPRO_CAMERA_BUTTON_PROBE";

    private static final CameraProfile CAMERA_PROFILE = SonyVideoProProfile.builtIn();
    private static final String VIDEO_PRO_PACKAGE = CAMERA_PROFILE.getPackageName();
    private static final String PHOTOPRO_PACKAGE =
            PhotographyProCameraButtonProbeDecision.PACKAGE_NAME;
    private static final String RECORDING_NODE_ID =
            CAMERA_PROFILE.getRecordButtonResourceId();
    private static final String REC_STATUS_NODE_ID = CAMERA_PROFILE.getStateResourceId();
    private static final String DURATION_NODE_ID = CAMERA_PROFILE.getDurationResourceId();
    private static final String REC_NODE_CLASS =
            CAMERA_PROFILE.getRecordButtonClassName();
    private static final long POST_CLICK_CHECK_ONE_MS = 250L;
    private static final long POST_CLICK_CHECK_TWO_MS = 750L;
    private static final long PREPARE_DEBOUNCE_MS = 50L;
    private static final long[] GENERAL_PREPARE_RETRY_DELAYS_MS = {0L, 40L, 100L};
    /**
     * Sony leaves the recording button disabled briefly during a state transition.
     * These are delays after the preceding attempt, giving approximately 50, 200,
     * 500, 1000, and 1500 ms from the verified-state re-arm request.
     */
    private static final long[] TRANSIENT_PREPARE_RETRY_DELAYS_MS =
            {0L, 150L, 300L, 500L, 500L};
    private static final int GENERAL_MAX_PREPARE_ATTEMPTS =
            GENERAL_PREPARE_RETRY_DELAYS_MS.length;
    private static final int TRANSIENT_MAX_PREPARE_ATTEMPTS =
            TRANSIENT_PREPARE_RETRY_DELAYS_MS.length;
    private static final long EVENT_REARM_DEBOUNCE_MS = 75L;
    private static final int UNKNOWN_WINDOW_ID = -1;

    private static VideoProKeyAccessibilityService activeInstance;

    private final CameraProfileLearningSession learningSession =
            new CameraProfileLearningSession();

    private String lastAccessibilityEventPackage = "";
    private String effectiveForegroundPackage = "";
    private int currentSonyWindowId = UNKNOWN_WINDOW_ID;
    private long foregroundGeneration;
    private long structuralGeneration;
    private AccessibilityNodeInfo cachedRecNode;
    private int cachedRecWindowId = UNKNOWN_WINDOW_ID;
    private AccessibilityNodeInfo preparedRecNode;
    private int preparedWindowId = UNKNOWN_WINDOW_ID;
    private long preparedForegroundGeneration = -1L;
    private long preparedStructuralGeneration = -1L;
    private long preparedAtElapsedRealtimeNanos = -1L;
    private boolean preparationScheduled;
    private Om8SonyTriggerTiming pendingPreparationTiming;
    private Runnable scheduledPreparationRunnable;
    private int preparationCycle;
    private int scheduledPreparationAttempt;
    private boolean preparationExtendedRecovery;
    private String lastRearmFailureReason = "";
    private long lastRearmFailureNanos = -1L;
    private BridgeDiagnosticLogger logger;
    private final Handler diagnosticHandler = new Handler(Looper.getMainLooper());

    /**
     * Fail-closed safety state prepared on the service main looper. The OM8
     * request is also dispatched to this looper, while volatile visibility
     * ensures an invalidation is observed if a lifecycle callback races the
     * request before dispatch.
     */
    private volatile boolean safetySnapshotValid;
    private volatile boolean snapshotInteractive;
    private volatile boolean snapshotKeyguardUnlocked;
    private volatile boolean snapshotSonyForeground;
    private volatile long snapshotForegroundGeneration = -1L;
    private volatile long snapshotStructuralGeneration = -1L;
    private volatile int snapshotPreparedWindowId = UNKNOWN_WINDOW_ID;
    private volatile long snapshotValidatedAtNanos = -1L;
    private volatile String snapshotConfiguredPackage = "";
    private boolean safetyEventReceiverRegistered;

    private final BroadcastReceiver safetyEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                log("SAFETY_SNAPSHOT invalidated reason=screen-off");
                invalidateAccessibilityNodeCaches();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                log("SAFETY_SNAPSHOT invalidated reason=screen-on-awaiting-user-present");
                invalidateSafetySnapshot();
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                log("SAFETY_SNAPSHOT user-present revalidation-scheduled");
                invalidateSafetySnapshot();
                scheduleSafetySnapshotRevalidation();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        activeInstance = this;
        invalidateAccessibilityNodeCaches();
        logger = new BridgeDiagnosticLogger(this);
        registerSafetyEventReceiver();
        log("service connected; volume sync is validation log-only");

        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            log("serviceInfo=null");
            return;
        }

        int requiredFlags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        if ((serviceInfo.flags & requiredFlags) != requiredFlags) {
            // The XML is the source of truth. Preserve every existing runtime
            // flag while adding only the flags this tracker needs.
            serviceInfo.flags |= requiredFlags;
            setServiceInfo(serviceInfo);
            AccessibilityServiceInfo refreshed = getServiceInfo();
            if (refreshed != null) {
                serviceInfo = refreshed;
            }
            log("runtime accessibility flags were incomplete; requested key filtering, interactive windows, and view IDs without replacing other flags");
        }

        int capabilities = serviceInfo.getCapabilities();
        boolean hasKeyFilterFlag = (serviceInfo.flags
                & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0;
        boolean hasInteractiveWindowsFlag = (serviceInfo.flags
                & AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS) != 0;
        boolean hasReportViewIdsFlag = (serviceInfo.flags
                & AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS) != 0;
        boolean canRequestKeyFilter =
                (capabilities & AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS) != 0;
        log("serviceInfo.flags=" + serviceInfo.flags
                + " flagRequestFilterKeyEvents=" + hasKeyFilterFlag
                + " flagRetrieveInteractiveWindows=" + hasInteractiveWindowsFlag
                + " flagReportViewIds=" + hasReportViewIdsFlag
                + " capabilities=" + capabilities
                + " capabilityCanRequestFilterKeyEvents=" + canRequestKeyFilter
                + " packageNames=" + (serviceInfo.packageNames == null
                ? "<all>"
                : Arrays.toString(serviceInfo.packageNames))
                + " eventTypes=" + serviceInfo.eventTypes);
    }

    /** Called only by the debug-only explicit ADB diagnostic receiver. */
    public static void requestDiagnosticRecClick() {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null) {
            Log.i(TAG, "diagnostic request received but accessibility service is not connected");
            return;
        }
        instance.diagnosticHandler.post(() -> instance.performSonyRecAction(null));
    }

    /**
     * Queues the temporary, package-scoped Photography Pro CAMERA_BUTTON probe.
     * The live service performs the foreground and device-safety checks on its
     * own main looper before constructing either broadcast.
     */
    public static boolean requestPhotographyProCameraButtonProbe() {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null) {
            Log.i(TAG, "PHOTOPRO_CAMERA_BUTTON_PROBE rejected reason=accessibility-service-not-connected");
            return false;
        }
        instance.diagnosticHandler.post(instance::performPhotographyProCameraButtonProbe);
        return true;
    }

    public static boolean isConnected() {
        return activeInstance != null;
    }

    public static boolean startLearning(String packageName) {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null) {
            return false;
        }
        boolean started = instance.learningSession.start(packageName);
        instance.log(started
                ? "LEARN_MODE started targetPackage=" + packageName
                : "LEARN_MODE rejected invalid-target-package");
        return started;
    }

    public static void cancelLearning() {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance != null) {
            instance.learningSession.cancel();
            instance.log("LEARN_MODE cancelled");
        }
    }

    public static CameraProfileLearningSession.Snapshot getLearningSnapshot() {
        VideoProKeyAccessibilityService instance = activeInstance;
        return instance == null
                ? new CameraProfileLearningSession().snapshot()
                : instance.learningSession.snapshot();
    }

    public static boolean confirmLearningCandidate() {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null) {
            return false;
        }
        boolean confirmed = instance.learningSession.confirmCandidate();
        instance.log(confirmed
                ? "LEARN_MODE candidate confirmed"
                : "LEARN_MODE confirmation rejected");
        return confirmed;
    }

    public static CameraProfile createConfirmedLearnedProfile(String displayName) {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null) {
            return null;
        }
        try {
            return instance.learningSession.createConfirmedProfile(displayName);
        } catch (IllegalStateException error) {
            instance.log("LEARN_MODE profile creation rejected reason=not-confirmed");
            return null;
        }
    }

    public static boolean isLearningMode() {
        VideoProKeyAccessibilityService instance = activeInstance;
        return instance != null && instance.learningSession.isActive();
    }

    /** Deliberate in-app test path for a saved LEARNED profile; never an exported command. */
    public static boolean requestGenericProfileTest(CameraProfile profile) {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null || profile == null) {
            return false;
        }
        instance.diagnosticHandler.post(() -> instance.performGenericProfileAction(profile));
        return true;
    }

    public static boolean requestGenericProfileAction(CameraProfile profile) {
        return requestGenericProfileTest(profile);
    }

    /**
     * In-process bridge from the receive-only OM8 listener. The AccessibilityService
     * remains the only owner of the Sony node lookup, safety checks, and click.
     *
     * @return true when the request was queued for the connected service
     */
    public static boolean requestSonyRecActionFromController(Om8SonyTriggerTiming timing) {
        VideoProKeyAccessibilityService instance = activeInstance;
        if (instance == null) {
            return false;
        }
        timing.tSonyBridgeReceivedNanos = SystemClock.elapsedRealtimeNanos();
        timing.tSonyMainDispatchRequestedNanos = SystemClock.elapsedRealtimeNanos();
        boolean alreadyOnMainLooper = Looper.myLooper() == instance.diagnosticHandler.getLooper();
        timing.sonyBridgeAlreadyOnMainLooper = alreadyOnMainLooper;
        timing.mainDispatchSkipped = alreadyOnMainLooper;
        Runnable action = () -> {
            timing.tSonyMainDispatchBeginNanos = SystemClock.elapsedRealtimeNanos();
            instance.performSonyRecAction(timing);
        };
        if (alreadyOnMainLooper) {
            action.run();
        } else {
            instance.diagnosticHandler.post(action);
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        int type = event.getEventType();
        captureLearningCandidate(event);
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            CharSequence packageName = event.getPackageName();
            lastAccessibilityEventPackage = packageName == null
                    ? ""
                    : packageName.toString().trim();
            boolean interactive = isDeviceInteractive();
            boolean keyguardLocked = isKeyguardLocked();
            ApplicationWindowSnapshot applicationWindow = findApplicationWindowSnapshot();
            String applicationWindowPackage = applicationWindow.packageName;
            String previousEffectivePackage = effectiveForegroundPackage;
            int previousSonyWindowId = currentSonyWindowId;
            effectiveForegroundPackage = VideoProKeyDecision.resolveEffectiveForegroundPackage(
                    effectiveForegroundPackage,
                    lastAccessibilityEventPackage,
                    applicationWindowPackage,
                    interactive,
                    keyguardLocked);
            log("accessibility event eventPackage=" + lastAccessibilityEventPackage
                    + " applicationWindowPackage=" + applicationWindowPackage
                    + " applicationWindowId=" + applicationWindow.windowId
                    + " effectiveForegroundPackage=" + effectiveForegroundPackage
                    + " interactive=" + interactive
                    + " keyguardLocked=" + keyguardLocked);
            if (!previousEffectivePackage.equals(effectiveForegroundPackage)) {
                log("foreground package updated=" + effectiveForegroundPackage
                        + " from eventPackage=" + lastAccessibilityEventPackage);
                com.androiddjibridge.core.BridgeRuntime.foregroundPackageChanged(
                        effectiveForegroundPackage);
            }
            boolean videoProActive = VIDEO_PRO_PACKAGE.equals(effectiveForegroundPackage);
            int observedSonyWindowId = applicationWindow.windowId;
            if (observedSonyWindowId == UNKNOWN_WINDOW_ID
                    && VIDEO_PRO_PACKAGE.equals(lastAccessibilityEventPackage)) {
                observedSonyWindowId = event.getWindowId();
            }
            boolean sonyWindowChanged = videoProActive
                    && observedSonyWindowId != UNKNOWN_WINDOW_ID
                    && previousSonyWindowId != UNKNOWN_WINDOW_ID
                    && observedSonyWindowId != previousSonyWindowId;
            currentSonyWindowId = videoProActive
                    ? observedSonyWindowId
                    : UNKNOWN_WINDOW_ID;
            boolean relevantStructuralEvent = videoProActive
                    && ((type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && VIDEO_PRO_PACKAGE.equals(lastAccessibilityEventPackage))
                    || (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                    && (VIDEO_PRO_PACKAGE.equals(lastAccessibilityEventPackage)
                    || VIDEO_PRO_PACKAGE.equals(applicationWindowPackage))));
            boolean foregroundChanged = !previousEffectivePackage.equals(effectiveForegroundPackage);
            boolean eventAssistedRearm = videoProActive
                    && interactive
                    && !keyguardLocked
                    && relevantStructuralEvent
                    && !foregroundChanged
                    && !sonyWindowChanged
                    && preparedRecNode == null
                    && pendingPreparationTiming != null
                    && "enabled".equals(lastRearmFailureReason);
            boolean structuralInvalidation = !interactive
                    || keyguardLocked
                    || foregroundChanged
                    || sonyWindowChanged
                    || (videoProActive && relevantStructuralEvent && !eventAssistedRearm);
            if (structuralInvalidation) {
                if (foregroundChanged) {
                    foregroundGeneration++;
                }
                structuralGeneration++;
                invalidateAccessibilityNodeCaches();
            }
            if (videoProActive && interactive && !keyguardLocked
                    && (foregroundChanged || sonyWindowChanged || relevantStructuralEvent)) {
                if (eventAssistedRearm) {
                    requestEventAssistedRearmPreparation();
                } else {
                    schedulePreparedRecNodePreparation();
                }
            }
        }
    }

    private void captureLearningCandidate(AccessibilityEvent event) {
        CameraProfileLearningSession.Snapshot snapshot = learningSession.snapshot();
        if (snapshot.getState() != CameraProfileLearningSession.State.WAITING_FOR_CONTROL
                || event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return;
        }
        String targetPackage = snapshot.getSelectedPackage();
        String eventPackage = event.getPackageName() == null
                ? "" : event.getPackageName().toString().trim();
        if (targetPackage == null || !targetPackage.equals(eventPackage)) {
            log("LEARN_CANDIDATE_REJECTED reason=package-scope"
                    + " eventPackage=" + eventPackage
                    + " targetPackage=" + targetPackage);
            return;
        }
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            log("LEARN_CANDIDATE_REJECTED reason=no-source");
            return;
        }
        AccessibilityNodeInfo parent = null;
        try {
            parent = source.getParent();
            EnumSet<CameraAccessibilityAction> actions =
                    EnumSet.noneOf(CameraAccessibilityAction.class);
            for (AccessibilityNodeInfo.AccessibilityAction action : source.getActionList()) {
                if (action != null
                        && action.getId() == AccessibilityNodeInfo.ACTION_CLICK) {
                    actions.add(CameraAccessibilityAction.CLICK);
                }
            }
            CameraProfileCandidate candidate = CameraProfileCandidate.builder()
                    .packageName(eventPackage)
                    .resourceId(value(source.getViewIdResourceName()))
                    .className(value(source.getClassName()))
                    .text(value(source.getText()))
                    .contentDescription(value(source.getContentDescription()))
                    .parentResourceId(parent == null
                            ? null : value(parent.getViewIdResourceName()))
                    .parentClassName(parent == null
                            ? null : value(parent.getClassName()))
                    .supportedActions(actions)
                    .clickable(source.isClickable())
                    .enabled(source.isEnabled())
                    .visibleToUser(source.isVisibleToUser())
                    .windowId(source.getWindowId())
                    .sourceEventType(event.getEventType())
                    .build();
            if (!learningSession.captureCandidate(candidate)) {
                log("LEARN_CANDIDATE_REJECTED reason=not-usable"
                        + " resourceId=" + value(candidate.getResourceId())
                        + " class=" + value(candidate.getClassName())
                        + " clickable=" + candidate.isClickable()
                        + " enabled=" + candidate.isEnabled()
                        + " visible=" + candidate.isVisibleToUser()
                        + " supportsClick=" + candidate.getSupportedActions()
                                .contains(CameraAccessibilityAction.CLICK));
                return;
            }
            log("LEARN_CANDIDATE_CAPTURED package=" + candidate.getPackageName()
                    + " resourceId=" + value(candidate.getResourceId())
                    + " class=" + value(candidate.getClassName())
                    + " text=" + value(candidate.getText())
                    + " contentDescription=" + value(candidate.getContentDescription())
                    + " parentResourceId=" + value(candidate.getParentResourceId())
                    + " actions=" + candidate.getSupportedActions()
                    + " windowId=" + candidate.getWindowId());
        } catch (RuntimeException error) {
            log("LEARN_CANDIDATE_REJECTED reason=metadata-invalid"
                    + " error=" + error.getClass().getSimpleName());
        } finally {
            recycleNode(parent);
            recycleNode(source);
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        logKeyEvent(event);
        if (event == null) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (!VideoProKeyDecision.isVolumeKey(keyCode)) {
            return false;
        }

        String configuredPackage = VIDEO_PRO_PACKAGE;
        String eventPackage = lastAccessibilityEventPackage == null
                ? ""
                : lastAccessibilityEventPackage;
        String activePackage = effectiveForegroundPackage == null
                ? ""
                : effectiveForegroundPackage;
        boolean interactive = isDeviceInteractive();
        boolean keyguardLocked = isKeyguardLocked();
        boolean validDown = event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0;
        boolean videoProActive = VideoProKeyDecision.isVideoProForeground(
                configuredPackage, activePackage);
        boolean valid = VideoProKeyDecision.isValid(
                keyCode,
                event.getAction(),
                event.getRepeatCount(),
                configuredPackage,
                activePackage,
                interactive,
                keyguardLocked);
        log("decision key=" + KeyEvent.keyCodeToString(keyCode)
                + " action=" + event.getAction()
                + " repeatCount=" + event.getRepeatCount()
                + " eventPackage=" + eventPackage
                + " effectiveForegroundPackage=" + activePackage
                + " configuredPackage=" + configuredPackage
                + " interactive=" + interactive
                + " keyguardLocked=" + keyguardLocked
                + " validDown=" + validDown
                + " videoProActive=" + videoProActive
                + " valid=" + valid);

        if (!valid) {
            return false;
        }

        log("VALID_VIDEO_PRO_VOLUME_KEY observed; validation mode sends no recorder command");
        // Deliberately return false. The validation stage never consumes or mirrors the key.
        return false;
    }

    private void log(String message) {
        Log.i(TAG, message);
        if (logger != null) {
            logger.log("ACCESSIBILITY", message);
        }
    }

    private void performGenericProfileAction(CameraProfile profile) {
        if (profile == null || profile.getOrigin() != CameraProfileOrigin.LEARNED) {
            log("GENERIC_PROFILE_TEST_REJECTED reason=not-learned-profile");
            return;
        }
        if (!profile.getRequiredChildResourceIds().isEmpty()) {
            log("GENERIC_PROFILE_TEST_REJECTED reason=unsupported-child-constraints");
            return;
        }
        if (learningSession.isActive()) {
            log("GENERIC_PROFILE_TEST_REJECTED reason=learn-mode-active");
            return;
        }
        boolean interactive = isDeviceInteractive();
        boolean keyguardLocked = isKeyguardLocked();
        String activePackage = effectiveForegroundPackage == null
                ? "" : effectiveForegroundPackage;
        AccessibilityNodeInfo root = null;
        List<AccessibilityNodeInfo> matches = Collections.emptyList();
        try {
            root = getRootInActiveWindow();
            if (root != null) {
                matches = findGenericProfileMatches(root, profile);
            }
            log("GENERIC_PROFILE_TEST package=" + profile.getPackageName()
                    + " activePackage=" + activePackage
                    + " interactive=" + interactive
                    + " keyguardLocked=" + keyguardLocked
                    + " rootAvailable=" + (root != null)
                    + " exactMatches=" + matches.size());
            if (!GenericAccessibilityDecision.canAttemptClick(
                    profile.getPackageName(), activePackage, interactive,
                    keyguardLocked, matches.size())) {
                log("GENERIC_PROFILE_TEST_REJECTED reason=safety-or-match-count");
                return;
            }
            AccessibilityNodeInfo target = matches.get(0);
            boolean result = target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            log("GENERIC_PROFILE_TEST_ACTION_CLICK result=" + result
                    + " package=" + profile.getPackageName());
        } catch (RuntimeException error) {
            log("GENERIC_PROFILE_TEST_REJECTED reason=tree-read-failed"
                    + " error=" + error.getClass().getSimpleName());
        } finally {
            recycleNodes(matches);
            if (root != null && !matches.contains(root)) {
                root.recycle();
            }
        }
    }

    private List<AccessibilityNodeInfo> findGenericProfileMatches(
            AccessibilityNodeInfo root, CameraProfile profile) {
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        if (profile.getRecordButtonResourceId() != null) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(
                    profile.getRecordButtonResourceId());
            if (nodes == null) {
                return matches;
            }
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null && matchesGenericNode(profile, node)) {
                    matches.add(node);
                } else {
                    recycleNode(node);
                }
            }
            return matches;
        }

        ArrayDeque<AccessibilityNodeInfo> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            AccessibilityNodeInfo node = pending.removeFirst();
            int childCount = node.getChildCount();
            for (int index = 0; index < childCount; index++) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) {
                    pending.addLast(child);
                }
            }
            if (matchesGenericNode(profile, node)) {
                matches.add(node);
            } else if (node != root) {
                recycleNode(node);
            }
        }
        return matches;
    }

    private boolean matchesGenericNode(CameraProfile profile, AccessibilityNodeInfo node) {
        if (!node.isClickable() || !node.isEnabled() || !node.isVisibleToUser()) {
            return false;
        }
        AccessibilityNodeInfo parent = null;
        try {
            parent = node.getParent();
            return AccessibilityTargetMatcher.matchesRecordTarget(
                    profile,
                    value(node.getPackageName()),
                    value(node.getViewIdResourceName()),
                    value(node.getClassName()),
                    value(node.getText()),
                    value(node.getContentDescription()),
                    parent == null ? null : value(parent.getViewIdResourceName()),
                    parent == null ? null : value(parent.getClassName()),
                    accessibilityActions(node));
        } finally {
            recycleNode(parent);
        }
    }

    private EnumSet<CameraAccessibilityAction> accessibilityActions(AccessibilityNodeInfo node) {
        EnumSet<CameraAccessibilityAction> actions =
                EnumSet.noneOf(CameraAccessibilityAction.class);
        List<AccessibilityNodeInfo.AccessibilityAction> nodeActions = node.getActionList();
        if (nodeActions != null) {
            for (AccessibilityNodeInfo.AccessibilityAction action : nodeActions) {
                if (action != null && action.getId() == AccessibilityNodeInfo.ACTION_CLICK) {
                    actions.add(CameraAccessibilityAction.CLICK);
                }
            }
        }
        return actions;
    }

    /** Runs on the AccessibilityService main looper; never called by OM8 input. */
    private void performPhotographyProCameraButtonProbe() {
        String activePackage = effectiveForegroundPackage == null
                ? ""
                : effectiveForegroundPackage;
        boolean interactive = isDeviceInteractive();
        boolean keyguardLocked = isKeyguardLocked();
        log("PHOTOPRO_CAMERA_BUTTON_PROBE begin"
                + " effectiveForegroundPackage=" + activePackage
                + " configuredPackage=" + PHOTOPRO_PACKAGE
                + " interactive=" + interactive
                + " keyguardLocked=" + keyguardLocked);

        List<PhotographyProCameraButtonProbeDecision.KeyEventSpec> sequence =
                PhotographyProCameraButtonProbeDecision.createSequence(
                        activePackage, interactive, keyguardLocked);
        if (sequence.isEmpty()) {
            String reason = !interactive
                    ? "device-not-interactive"
                    : keyguardLocked
                    ? "keyguard-locked"
                    : !PHOTOPRO_PACKAGE.equals(activePackage)
                    ? "foreground-package-not-photopro"
                    : "safety-rejected";
            log("PHOTOPRO_CAMERA_BUTTON_PROBE rejected reason=" + reason);
            log("PHOTOPRO_CAMERA_BUTTON_PROBE complete");
            return;
        }

        long downTime = SystemClock.uptimeMillis();
        for (PhotographyProCameraButtonProbeDecision.KeyEventSpec spec : sequence) {
            long eventTime = SystemClock.uptimeMillis();
            KeyEvent event = new KeyEvent(
                    downTime,
                    eventTime,
                    spec.getAction(),
                    spec.getKeyCode(),
                    0);
            sendPhotographyProCameraButtonBroadcast(event);
        }
        log("PHOTOPRO_CAMERA_BUTTON_PROBE complete");
    }

    private void sendPhotographyProCameraButtonBroadcast(KeyEvent event) {
        String actionName = event.getAction() == KeyEvent.ACTION_DOWN
                ? "ACTION_DOWN" : "ACTION_UP";
        log("PHOTOPRO_CAMERA_BUTTON_PROBE send attempt"
                + " package=" + PHOTOPRO_PACKAGE
                + " action=" + Intent.ACTION_CAMERA_BUTTON
                + " keyCode=" + event.getKeyCode()
                + " keyAction=" + actionName);
        Intent intent = new Intent(Intent.ACTION_CAMERA_BUTTON)
                .setPackage(PHOTOPRO_PACKAGE)
                .putExtra(Intent.EXTRA_KEY_EVENT, event);
        try {
            sendBroadcast(intent);
            log("PHOTOPRO_CAMERA_BUTTON_PROBE send returned normally"
                    + " package=" + PHOTOPRO_PACKAGE
                    + " action=" + Intent.ACTION_CAMERA_BUTTON
                    + " keyCode=" + event.getKeyCode()
                    + " keyAction=" + actionName);
        } catch (SecurityException error) {
            log("PHOTOPRO_CAMERA_BUTTON_PROBE send exception"
                    + " exception=SecurityException"
                    + " message=" + String.valueOf(error.getMessage())
                    + " package=" + PHOTOPRO_PACKAGE
                    + " keyAction=" + actionName);
        } catch (RuntimeException error) {
            log("PHOTOPRO_CAMERA_BUTTON_PROBE send exception"
                    + " exception=" + error.getClass().getSimpleName()
                    + " message=" + String.valueOf(error.getMessage())
                    + " package=" + PHOTOPRO_PACKAGE
                    + " keyAction=" + actionName);
        }
    }

    private void performSonyRecAction(Om8SonyTriggerTiming timing) {
        performSonyRecAction(timing, true);
    }

    private void performSonyRecAction(Om8SonyTriggerTiming timing,
                                      boolean allowPreparedFastPath) {
        boolean fromOm8 = timing != null;
        boolean interactive;
        boolean keyguardLocked;
        String configuredPackage;
        String activePackage;
        boolean videoProActive;
        boolean usedSafetySnapshot = false;
        if (fromOm8) {
            timing.safetySnapshotAvailable = safetySnapshotValid;
            if (safetySnapshotValid && snapshotValidatedAtNanos >= 0L) {
                timing.safetySnapshotAgeNanos = Math.max(0L,
                        SystemClock.elapsedRealtimeNanos() - snapshotValidatedAtNanos);
            }

            long localSafetyStart = SystemClock.elapsedRealtimeNanos();
            timing.tSafetySnapshotCheckStartNanos = localSafetyStart;
            boolean snapshotGenerationValid = safetySnapshotValid
                    && snapshotForegroundGeneration == foregroundGeneration
                    && snapshotStructuralGeneration == structuralGeneration
                    && preparedRecNode != null
                    && preparedForegroundGeneration == snapshotForegroundGeneration
                    && preparedStructuralGeneration == snapshotStructuralGeneration;
            boolean snapshotWindowValid = safetySnapshotValid
                    && snapshotPreparedWindowId != UNKNOWN_WINDOW_ID
                    && currentSonyWindowId != UNKNOWN_WINDOW_ID
                    && preparedRecNode != null
                    && preparedWindowId == snapshotPreparedWindowId
                    && preparedWindowId == currentSonyWindowId;
            boolean currentSnapshotValid = snapshotGenerationValid
                    && snapshotWindowValid
                    && snapshotInteractive
                    && snapshotKeyguardUnlocked
                    && snapshotSonyForeground;
            timing.safetySnapshotGenerationValid = snapshotGenerationValid;
            timing.safetySnapshotWindowValid = snapshotWindowValid;
            timing.safetySnapshotValid = currentSnapshotValid;
            if (allowPreparedFastPath && currentSnapshotValid) {
                usedSafetySnapshot = true;
                timing.safetySnapshotFastPathUsed = true;
                interactive = snapshotInteractive;
                keyguardLocked = !snapshotKeyguardUnlocked;
                configuredPackage = snapshotConfiguredPackage;
                activePackage = effectiveForegroundPackage == null
                        ? ""
                        : effectiveForegroundPackage;
                videoProActive = snapshotSonyForeground;
            } else {
                interactive = false;
                keyguardLocked = true;
                configuredPackage = "";
                activePackage = "";
                videoProActive = false;
            }
            timing.tSafetySnapshotCheckEndNanos = SystemClock.elapsedRealtimeNanos();
            if (usedSafetySnapshot) {
                timing.tForegroundCheckStartNanos = localSafetyStart;
                timing.tForegroundCheckEndNanos = timing.tSafetySnapshotCheckEndNanos;
            }
        } else {
            interactive = false;
            keyguardLocked = true;
            configuredPackage = "";
            activePackage = "";
            videoProActive = false;
        }

        if (!usedSafetySnapshot) {
            if (fromOm8) {
                timing.tForegroundCheckStartNanos = SystemClock.elapsedRealtimeNanos();
                timing.tInteractiveCheckStartNanos = timing.tForegroundCheckStartNanos;
                timing.interactiveCheckPerformed = true;
            }
            interactive = isDeviceInteractive();
            if (fromOm8) {
                timing.tInteractiveCheckEndNanos = SystemClock.elapsedRealtimeNanos();
                timing.tKeyguardCheckStartNanos = timing.tInteractiveCheckEndNanos;
                timing.keyguardCheckPerformed = true;
            }
            keyguardLocked = isKeyguardLocked();
            if (fromOm8) {
                timing.tKeyguardCheckEndNanos = SystemClock.elapsedRealtimeNanos();
                timing.tForegroundPackageCheckStartNanos = timing.tKeyguardCheckEndNanos;
                timing.foregroundPackageCheckPerformed = true;
            }
            configuredPackage = VIDEO_PRO_PACKAGE;
            activePackage = effectiveForegroundPackage == null
                    ? ""
                    : effectiveForegroundPackage;
            videoProActive = VideoProKeyDecision.isVideoProForeground(
                    configuredPackage, activePackage);
            if (fromOm8) {
                timing.tForegroundPackageCheckEndNanos = SystemClock.elapsedRealtimeNanos();
                timing.tForegroundCheckEndNanos = SystemClock.elapsedRealtimeNanos();
            }
        }
        if (!fromOm8) {
            log("DIAGNOSTIC_REQUEST received"
                    + " eventPackage=" + lastAccessibilityEventPackage
                    + " effectiveForegroundPackage=" + activePackage
                    + " configuredPackage=" + configuredPackage
                    + " interactive=" + interactive
                    + " keyguardLocked=" + keyguardLocked
                    + " videoProActive=" + videoProActive);
        }

        if (!interactive) {
            invalidateAccessibilityNodeCaches();
            rejectSonyRecAction(timing, "device-not-interactive");
            return;
        }
        if (keyguardLocked) {
            invalidateAccessibilityNodeCaches();
            rejectSonyRecAction(timing, "keyguard-locked");
            return;
        }
        if (!videoProActive) {
            invalidateAccessibilityNodeCaches();
            rejectSonyRecAction(timing, "foreground-package-not-video-pro");
            return;
        }

        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo recStatusNode = null;
        List<AccessibilityNodeInfo> matches = java.util.Collections.emptyList();
        boolean usingPreparedNode = false;
        boolean usingCachedNode = false;
        try {
            AccessibilityNodeInfo intendedNode = null;
            RecNodePredicates intendedPredicates = null;
            if (fromOm8 && allowPreparedFastPath) {
                timing.preparedNodeAvailable = preparedRecNode != null;
                timing.tPreparedGenerationCheckStartNanos = SystemClock.elapsedRealtimeNanos();
                timing.preparedNodeGenerationValid = preparedRecNode != null
                        && preparedForegroundGeneration == foregroundGeneration
                        && preparedStructuralGeneration == structuralGeneration;
                timing.tPreparedGenerationCheckEndNanos = SystemClock.elapsedRealtimeNanos();
                timing.tPreparedWindowCheckStartNanos = SystemClock.elapsedRealtimeNanos();
                timing.preparedNodeWindowValid = preparedRecNode != null
                        && preparedWindowId != UNKNOWN_WINDOW_ID
                        && currentSonyWindowId != UNKNOWN_WINDOW_ID
                        && preparedWindowId == currentSonyWindowId;
                timing.tPreparedWindowCheckEndNanos = SystemClock.elapsedRealtimeNanos();
                if (preparedRecNode != null && preparedAtElapsedRealtimeNanos >= 0L) {
                    timing.preparedNodeAgeNanos = Math.max(0L,
                            SystemClock.elapsedRealtimeNanos() - preparedAtElapsedRealtimeNanos);
                }
                if (timing.preparedNodeAvailable
                        && timing.preparedNodeGenerationValid
                        && timing.preparedNodeWindowValid) {
                    intendedNode = preparedRecNode;
                    usingPreparedNode = true;
                    usingCachedNode = false;
                    timing.preparedNodeFastPathUsed = true;
                } else if (preparedRecNode != null) {
                    invalidatePreparedRecNode();
                }
            }
            if (fromOm8) {
                timing.cachedNodeAvailable = cachedRecNode != null;
                if (!usingPreparedNode && cachedRecNode != null) {
                    boolean refreshed = false;
                    timing.preClickNodeRefreshPerformed = true;
                    timing.tCachedNodeRefreshStartNanos = SystemClock.elapsedRealtimeNanos();
                    long refreshStart = timing.tCachedNodeRefreshStartNanos;
                    try {
                        refreshed = cachedRecNode.refresh();
                    } catch (RuntimeException ignored) {
                        // A failed refresh is handled as a cache miss and falls back safely.
                    }
                    timing.tCachedNodeRefreshEndNanos = SystemClock.elapsedRealtimeNanos();
                    timing.preClickNodeRefreshNanos = timing.tCachedNodeRefreshEndNanos - refreshStart;
                    timing.cachedNodeRefreshResult = refreshed;
                    if (refreshed) {
                        int refreshedWindowId = cachedRecNode.getWindowId();
                        boolean windowMatches = cachedRecWindowId == UNKNOWN_WINDOW_ID
                                || refreshedWindowId == UNKNOWN_WINDOW_ID
                                || refreshedWindowId == cachedRecWindowId;
                        timing.tNodeValidationStartNanos = SystemClock.elapsedRealtimeNanos();
                        RecNodePredicates cachedPredicates = inspectRecNode(cachedRecNode);
                        timing.tNodeValidationEndNanos = SystemClock.elapsedRealtimeNanos();
                        if (windowMatches && cachedPredicates.isUsable()) {
                            intendedNode = cachedRecNode;
                            intendedPredicates = cachedPredicates;
                            usingCachedNode = true;
                            timing.cachedNodeFastPathUsed = true;
                        } else {
                            invalidateCachedRecNode();
                        }
                    } else {
                        invalidateCachedRecNode();
                    }
                }
                if (!usingPreparedNode && !usingCachedNode) {
                    timing.freshRootFallbackUsed = true;
                }
            }

            if (!usingPreparedNode && !usingCachedNode) {
                if (fromOm8) {
                    timing.tRootQueryStartNanos = SystemClock.elapsedRealtimeNanos();
                }
                root = getRootInActiveWindow();
                if (fromOm8) {
                    timing.tRootQueryEndNanos = SystemClock.elapsedRealtimeNanos();
                } else {
                    log("DIAGNOSTIC root available=" + (root != null));
                }
                if (root == null) {
                    if (fromOm8) {
                        rejectSonyRecAction(timing, "root-unavailable");
                    } else {
                        log("DIAGNOSTIC ACTION_CLICK willAttempt=false reason=root-unavailable");
                    }
                    return;
                }

                if (fromOm8) {
                    timing.tRecordingNodeQueryStartNanos = SystemClock.elapsedRealtimeNanos();
                }
                matches = root.findAccessibilityNodeInfosByViewId(RECORDING_NODE_ID);
                if (matches == null) {
                    matches = java.util.Collections.emptyList();
                }
                if (fromOm8) {
                    timing.tRecordingNodeQueryEndNanos = SystemClock.elapsedRealtimeNanos();
                } else {
                    log("DIAGNOSTIC recording matching nodes=" + matches.size());
                }

                if (fromOm8) {
                    timing.tNodeValidationStartNanos = SystemClock.elapsedRealtimeNanos();
                }
                for (int index = 0; index < matches.size(); index++) {
                    AccessibilityNodeInfo node = matches.get(index);
                    RecNodePredicates predicates = inspectRecNode(node);
                    if (intendedNode == null && predicates.isUsable()) {
                        intendedNode = node;
                        intendedPredicates = predicates;
                    }
                }

                boolean willAttempt = matches.size() == 1 && intendedNode != null;
                if (fromOm8) {
                    timing.tNodeValidationEndNanos = SystemClock.elapsedRealtimeNanos();
                }
                if (!willAttempt) {
                    RecNodePredicates predicates = intendedPredicates;
                    if (predicates == null && matches.size() == 1) {
                        predicates = inspectRecNode(matches.get(0));
                    }
                    log((fromOm8 ? "SONY" : "DIAGNOSTIC") + " predicates"
                            + " unique=" + (matches.size() == 1)
                            + " packageMatches=" + (predicates != null && predicates.packageMatches)
                            + " resourceIdMatches=" + (predicates != null && predicates.resourceIdMatches)
                            + " classMatches=" + (predicates != null && predicates.classMatches)
                            + " clickable=" + (predicates != null && predicates.clickable)
                            + " enabled=" + (predicates != null && predicates.enabled)
                            + " visible=" + (predicates != null && predicates.visible)
                            + " descriptionMatches=" + (predicates != null && predicates.descriptionMatches));
                    if (fromOm8) {
                        log("SONY recording matching nodes=" + matches.size());
                        log("SONY ACTION_CLICK willAttempt=false reason=node-not-unique-or-usable");
                        for (int index = 0; index < matches.size(); index++) {
                            logRecordingNode("SONY", index, matches.get(index));
                        }
                        rejectSonyRecAction(timing, "rec-node-not-unique-or-usable");
                    } else {
                        log("DIAGNOSTIC ACTION_CLICK willAttempt=false reason=node-not-unique-or-usable");
                        for (int index = 0; index < matches.size(); index++) {
                            logRecordingNode("DIAGNOSTIC", index, matches.get(index));
                        }
                    }
                    return;
                }

                if (fromOm8) {
                    cachedRecNode = AccessibilityNodeInfo.obtain(intendedNode);
                    cachedRecWindowId = cachedRecNode.getWindowId();
                }
            }

            String expectedState = null;
            if (!fromOm8) {
                // Preserve the explicit ADB diagnostic's before/after reporting. The
                // OM8 hot path deliberately skips this non-safety lookup.
                recStatusNode = findFirstNode(root, REC_STATUS_NODE_ID);
                String currentState = normalizeRecStatus(
                        recStatusNode == null ? null : recStatusNode.getText());
                if (!isAuthoritativeRecStatus(currentState)) {
                    log("DIAGNOSTIC recStatus before=<unusable> value="
                            + (currentState.isEmpty() ? "<unavailable>" : currentState));
                    return;
                }
                expectedState = "REC".equals(currentState) ? "Stby" : "REC";
                log("DIAGNOSTIC recStatus before=" + currentState
                        + " expectedAfter=" + expectedState);
                log("DIAGNOSTIC ACTION_CLICK willAttempt=true");
            }

            if (fromOm8) {
                timing.tPreparedActionReadyNanos = SystemClock.elapsedRealtimeNanos();
                timing.tPerformActionStartNanos = SystemClock.elapsedRealtimeNanos();
            }
            long actionClickNanos = fromOm8
                    ? timing.tPerformActionStartNanos
                    : SystemClock.elapsedRealtimeNanos();
            boolean clicked = intendedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (fromOm8) {
                timing.tPerformActionEndNanos = SystemClock.elapsedRealtimeNanos();
            }
            if (fromOm8) {
                if (usingPreparedNode) {
                    timing.preparedNodeActionResult = clicked;
                    invalidatePreparedRecNode();
                    if (!clicked) {
                        timing.preparedNodeFastPathUsed = false;
                        performSonyRecAction(timing, false);
                        return;
                    }
                }
                log("SONY_ACTION_CLICK result=" + clicked
                        + " om8PressSequence=" + timing.om8PressSequence
                        + " t_sony_action_click=" + actionClickNanos);
                log("SONY prepared available=" + timing.preparedNodeAvailable
                        + " generationValid=" + timing.preparedNodeGenerationValid
                        + " windowValid=" + timing.preparedNodeWindowValid
                        + " fastPathUsed=" + timing.preparedNodeFastPathUsed);
                log("SONY cache available=" + timing.cachedNodeAvailable
                        + " refreshResult=" + timing.cachedNodeRefreshResult
                        + " fastPathUsed=" + timing.cachedNodeFastPathUsed
                        + " freshRootFallbackUsed=" + timing.freshRootFallbackUsed);
                if (usingPreparedNode) {
                    log("SONY PREPARED_NODE_DIRECT_ACTION");
                } else if (!usingCachedNode) {
                    log("SONY recording matching nodes=" + matches.size());
                    for (int index = 0; index < matches.size(); index++) {
                        logRecordingNode("SONY", index, matches.get(index));
                    }
                } else {
                    log("SONY CACHE_HIT refreshed REC node used");
                }
                log("SONY preClickRecStatusQuery=false");
                logOm8Timing(timing, clicked ? "CLICK_SUBMITTED" : "CLICK_REJECTED");
            } else {
                log("performAction(ACTION_CLICK)=" + clicked);
            }
            if (fromOm8 && !clicked && usingCachedNode) {
                invalidateCachedRecNode();
            }
            if (clicked) {
                SonyRecVerification verification = new SonyRecVerification(
                        fromOm8,
                        expectedState,
                        timing,
                        actionClickNanos);
                diagnosticHandler.postDelayed(
                        () -> verifySonyRecState(verification, false),
                        POST_CLICK_CHECK_ONE_MS);
                diagnosticHandler.postDelayed(
                        () -> verifySonyRecState(verification, true),
                        POST_CLICK_CHECK_TWO_MS);
            }
        } catch (RuntimeException error) {
            log((fromOm8 ? "SONY" : "DIAGNOSTIC") + " tree/click failed="
                    + error.getClass().getSimpleName()
                    + " message=" + error.getMessage());
            if (fromOm8) {
                logOm8Timing(timing, "EXCEPTION_" + error.getClass().getSimpleName());
            }
        } finally {
            recycleNodes(matches);
            recycleNode(recStatusNode);
            if (root != null) {
                root.recycle();
            }
        }
    }

    private void rejectSonyRecAction(Om8SonyTriggerTiming timing, String reason) {
        if (timing != null) {
            log("OM8_SONY_TRIGGER_REJECTED reason=" + reason);
            logOm8Timing(timing, "REJECTED_" + reason);
            return;
        }
        log("DIAGNOSTIC_REQUEST rejected: " + reason);
        log("DIAGNOSTIC root available=false (not queried after foreground/safety rejection)");
        log("DIAGNOSTIC ACTION_CLICK willAttempt=false");
    }

    private void schedulePreparedRecNodePreparation() {
        schedulePreparedRecNodePreparation(null);
    }

    private void schedulePreparedRecNodePreparation(Om8SonyTriggerTiming timing) {
        if (timing != null) {
            pendingPreparationTiming = timing;
            timing.postClickRearmAttempted = true;
            if (timing.tPostClickRearmRequestNanos < 0L) {
                timing.tPostClickRearmRequestNanos = SystemClock.elapsedRealtimeNanos();
            }
        }
        if (preparationScheduled) {
            return;
        }
        preparationScheduled = true;
        int cycle = ++preparationCycle;
        preparationExtendedRecovery = false;
        scheduledPreparationAttempt = 1;
        schedulePreparedRecNodePreparationAttempt(
                cycle, 1, PREPARE_DEBOUNCE_MS, false);
    }

    private void schedulePreparedRecNodePreparationAttempt(int cycle,
                                                            int attempt,
                                                            long delayMs,
                                                            boolean extendedRecovery) {
        Runnable runnable = () -> {
            if (!preparationScheduled || cycle != preparationCycle) {
                return;
            }
            scheduledPreparationRunnable = null;
            scheduledPreparationAttempt = attempt;
            preparationExtendedRecovery = extendedRecovery;
            Om8SonyTriggerTiming rearmTiming = pendingPreparationTiming;
            if (rearmTiming != null && rearmTiming.tPostClickRearmBeginNanos < 0L) {
                rearmTiming.tPostClickRearmBeginNanos = SystemClock.elapsedRealtimeNanos();
                rearmTiming.tPostClickRearmStartNanos = rearmTiming.tPostClickRearmBeginNanos;
            }
            if (rearmTiming != null) {
                log("POST_CLICK_REARM attempt=" + attempt
                        + " om8PressSequence=" + rearmTiming.om8PressSequence
                        + " begin");
            }

            PreparationResult result = prepareRecNodeOutsideHotPath();
            if (result.prepared) {
                finishPreparationCycle(cycle, attempt, result, rearmTiming);
                return;
            }

            if (rearmTiming != null) {
                rearmTiming.postClickRearmAttempt = attempt;
                rearmTiming.postClickRearmFailureReason = result.reason;
                lastRearmFailureReason = result.reason;
                lastRearmFailureNanos = SystemClock.elapsedRealtimeNanos();
                log("POST_CLICK_REARM attempt=" + attempt
                        + " reason=" + result.reason
                        + " om8PressSequence=" + rearmTiming.om8PressSequence
                        + " timeSinceVerifiedStateNanos="
                        + nanosSince(rearmTiming.tSonyVerifiedStateNanos,
                        lastRearmFailureNanos)
                        + " timeSinceActionReturnNanos="
                        + nanosSince(rearmTiming.tPerformActionEndNanos,
                        lastRearmFailureNanos));
                log("POST_CLICK_REARM_FAILED reason=" + result.reason
                        + " attempt=" + attempt
                        + " om8PressSequence=" + rearmTiming.om8PressSequence
                        + " timeSinceVerifiedStateNanos="
                        + nanosSince(rearmTiming.tSonyVerifiedStateNanos,
                        lastRearmFailureNanos)
                        + " timeSinceActionReturnNanos="
                        + nanosSince(rearmTiming.tPerformActionEndNanos,
                        lastRearmFailureNanos));
            } else {
                lastRearmFailureReason = result.reason;
                lastRearmFailureNanos = SystemClock.elapsedRealtimeNanos();
                log("SONY_REC_NODE_PREPARE_RETRY attempt=" + attempt
                        + " reason=" + result.reason);
            }

            boolean useExtendedRecovery = extendedRecovery
                    || isTransientPreparationReason(result.reason);
            int maxAttempts = useExtendedRecovery
                    ? TRANSIENT_MAX_PREPARE_ATTEMPTS
                    : GENERAL_MAX_PREPARE_ATTEMPTS;
            if (attempt < maxAttempts
                    && result.retryable
                    && isPreparationStillSafe()) {
                long nextDelayMs = useExtendedRecovery
                        ? TRANSIENT_PREPARE_RETRY_DELAYS_MS[attempt]
                        : GENERAL_PREPARE_RETRY_DELAYS_MS[attempt];
                scheduledPreparationAttempt = attempt + 1;
                preparationExtendedRecovery = useExtendedRecovery;
                schedulePreparedRecNodePreparationAttempt(
                        cycle,
                        attempt + 1,
                        nextDelayMs,
                        useExtendedRecovery);
            } else {
                finishPreparationCycle(cycle, attempt, result, rearmTiming);
            }
        };
        scheduledPreparationRunnable = runnable;
        scheduledPreparationAttempt = attempt;
        preparationExtendedRecovery = extendedRecovery;
        diagnosticHandler.postDelayed(runnable, delayMs);
    }

    /**
     * A window/content transition can be the first indication that Sony has
     * re-enabled its REC button. Coalesce that event into the existing retry
     * cycle and move the next attempt forward, without polling or creating a
     * second preparation operation.
     */
    private void requestEventAssistedRearmPreparation() {
        if (preparedRecNode != null
                || !"enabled".equals(lastRearmFailureReason)) {
            return;
        }
        long now = SystemClock.elapsedRealtimeNanos();
        if (lastRearmFailureNanos >= 0L
                && now - lastRearmFailureNanos < EVENT_REARM_DEBOUNCE_MS * 1_000_000L) {
            return;
        }
        if (!preparationScheduled) {
            preparationScheduled = true;
            int cycle = ++preparationCycle;
            preparationExtendedRecovery = true;
            scheduledPreparationAttempt = 1;
            log("POST_CLICK_REARM_EVENT_ASSISTED attempt=1"
                    + " om8PressSequence="
                    + (pendingPreparationTiming == null
                    ? -1L : pendingPreparationTiming.om8PressSequence));
            schedulePreparedRecNodePreparationAttempt(cycle, 1, 0L, true);
            return;
        }
        if (scheduledPreparationRunnable == null) {
            return;
        }
        diagnosticHandler.removeCallbacks(scheduledPreparationRunnable);
        scheduledPreparationRunnable = null;
        int attempt = Math.max(1, scheduledPreparationAttempt);
        preparationExtendedRecovery = true;
        log("POST_CLICK_REARM_EVENT_ASSISTED attempt=" + attempt
                + " om8PressSequence="
                + (pendingPreparationTiming == null
                ? -1L : pendingPreparationTiming.om8PressSequence));
        schedulePreparedRecNodePreparationAttempt(
                preparationCycle, attempt, 0L, true);
    }

    private void finishPreparationCycle(int cycle,
                                        int attempt,
                                        PreparationResult result,
                                        Om8SonyTriggerTiming rearmTiming) {
        if (cycle != preparationCycle) {
            return;
        }
        preparationScheduled = false;
        scheduledPreparationRunnable = null;
        scheduledPreparationAttempt = 0;
        preparationExtendedRecovery = false;
        pendingPreparationTiming = null;
        if (rearmTiming == null) {
            if (!result.prepared) {
                lastRearmFailureReason = result.reason;
                lastRearmFailureNanos = SystemClock.elapsedRealtimeNanos();
                log("SONY_REC_NODE_PREPARE_GAVE_UP reason=" + result.reason
                        + " attempt=" + attempt);
            } else {
                lastRearmFailureReason = "";
                lastRearmFailureNanos = -1L;
            }
            return;
        }

        rearmTiming.postClickRearmAttempt = attempt;
        rearmTiming.postClickRearmResult = result.prepared;
        rearmTiming.postClickRearmFailureReason = result.prepared ? "" : result.reason;
        if (result.prepared) {
            lastRearmFailureReason = "";
            lastRearmFailureNanos = -1L;
            rearmTiming.tPostClickRearmReadyNanos = SystemClock.elapsedRealtimeNanos();
            rearmTiming.tPostClickRearmEndNanos = rearmTiming.tPostClickRearmReadyNanos;
            log("POST_CLICK_REARMED attempt=" + attempt
                    + " om8PressSequence=" + rearmTiming.om8PressSequence
                    + " timeSinceVerifiedStateNanos="
                    + nanosSince(rearmTiming.tSonyVerifiedStateNanos,
                    rearmTiming.tPostClickRearmReadyNanos)
                    + " timeSinceActionReturnNanos="
                    + nanosSince(rearmTiming.tPerformActionEndNanos,
                    rearmTiming.tPostClickRearmReadyNanos));
            logOm8Timing(rearmTiming, "POST_CLICK_REARMED");
        } else {
            rearmTiming.tPostClickRearmEndNanos = SystemClock.elapsedRealtimeNanos();
            lastRearmFailureReason = result.reason;
            lastRearmFailureNanos = rearmTiming.tPostClickRearmEndNanos;
            log("POST_CLICK_REARM_GAVE_UP reason=" + result.reason
                    + " attempt=" + attempt
                    + " om8PressSequence=" + rearmTiming.om8PressSequence
                    + " timeSinceVerifiedStateNanos="
                    + nanosSince(rearmTiming.tSonyVerifiedStateNanos,
                    rearmTiming.tPostClickRearmEndNanos)
                    + " timeSinceActionReturnNanos="
                    + nanosSince(rearmTiming.tPerformActionEndNanos,
                    rearmTiming.tPostClickRearmEndNanos));
            logOm8Timing(rearmTiming, "POST_CLICK_REARM_GAVE_UP_" + result.reason);
        }
    }

    private boolean isTransientPreparationReason(String reason) {
        return "enabled".equals(reason)
                || "clickable".equals(reason)
                || "visible".equals(reason);
    }

    private long nanosSince(long startNanos, long endNanos) {
        if (startNanos < 0L || endNanos < startNanos) {
            return -1L;
        }
        return endNanos - startNanos;
    }

    private boolean isPreparationStillSafe() {
        return isDeviceInteractive()
                && !isKeyguardLocked()
                && VIDEO_PRO_PACKAGE.equals(effectiveForegroundPackage);
    }

    private void registerSafetyEventReceiver() {
        if (safetyEventReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(safetyEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(safetyEventReceiver, filter);
            }
            safetyEventReceiverRegistered = true;
            log("SAFETY_SNAPSHOT lifecycle receiver registered");
        } catch (RuntimeException error) {
            log("SAFETY_SNAPSHOT lifecycle receiver registration failed="
                    + error.getClass().getSimpleName());
        }
    }

    private void unregisterSafetyEventReceiver() {
        if (!safetyEventReceiverRegistered) {
            return;
        }
        try {
            unregisterReceiver(safetyEventReceiver);
        } catch (IllegalArgumentException ignored) {
            // The receiver may already have been unregistered during teardown.
        }
        safetyEventReceiverRegistered = false;
    }

    /** Runs only on the service main looper and never from the OM8 click path. */
    private void scheduleSafetySnapshotRevalidation() {
        diagnosticHandler.post(() -> {
            if (!isDeviceInteractive()
                    || isKeyguardLocked()
                    || !VIDEO_PRO_PACKAGE.equals(effectiveForegroundPackage)) {
                invalidateSafetySnapshot();
                return;
            }
            if (preparedRecNode != null) {
                refreshSafetySnapshotForPreparedNode();
            } else {
                schedulePreparedRecNodePreparation();
            }
        });
    }

    /**
     * Rechecks system safety and the already-prepared local node context off the
     * physical-button path. No root lookup, refresh, or node search occurs here.
     */
    private void refreshSafetySnapshotForPreparedNode() {
        invalidateSafetySnapshot();
        boolean interactive = isDeviceInteractive();
        boolean keyguardLocked = isKeyguardLocked();
        String configuredPackage = VIDEO_PRO_PACKAGE;
        String activePackage = effectiveForegroundPackage == null
                ? ""
                : effectiveForegroundPackage;
        boolean sonyForeground = VideoProKeyDecision.isVideoProForeground(
                configuredPackage, activePackage);
        boolean preparedContextValid = preparedRecNode != null
                && preparedWindowId != UNKNOWN_WINDOW_ID
                && currentSonyWindowId != UNKNOWN_WINDOW_ID
                && preparedWindowId == currentSonyWindowId
                && preparedForegroundGeneration == foregroundGeneration
                && preparedStructuralGeneration == structuralGeneration;
        if (!interactive || keyguardLocked || !sonyForeground || !preparedContextValid) {
            return;
        }
        snapshotInteractive = true;
        snapshotKeyguardUnlocked = true;
        snapshotSonyForeground = true;
        snapshotConfiguredPackage = configuredPackage;
        snapshotForegroundGeneration = foregroundGeneration;
        snapshotStructuralGeneration = structuralGeneration;
        snapshotPreparedWindowId = preparedWindowId;
        snapshotValidatedAtNanos = SystemClock.elapsedRealtimeNanos();
        safetySnapshotValid = true;
        log("SAFETY_SNAPSHOT valid windowId=" + snapshotPreparedWindowId
                + " foregroundGeneration=" + snapshotForegroundGeneration
                + " structuralGeneration=" + snapshotStructuralGeneration);
    }

    private void invalidateSafetySnapshot() {
        safetySnapshotValid = false;
        snapshotInteractive = false;
        snapshotKeyguardUnlocked = false;
        snapshotSonyForeground = false;
        snapshotForegroundGeneration = -1L;
        snapshotStructuralGeneration = -1L;
        snapshotPreparedWindowId = UNKNOWN_WINDOW_ID;
        snapshotValidatedAtNanos = -1L;
        snapshotConfiguredPackage = "";
    }

    private PreparationResult prepareRecNodeOutsideHotPath() {
        String failureReason = "unknown";
        AccessibilityNodeInfo root = null;
        List<AccessibilityNodeInfo> matches = java.util.Collections.emptyList();
        try {
            if (!isDeviceInteractive()) {
                invalidatePreparedRecNode();
                invalidateCachedRecNode();
                return PreparationResult.notPrepared("device-not-interactive", false);
            }
            if (isKeyguardLocked()) {
                invalidatePreparedRecNode();
                invalidateCachedRecNode();
                return PreparationResult.notPrepared("keyguard-locked", false);
            }
            if (!VIDEO_PRO_PACKAGE.equals(effectiveForegroundPackage)) {
                invalidatePreparedRecNode();
                invalidateCachedRecNode();
                return PreparationResult.notPrepared("sony-not-foreground", false);
            }

            if (cachedRecNode != null) {
                boolean refreshed = false;
                try {
                    refreshed = cachedRecNode.refresh();
                } catch (RuntimeException ignored) {
                    failureReason = "cached-node-refresh-exception";
                }
                int nodeWindowId = cachedRecNode.getWindowId();
                boolean windowMatches = currentSonyWindowId != UNKNOWN_WINDOW_ID
                        && nodeWindowId != UNKNOWN_WINDOW_ID
                        && nodeWindowId == currentSonyWindowId;
                RecNodePredicates cachedPredicates = refreshed
                        ? inspectRecNode(cachedRecNode)
                        : RecNodePredicates.none();
                if (!refreshed && failureReason.equals("unknown")) {
                    failureReason = "cached-node-refresh-false";
                } else if (!windowMatches) {
                    failureReason = "window-mismatch";
                } else if (!cachedPredicates.isUsable()) {
                    failureReason = firstFailedPredicate(cachedPredicates);
                }
                if (refreshed && windowMatches && cachedPredicates.isUsable()) {
                    armPreparedRecNode(cachedRecNode);
                    log("SONY_REC_NODE_PREPARED source=cached-refresh"
                            + " windowId=" + preparedWindowId
                            + " foregroundGeneration=" + preparedForegroundGeneration
                            + " structuralGeneration=" + preparedStructuralGeneration);
                    return PreparationResult.prepared();
                } else {
                    invalidateCachedRecNode();
                }
            }

            root = getRootInActiveWindow();
            if (root == null) {
                return PreparationResult.notPrepared("root-unavailable", true);
            }
                int rootWindowId = root.getWindowId();
                if (currentSonyWindowId == UNKNOWN_WINDOW_ID
                        && rootWindowId != UNKNOWN_WINDOW_ID) {
                    currentSonyWindowId = rootWindowId;
                }
                matches = root.findAccessibilityNodeInfosByViewId(RECORDING_NODE_ID);
                if (matches == null || matches.isEmpty()) {
                    return PreparationResult.notPrepared("recording-node-count-0", true);
                }
                if (matches.size() != 1) {
                    return PreparationResult.notPrepared("recording-node-count-multiple", true);
                }
                AccessibilityNodeInfo node = matches.get(0);
                int nodeWindowId = node.getWindowId();
                boolean windowMatches = currentSonyWindowId != UNKNOWN_WINDOW_ID
                        && nodeWindowId != UNKNOWN_WINDOW_ID
                        && nodeWindowId == currentSonyWindowId;
                if (!windowMatches) {
                    return PreparationResult.notPrepared("window-mismatch", true);
                }
                RecNodePredicates predicates = inspectRecNode(node);
                if (!predicates.isUsable()) {
                    return PreparationResult.notPrepared(
                            firstFailedPredicate(predicates), true);
                }
                replaceCachedRecNode(node);
                armPreparedRecNode(node);
                log("SONY_REC_NODE_PREPARED source=fresh-lookup"
                        + " windowId=" + preparedWindowId
                        + " foregroundGeneration=" + preparedForegroundGeneration
                        + " structuralGeneration=" + preparedStructuralGeneration);
            return PreparationResult.prepared();
        } catch (RuntimeException error) {
            invalidatePreparedRecNode();
            invalidateCachedRecNode();
            return PreparationResult.notPrepared(
                    "exception-" + error.getClass().getSimpleName(), true);
        } finally {
            recycleNodes(matches);
            if (root != null) {
                root.recycle();
            }
        }
    }

    private String firstFailedPredicate(RecNodePredicates predicates) {
        if (!predicates.packageMatches) {
            return "package";
        }
        if (!predicates.resourceIdMatches) {
            return "resource-id";
        }
        if (!predicates.classMatches) {
            return "class";
        }
        if (!predicates.clickable) {
            return "clickable";
        }
        if (!predicates.enabled) {
            return "enabled";
        }
        if (!predicates.visible) {
            return "visible";
        }
        if (!predicates.descriptionMatches) {
            return "content-description";
        }
        return "unknown";
    }

    private static final class PreparationResult {
        private final boolean prepared;
        private final String reason;
        private final boolean retryable;

        private PreparationResult(boolean prepared, String reason, boolean retryable) {
            this.prepared = prepared;
            this.reason = reason;
            this.retryable = retryable;
        }

        private static PreparationResult prepared() {
            return new PreparationResult(true, "", false);
        }

        private static PreparationResult notPrepared(String reason, boolean retryable) {
            return new PreparationResult(false, reason, retryable);
        }
    }

    private void replaceCachedRecNode(AccessibilityNodeInfo source) {
        invalidateCachedRecNode();
        cachedRecNode = AccessibilityNodeInfo.obtain(source);
        cachedRecWindowId = cachedRecNode.getWindowId();
    }

    private void armPreparedRecNode(AccessibilityNodeInfo source) {
        invalidatePreparedRecNode();
        preparedRecNode = AccessibilityNodeInfo.obtain(source);
        preparedWindowId = preparedRecNode.getWindowId();
        preparedForegroundGeneration = foregroundGeneration;
        preparedStructuralGeneration = structuralGeneration;
        preparedAtElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        refreshSafetySnapshotForPreparedNode();
    }

    private void logRecordingNode(String prefix, int index, AccessibilityNodeInfo node) {
        if (node == null) {
            log(prefix + " recording node[" + index + "]=<null>");
            return;
        }
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        log(prefix + " recording node[" + index + "]"
                + " packageName=" + value(node.getPackageName())
                + " resourceId=" + value(node.getViewIdResourceName())
                + " className=" + value(node.getClassName())
                + " text=" + value(node.getText())
                + " contentDescription=" + value(node.getContentDescription())
                + " clickable=" + node.isClickable()
                + " enabled=" + node.isEnabled()
                + " visibleToUser=" + node.isVisibleToUser()
                + " bounds=" + bounds);
    }

    private RecNodePredicates inspectRecNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return RecNodePredicates.none();
        }
        boolean packageMatches = VIDEO_PRO_PACKAGE.contentEquals(node.getPackageName());
        CharSequence description = node.getContentDescription();
        boolean descriptionMatches = CAMERA_PROFILE.isAllowedContentDescription(
                description == null ? null : description.toString());
        return new RecNodePredicates(
                packageMatches,
                RECORDING_NODE_ID.equals(node.getViewIdResourceName()),
                REC_NODE_CLASS.contentEquals(node.getClassName()),
                node.isClickable(),
                node.isEnabled(),
                node.isVisibleToUser(),
                descriptionMatches);
    }

    private static final class RecNodePredicates {
        private final boolean packageMatches;
        private final boolean resourceIdMatches;
        private final boolean classMatches;
        private final boolean clickable;
        private final boolean enabled;
        private final boolean visible;
        private final boolean descriptionMatches;

        private RecNodePredicates(boolean packageMatches,
                                  boolean resourceIdMatches,
                                  boolean classMatches,
                                  boolean clickable,
                                  boolean enabled,
                                  boolean visible,
                                  boolean descriptionMatches) {
            this.packageMatches = packageMatches;
            this.resourceIdMatches = resourceIdMatches;
            this.classMatches = classMatches;
            this.clickable = clickable;
            this.enabled = enabled;
            this.visible = visible;
            this.descriptionMatches = descriptionMatches;
        }

        private static RecNodePredicates none() {
            return new RecNodePredicates(false, false, false, false, false, false, false);
        }

        private boolean isUsable() {
            return packageMatches
                    && resourceIdMatches
                    && classMatches
                    && clickable
                    && enabled
                    && visible
                    && descriptionMatches;
        }
    }

    private void logOm8Timing(Om8SonyTriggerTiming timing, String outcome) {
        if (timing == null) {
            return;
        }
        StringBuilder message = new StringBuilder("OM8_TIMING outcome=")
                .append(outcome)
                .append(" om8PressSequence=")
                .append(timing.om8PressSequence)
                .append(" t_ble_callback_enter=")
                .append(timing.tBleCallbackEnterNanos)
                .append(" t_duml_parse_start=")
                .append(timing.tDumlParseStartNanos)
                .append(" t_duml_parse_end=")
                .append(timing.tDumlParseEndNanos)
                .append(" t_edge_detect_start=")
                .append(timing.tEdgeDetectStartNanos)
                .append(" t_edge_detect_end=")
                .append(timing.tEdgeDetectEndNanos)
                .append(" t_bridge_request_start=")
                .append(timing.tBridgeRequestStartNanos)
                .append(" t_bridge_request_end=")
                .append(timing.tBridgeRequestEndNanos)
                .append(" t_sony_trigger_requested=")
                .append(timing.tSonyTriggerRequestedNanos)
                .append(" t_sony_bridge_received=")
                .append(timing.tSonyBridgeReceivedNanos)
                .append(" t_sony_main_dispatch_requested=")
                .append(timing.tSonyMainDispatchRequestedNanos)
                .append(" t_sony_main_dispatch_begin=")
                .append(timing.tSonyMainDispatchBeginNanos)
                .append(" gattCallbackOnMainLooper=")
                .append(timing.gattCallbackOnMainLooper)
                .append(" gattCallbackThreadId=")
                .append(timing.gattCallbackThreadId)
                .append(" sonyBridgeAlreadyOnMainLooper=")
                .append(timing.sonyBridgeAlreadyOnMainLooper)
                .append(" mainDispatchSkipped=")
                .append(timing.mainDispatchSkipped)
                .append(" cachedNodeAvailable=")
                .append(timing.cachedNodeAvailable)
                .append(" cachedNodeRefreshResult=")
                .append(timing.cachedNodeRefreshResult)
                .append(" cachedNodeFastPathUsed=")
                .append(timing.cachedNodeFastPathUsed)
                .append(" freshRootFallbackUsed=")
                .append(timing.freshRootFallbackUsed)
                .append(" t_cached_node_refresh_start=")
                .append(timing.tCachedNodeRefreshStartNanos)
                .append(" t_cached_node_refresh_end=")
                .append(timing.tCachedNodeRefreshEndNanos)
                .append(" preparedNodeAvailable=")
                .append(timing.preparedNodeAvailable)
                .append(" preparedNodeFastPathUsed=")
                .append(timing.preparedNodeFastPathUsed)
                .append(" preparedNodeGenerationValid=")
                .append(timing.preparedNodeGenerationValid)
                .append(" preparedNodeWindowValid=")
                .append(timing.preparedNodeWindowValid)
                .append(" preparedNodeAgeNanos=")
                .append(timing.preparedNodeAgeNanos)
                .append(" safetySnapshotAvailable=")
                .append(timing.safetySnapshotAvailable)
                .append(" safetySnapshotValid=")
                .append(timing.safetySnapshotValid)
                .append(" safetySnapshotFastPathUsed=")
                .append(timing.safetySnapshotFastPathUsed)
                .append(" safetySnapshotGenerationValid=")
                .append(timing.safetySnapshotGenerationValid)
                .append(" safetySnapshotWindowValid=")
                .append(timing.safetySnapshotWindowValid)
                .append(" safetySnapshotAgeNanos=")
                .append(timing.safetySnapshotAgeNanos)
                .append(" t_safety_snapshot_check_start=")
                .append(timing.tSafetySnapshotCheckStartNanos)
                .append(" t_safety_snapshot_check_end=")
                .append(timing.tSafetySnapshotCheckEndNanos)
                .append(" interactiveCheckPerformed=")
                .append(timing.interactiveCheckPerformed)
                .append(" keyguardCheckPerformed=")
                .append(timing.keyguardCheckPerformed)
                .append(" foregroundPackageCheckPerformed=")
                .append(timing.foregroundPackageCheckPerformed)
                .append(" preClickNodeRefreshPerformed=")
                .append(timing.preClickNodeRefreshPerformed)
                .append(" preClickNodeRefreshNanos=")
                .append(timing.preClickNodeRefreshNanos)
                .append(" preparedNodeActionResult=")
                .append(timing.preparedNodeActionResult)
                .append(" postClickRearmAttempted=")
                .append(timing.postClickRearmAttempted)
                .append(" postClickRearmResult=")
                .append(timing.postClickRearmResult)
                .append(" postClickRearmAttempt=")
                .append(timing.postClickRearmAttempt)
                .append(" postClickRearmFailureReason=")
                .append(timing.postClickRearmFailureReason)
                .append(" t_post_click_rearm_request=")
                .append(timing.tPostClickRearmRequestNanos)
                .append(" t_post_click_rearm_begin=")
                .append(timing.tPostClickRearmBeginNanos)
                .append(" t_post_click_rearm_ready=")
                .append(timing.tPostClickRearmReadyNanos)
                .append(" t_post_click_rearm_start=")
                .append(timing.tPostClickRearmStartNanos)
                .append(" t_post_click_rearm_end=")
                .append(timing.tPostClickRearmEndNanos)
                .append(" t_foreground_check_start=")
                .append(timing.tForegroundCheckStartNanos)
                .append(" t_foreground_check_end=")
                .append(timing.tForegroundCheckEndNanos)
                .append(" t_interactive_check_start=")
                .append(timing.tInteractiveCheckStartNanos)
                .append(" t_interactive_check_end=")
                .append(timing.tInteractiveCheckEndNanos)
                .append(" t_keyguard_check_start=")
                .append(timing.tKeyguardCheckStartNanos)
                .append(" t_keyguard_check_end=")
                .append(timing.tKeyguardCheckEndNanos)
                .append(" t_foreground_package_check_start=")
                .append(timing.tForegroundPackageCheckStartNanos)
                .append(" t_foreground_package_check_end=")
                .append(timing.tForegroundPackageCheckEndNanos)
                .append(" t_prepared_generation_check_start=")
                .append(timing.tPreparedGenerationCheckStartNanos)
                .append(" t_prepared_generation_check_end=")
                .append(timing.tPreparedGenerationCheckEndNanos)
                .append(" t_prepared_window_check_start=")
                .append(timing.tPreparedWindowCheckStartNanos)
                .append(" t_prepared_window_check_end=")
                .append(timing.tPreparedWindowCheckEndNanos)
                .append(" t_root_query_start=")
                .append(timing.tRootQueryStartNanos)
                .append(" t_root_query_end=")
                .append(timing.tRootQueryEndNanos)
                .append(" t_recording_node_query_start=")
                .append(timing.tRecordingNodeQueryStartNanos)
                .append(" t_recording_node_query_end=")
                .append(timing.tRecordingNodeQueryEndNanos)
                .append(" t_rec_status_query_start=")
                .append(timing.tRecStatusQueryStartNanos)
                .append(" t_rec_status_query_end=")
                .append(timing.tRecStatusQueryEndNanos)
                .append(" t_post_click_rec_status_query_start=")
                .append(timing.tPostClickRecStatusQueryStartNanos)
                .append(" t_post_click_rec_status_query_end=")
                .append(timing.tPostClickRecStatusQueryEndNanos)
                .append(" t_node_validation_start=")
                .append(timing.tNodeValidationStartNanos)
                .append(" t_node_validation_end=")
                .append(timing.tNodeValidationEndNanos)
                .append(" t_perform_action_start=")
                .append(timing.tPerformActionStartNanos)
                .append(" t_perform_action_end=")
                .append(timing.tPerformActionEndNanos)
                 .append(" t_prepared_action_ready=")
                 .append(timing.tPreparedActionReadyNanos)
                .append(" t_action_return=")
                .append(timing.tPerformActionEndNanos)
                .append(" t_sony_action_click=")
                .append(timing.tPerformActionStartNanos)
                .append(" t_sony_verified_state=")
                .append(timing.tSonyVerifiedStateNanos)
                .append(" t_rec_state_verified=")
                .append(timing.tSonyVerifiedStateNanos)
                .append(" t_rearm_request=")
                .append(timing.tPostClickRearmRequestNanos)
                .append(" t_rearm_begin=")
                .append(timing.tPostClickRearmBeginNanos)
                .append(" t_rearm_ready=")
                .append(timing.tPostClickRearmReadyNanos)
                .append(" preClickRecStatusQuery=false")
                .append(" preClickRecStatusQueryNanos=0")
                .append(" bleCallbackToParseNanos=")
                .append(delta(timing.tBleCallbackEnterNanos, timing.tDumlParseStartNanos))
                .append(" parseNanos=")
                .append(delta(timing.tDumlParseStartNanos, timing.tDumlParseEndNanos))
                .append(" edgeDetectionNanos=")
                .append(delta(timing.tEdgeDetectStartNanos, timing.tEdgeDetectEndNanos))
                .append(" bridgeDispatchNanos=")
                .append(delta(timing.tBridgeRequestStartNanos, timing.tBridgeRequestEndNanos))
                .append(" mainThreadQueueNanos=")
                .append(timing.mainDispatchSkipped
                        ? 0L
                        : delta(timing.tSonyMainDispatchRequestedNanos,
                        timing.tSonyMainDispatchBeginNanos))
                .append(" callbackEntryToBridgeRequestNanos=")
                .append(delta(timing.tBleCallbackEnterNanos,
                        timing.tBridgeRequestStartNanos))
                .append(" bridgeRequestToSonyClickNanos=")
                .append(delta(timing.tBridgeRequestStartNanos,
                        timing.tPerformActionStartNanos))
                .append(" cachedNodeRefreshNanos=")
                .append(timing.cachedNodeAvailable
                        ? delta(timing.tCachedNodeRefreshStartNanos,
                        timing.tCachedNodeRefreshEndNanos)
                        : 0L)
                .append(" postClickRearmNanos=")
                .append(delta(timing.tPostClickRearmRequestNanos,
                        timing.tPostClickRearmReadyNanos))
                .append(" actionReturnToRearmReadyNanos=")
                .append(delta(timing.tPerformActionEndNanos,
                        timing.tPostClickRearmReadyNanos))
                .append(" verifiedStateToRearmReadyNanos=")
                .append(delta(timing.tSonyVerifiedStateNanos,
                        timing.tPostClickRearmReadyNanos))
                .append(" foregroundCheckNanos=")
                .append(delta(timing.tForegroundCheckStartNanos, timing.tForegroundCheckEndNanos))
                .append(" localSafetySnapshotCheckNanos=")
                .append(delta(timing.tSafetySnapshotCheckStartNanos,
                        timing.tSafetySnapshotCheckEndNanos))
                .append(" interactiveCheckNanos=")
                .append(timing.interactiveCheckPerformed
                        ? delta(timing.tInteractiveCheckStartNanos,
                        timing.tInteractiveCheckEndNanos)
                        : 0L)
                .append(" keyguardCheckNanos=")
                .append(timing.keyguardCheckPerformed
                        ? delta(timing.tKeyguardCheckStartNanos,
                        timing.tKeyguardCheckEndNanos)
                        : 0L)
                .append(" foregroundPackageCheckNanos=")
                .append(timing.foregroundPackageCheckPerformed
                        ? delta(timing.tForegroundPackageCheckStartNanos,
                        timing.tForegroundPackageCheckEndNanos)
                        : 0L)
                .append(" preparedGenerationCheckNanos=")
                .append(delta(timing.tPreparedGenerationCheckStartNanos,
                        timing.tPreparedGenerationCheckEndNanos))
                .append(" preparedWindowCheckNanos=")
                .append(delta(timing.tPreparedWindowCheckStartNanos,
                        timing.tPreparedWindowCheckEndNanos))
                .append(" rootQueryNanos=")
                .append(timing.preparedNodeFastPathUsed || timing.cachedNodeFastPathUsed
                        ? 0L
                        : delta(timing.tRootQueryStartNanos, timing.tRootQueryEndNanos))
                .append(" recordingNodeQueryNanos=")
                .append(timing.preparedNodeFastPathUsed || timing.cachedNodeFastPathUsed
                        ? 0L
                        : delta(timing.tRecordingNodeQueryStartNanos,
                        timing.tRecordingNodeQueryEndNanos))
                .append(" postClickRecStatusQueryNanos=")
                .append(delta(timing.tPostClickRecStatusQueryStartNanos,
                        timing.tPostClickRecStatusQueryEndNanos))
                .append(" nodeValidationNanos=")
                .append(delta(timing.tNodeValidationStartNanos, timing.tNodeValidationEndNanos))
                .append(" performActionCallNanos=")
                .append(delta(timing.tPerformActionStartNanos, timing.tPerformActionEndNanos))
                 .append(" preparedActionToClickNanos=")
                 .append(delta(timing.tPreparedActionReadyNanos,
                         timing.tPerformActionStartNanos))
                .append(" notificationToDetectionNanos=")
                .append(delta(timing.tBleCallbackEnterNanos, timing.tEdgeDetectEndNanos))
                .append(" detectionToSonyClickNanos=")
                .append(delta(timing.tEdgeDetectEndNanos, timing.tPerformActionStartNanos))
                .append(" totalNotificationToSonyClickNanos=")
                .append(delta(timing.tBleCallbackEnterNanos, timing.tPerformActionStartNanos))
                .append(" cacheHitNotificationToSonyClickNanos=")
                .append(timing.cachedNodeFastPathUsed
                        ? delta(timing.tBleCallbackEnterNanos, timing.tPerformActionStartNanos)
                        : -1L)
                .append(" notificationToPerformActionReturnNanos=")
                .append(delta(timing.tBleCallbackEnterNanos, timing.tPerformActionEndNanos))
                .append(" performActionReturnToVerifiedStateNanos=")
                .append(delta(timing.tPerformActionEndNanos, timing.tSonyVerifiedStateNanos));
        log(message.toString());
    }

    private long delta(long startNanos, long endNanos) {
        if (startNanos < 0L || endNanos < 0L || endNanos < startNanos) {
            return -1L;
        }
        return endNanos - startNanos;
    }

    private static final class SonyRecVerification {
        private final boolean fromOm8;
        private final String expectedState;
        private final Om8SonyTriggerTiming timing;
        private final long actionClickNanos;
        private boolean verified;

        private SonyRecVerification(boolean fromOm8,
                                    String expectedState,
                                    Om8SonyTriggerTiming timing,
                                    long actionClickNanos) {
            this.fromOm8 = fromOm8;
            this.expectedState = expectedState;
            this.timing = timing;
            this.actionClickNanos = actionClickNanos;
        }
    }

    private void verifySonyRecState(SonyRecVerification verification, boolean finalAttempt) {
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo recording = null;
        AccessibilityNodeInfo recStatus = null;
        AccessibilityNodeInfo duration = null;
        try {
            root = getRootInActiveWindow();
            if (root == null) {
                log("POST_CLICK root available=false"
                        + " recording.contentDescription=<unavailable>"
                        + " recStatus=<unavailable> duration=<unavailable>");
                if (finalAttempt && verification.fromOm8 && !verification.verified) {
                    log("OM8_SONY_VERIFICATION_FAILED reason=root-unavailable"
                            + " expected=" + verification.expectedState);
                }
                return;
            }
            recording = findFirstNode(root, RECORDING_NODE_ID);
            if (verification.fromOm8) {
                verification.timing.tPostClickRecStatusQueryStartNanos =
                        SystemClock.elapsedRealtimeNanos();
                verification.timing.tRecStatusQueryStartNanos =
                        verification.timing.tPostClickRecStatusQueryStartNanos;
            }
            recStatus = findFirstNode(root, REC_STATUS_NODE_ID);
            if (verification.fromOm8) {
                verification.timing.tPostClickRecStatusQueryEndNanos =
                        SystemClock.elapsedRealtimeNanos();
                verification.timing.tRecStatusQueryEndNanos =
                        verification.timing.tPostClickRecStatusQueryEndNanos;
            }
            duration = findFirstNode(root, DURATION_NODE_ID);
            String observedState = normalizeRecStatus(
                    recStatus == null ? null : recStatus.getText());
            log("POST_CLICK recording.contentDescription="
                    + value(recording == null ? null : recording.getContentDescription())
                    + " recStatus=" + (observedState.isEmpty() ? "<unavailable>" : observedState)
                    + " duration=" + value(duration == null ? null : duration.getText()));

            boolean knownState = isAuthoritativeRecStatus(observedState);
            boolean expectedStateMatches = verification.expectedState == null
                    || verification.expectedState.equals(observedState);
            if (!verification.verified && knownState && expectedStateMatches) {
                verification.verified = true;
                long verifiedStateNanos = SystemClock.elapsedRealtimeNanos();
                if (verification.fromOm8) {
                    verification.timing.tSonyVerifiedStateNanos = verifiedStateNanos;
                    log("SONY_REC_VERIFIED state=" + observedState
                            + " om8PressSequence=" + verification.timing.om8PressSequence
                            + " t_sony_verified_state=" + verifiedStateNanos
                            + " om8NotificationToSonyClickNanos="
                            + delta(verification.timing.tBleCallbackEnterNanos,
                            verification.actionClickNanos)
                            + " om8DetectionToSonyClickNanos="
                            + delta(verification.timing.tEdgeDetectEndNanos,
                            verification.actionClickNanos)
                            + " sonyClickToVerifiedStateNanos="
                            + (verifiedStateNanos - verification.actionClickNanos)
                            + " performActionReturnToVerifiedStateNanos="
                            + delta(verification.timing.tPerformActionEndNanos,
                            verifiedStateNanos));
                    logOm8Timing(verification.timing, "VERIFIED_" + observedState);
                    schedulePreparedRecNodePreparation(verification.timing);
                } else {
                    log("DIAGNOSTIC SONY_REC_VERIFIED state=" + observedState);
                }
            } else if (finalAttempt && !verification.verified && verification.fromOm8) {
                log("OM8_SONY_VERIFICATION_FAILED expected=" + verification.expectedState
                        + " observed="
                        + (observedState.isEmpty() ? "<unavailable>" : observedState));
            }
        } catch (RuntimeException error) {
            log("POST_CLICK tree read failed=" + error.getClass().getSimpleName()
                    + " message=" + error.getMessage());
            if (finalAttempt && verification.fromOm8 && !verification.verified) {
                log("OM8_SONY_VERIFICATION_FAILED reason=tree-read-failed");
            }
        } finally {
            recycleNode(recording);
            recycleNode(recStatus);
            recycleNode(duration);
            if (root != null) {
                root.recycle();
            }
        }
    }

    private String normalizeRecStatus(CharSequence state) {
        return state == null ? "" : state.toString().trim();
    }

    private boolean isAuthoritativeRecStatus(String state) {
        return "REC".equals(state) || "Stby".equals(state);
    }

    private AccessibilityNodeInfo findFirstNode(AccessibilityNodeInfo root, String resourceId) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(resourceId);
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        AccessibilityNodeInfo first = nodes.get(0);
        for (int index = 1; index < nodes.size(); index++) {
            recycleNode(nodes.get(index));
        }
        return first;
    }

    private void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) {
            return;
        }
        for (AccessibilityNodeInfo node : nodes) {
            recycleNode(node);
        }
    }

    private void recycleNode(AccessibilityNodeInfo node) {
        if (node != null) {
            node.recycle();
        }
    }

    private String value(CharSequence value) {
        return value == null ? "<null>" : value.toString();
    }

    private void logKeyEvent(KeyEvent event) {
        if (event == null) {
            log("onKeyEvent keyCode=<null> keyName=<null> action=<null>"
                    + " repeatCount=<null> eventTime=<null> downTime=<null> deviceId=<null>");
            return;
        }
        log("onKeyEvent keyCode=" + event.getKeyCode()
                + " keyName=" + KeyEvent.keyCodeToString(event.getKeyCode())
                + " action=" + event.getAction()
                + " actionName=" + actionName(event.getAction())
                + " repeatCount=" + event.getRepeatCount()
                + " eventTime=" + event.getEventTime()
                + " downTime=" + event.getDownTime()
                + " deviceId=" + event.getDeviceId());
    }

    private boolean isDeviceInteractive() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager != null && powerManager.isInteractive();
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager == null) {
            return true;
        }
        return keyguardManager.isKeyguardLocked()
                || (android.os.Build.VERSION.SDK_INT >= 23 && keyguardManager.isDeviceLocked());
    }

    private ApplicationWindowSnapshot findApplicationWindowSnapshot() {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            String focusedPackage = "";
            int focusedWindowId = UNKNOWN_WINDOW_ID;
            String activePackage = "";
            int activeWindowId = UNKNOWN_WINDOW_ID;
            String firstApplicationPackage = "";
            int firstApplicationWindowId = UNKNOWN_WINDOW_ID;
            for (AccessibilityWindowInfo window : windows) {
                if (window == null || window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) {
                    continue;
                }
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) {
                    continue;
                }
                CharSequence packageName = root.getPackageName();
                if (packageName == null || packageName.length() == 0) {
                    root.recycle();
                    continue;
                }
                String candidate = packageName.toString().trim();
                root.recycle();
                if (VideoProKeyDecision.isSystemUiPackage(candidate)) {
                    continue;
                }
                if (firstApplicationPackage.isEmpty()) {
                    firstApplicationPackage = candidate;
                    firstApplicationWindowId = window.getId();
                }
                if (window.isFocused()) {
                    focusedPackage = candidate;
                    focusedWindowId = window.getId();
                } else if (window.isActive() && activePackage.isEmpty()) {
                    activePackage = candidate;
                    activeWindowId = window.getId();
                }
            }
            if (!focusedPackage.isEmpty()) {
                return new ApplicationWindowSnapshot(focusedPackage, focusedWindowId);
            }
            if (!activePackage.isEmpty()) {
                return new ApplicationWindowSnapshot(activePackage, activeWindowId);
            }
            return new ApplicationWindowSnapshot(firstApplicationPackage, firstApplicationWindowId);
        } catch (RuntimeException error) {
            log("application window lookup failed=" + error.getClass().getSimpleName());
            return new ApplicationWindowSnapshot("", UNKNOWN_WINDOW_ID);
        }
    }

    private void invalidateCachedRecNode() {
        if (cachedRecNode != null) {
            cachedRecNode.recycle();
            cachedRecNode = null;
        }
        cachedRecWindowId = UNKNOWN_WINDOW_ID;
    }

    private void invalidatePreparedRecNode() {
        invalidateSafetySnapshot();
        if (preparedRecNode != null) {
            preparedRecNode.recycle();
            preparedRecNode = null;
        }
        preparedWindowId = UNKNOWN_WINDOW_ID;
        preparedForegroundGeneration = -1L;
        preparedStructuralGeneration = -1L;
        preparedAtElapsedRealtimeNanos = -1L;
    }

    private void invalidateAccessibilityNodeCaches() {
        cancelPreparedRecNodePreparation("structural-safety-invalidation");
        invalidatePreparedRecNode();
        invalidateCachedRecNode();
    }

    private void cancelPreparedRecNodePreparation(String reason) {
        preparationCycle++;
        if (scheduledPreparationRunnable != null) {
            diagnosticHandler.removeCallbacks(scheduledPreparationRunnable);
            scheduledPreparationRunnable = null;
        }
        preparationScheduled = false;
        scheduledPreparationAttempt = 0;
        preparationExtendedRecovery = false;
        lastRearmFailureReason = "";
        lastRearmFailureNanos = -1L;
        Om8SonyTriggerTiming rearmTiming = pendingPreparationTiming;
        pendingPreparationTiming = null;
        if (rearmTiming == null || rearmTiming.postClickRearmResult
                || rearmTiming.tPostClickRearmReadyNanos >= 0L) {
            return;
        }
        rearmTiming.postClickRearmAttempted = true;
        rearmTiming.postClickRearmResult = false;
        rearmTiming.postClickRearmFailureReason = reason;
        rearmTiming.tPostClickRearmEndNanos = SystemClock.elapsedRealtimeNanos();
        log("POST_CLICK_REARM_GAVE_UP reason=" + reason
                + " om8PressSequence=" + rearmTiming.om8PressSequence
                + " timeSinceVerifiedStateNanos="
                + nanosSince(rearmTiming.tSonyVerifiedStateNanos,
                rearmTiming.tPostClickRearmEndNanos)
                + " timeSinceActionReturnNanos="
                + nanosSince(rearmTiming.tPerformActionEndNanos,
                rearmTiming.tPostClickRearmEndNanos));
        logOm8Timing(rearmTiming, "POST_CLICK_REARM_GAVE_UP_" + reason);
    }

    private static final class ApplicationWindowSnapshot {
        private final String packageName;
        private final int windowId;

        private ApplicationWindowSnapshot(String packageName, int windowId) {
            this.packageName = packageName;
            this.windowId = windowId;
        }
    }

    private String actionName(int action) {
        if (action == KeyEvent.ACTION_DOWN) {
            return "ACTION_DOWN";
        }
        if (action == KeyEvent.ACTION_UP) {
            return "ACTION_UP";
        }
        return "ACTION_" + action;
    }

    @Override
    public void onInterrupt() {
        learningSession.cancel();
        cancelPreparedRecNodePreparation("service-interrupted");
        invalidateAccessibilityNodeCaches();
    }

    @Override
    public void onDestroy() {
        learningSession.cancel();
        cancelPreparedRecNodePreparation("service-destroyed");
        invalidateAccessibilityNodeCaches();
        unregisterSafetyEventReceiver();
        if (activeInstance == this) {
            activeInstance = null;
        }
        diagnosticHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
