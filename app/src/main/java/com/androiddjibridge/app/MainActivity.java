package com.androiddjibridge.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraAction;
import com.androiddjibridge.core.CameraProfileCandidate;
import com.androiddjibridge.core.CameraApplicationDiscovery;
import com.androiddjibridge.core.CameraProfileLearningSession;
import com.androiddjibridge.core.CameraProfileOrigin;
import com.androiddjibridge.core.ControllerTrigger;
import com.androiddjibridge.om8.Om8BleListenerService;
import com.androiddjibridge.om8.Om8ConnectionPreferences;
import com.androiddjibridge.om8.Om8ConnectionStatusStore;
import com.androiddjibridge.om8.Om8DiagnosticsPreferences;
import com.androiddjibridge.sony.VideoProKeyAccessibilityService;

import java.util.ArrayList;
import java.util.List;

/** Consumer-facing setup screen; protocol details remain under Advanced / Diagnostics. */
public final class MainActivity extends Activity {
    private static final int REQUEST_BLUETOOTH = 4101;
    private static final int REQUEST_COMPANION_ASSOCIATION = 4102;
    private TextView status;
    private TextView learningStatus;
    private Spinner profileSpinner;
    private ArrayAdapter<String> profileChoices;
    private final List<CameraProfile> displayedProfiles = new ArrayList<>();
    private boolean verboseDiagnostics;
    private TextView diagnosticsState;
    private Button diagnosticsButton;
    private TextView gimbalDevice;
    private Button findGimbalButton;
    private Button disconnectButton;
    private Button forgetGimbalButton;
    private TextView accessibilityHint;
    private Button accessibilityButton;
    private LinearLayout cameraAppsContainer;
    private LinearLayout advancedContainer;
    private boolean advancedExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verboseDiagnostics = Om8DiagnosticsPreferences.isEnabled(this);
        setContentView(buildView());
        requestRuntimePermissionsIfNeeded();
        refreshConsumerUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CompanionAssociationCoordinator.observePresence(this);
        refreshConsumerUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_COMPANION_ASSOCIATION) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            updateStatus("Gimbal selection was cancelled.");
            return;
        }
        BluetoothDevice device;
        if (Build.VERSION.SDK_INT >= 33) {
            device = data.getParcelableExtra(
                    CompanionDeviceManager.EXTRA_DEVICE, BluetoothDevice.class);
        } else {
            device = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
        }
        if (CompanionAssociationCoordinator.handleActivityResult(this, device)) {
            updateStatus("Gimbal selected. Connecting…");
        } else {
            updateStatus("The selected device was not a compatible OM8 gimbal.");
        }
        refreshConsumerUi();
    }

    private ScrollView buildView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("DJI Gimbal Bridge");
        title.setTextSize(26f);
        title.setTextColor(Color.BLACK);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Connect a DJI gimbal once, then use your configured camera app.");
        subtitle.setPadding(0, 8, 0, 24);
        root.addView(subtitle, matchWrap());

        TextView gimbalHeading = new TextView(this);
        gimbalHeading.setText("DJI Gimbal");
        gimbalHeading.setTextSize(19f);
        gimbalHeading.setTextColor(Color.BLACK);
        gimbalHeading.setPadding(0, 12, 0, 8);
        root.addView(gimbalHeading, matchWrap());

        gimbalDevice = new TextView(this);
        gimbalDevice.setTextSize(17f);
        root.addView(gimbalDevice, matchWrap());
        status = new TextView(this);
        status.setPadding(0, 4, 0, 12);
        root.addView(status, matchWrap());

        findGimbalButton = new Button(this);
        findGimbalButton.setText("Find DJI Gimbal");
        findGimbalButton.setOnClickListener(view -> associateOm8());
        root.addView(findGimbalButton, matchWrap());

        disconnectButton = new Button(this);
        disconnectButton.setText("Disconnect");
        disconnectButton.setOnClickListener(view -> stopBridge());
        root.addView(disconnectButton, matchWrap());

        forgetGimbalButton = new Button(this);
        forgetGimbalButton.setText("Forget DJI Gimbal");
        forgetGimbalButton.setOnClickListener(view -> forgetGimbal());
        root.addView(forgetGimbalButton, matchWrap());

        accessibilityHint = new TextView(this);
        accessibilityHint.setText("Camera control needs one Android permission.");
        accessibilityHint.setPadding(0, 20, 0, 4);
        root.addView(accessibilityHint, matchWrap());
        accessibilityButton = new Button(this);
        accessibilityButton.setText("Enable Camera Control");
        accessibilityButton.setOnClickListener(view -> openAccessibilitySettings());
        root.addView(accessibilityButton, matchWrap());

        TextView cameraHeading = new TextView(this);
        cameraHeading.setText("Camera Apps");
        cameraHeading.setTextSize(19f);
        cameraHeading.setTextColor(Color.BLACK);
        cameraHeading.setPadding(0, 24, 0, 8);
        root.addView(cameraHeading, matchWrap());
        TextView cameraHelp = new TextView(this);
        cameraHelp.setText("Each configured camera app has its own mapping preset. "
                + "The foreground camera is selected automatically.");
        root.addView(cameraHelp, matchWrap());
        cameraAppsContainer = new LinearLayout(this);
        cameraAppsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(cameraAppsContainer, matchWrap());

        Button addCamera = new Button(this);
        addCamera.setText("+ Add camera app");
        addCamera.setOnClickListener(view -> chooseCameraApplication());
        root.addView(addCamera, matchWrap());
        Button learn = new Button(this);
        learn.setText("Learn a camera app");
        learn.setOnClickListener(view -> chooseLearningTarget());
        root.addView(learn, matchWrap());
        learningStatus = new TextView(this);
        learningStatus.setPadding(0, 12, 0, 0);
        root.addView(learningStatus, matchWrap());

        Button advanced = new Button(this);
        advanced.setText("Advanced / Diagnostics");
        advanced.setOnClickListener(view -> {
            advancedExpanded = !advancedExpanded;
            advancedContainer.setVisibility(advancedExpanded ? ViewGroup.VISIBLE : ViewGroup.GONE);
        });
        root.addView(advanced, matchWrap());
        advancedContainer = new LinearLayout(this);
        advancedContainer.setOrientation(LinearLayout.VERTICAL);
        advancedContainer.setVisibility(ViewGroup.GONE);
        root.addView(advancedContainer, matchWrap());

        diagnosticsState = new TextView(this);
        advancedContainer.addView(diagnosticsState, matchWrap());
        diagnosticsButton = new Button(this);
        diagnosticsButton.setOnClickListener(view -> {
            verboseDiagnostics = !verboseDiagnostics;
            setOm8Diagnostics(verboseDiagnostics);
        });
        advancedContainer.addView(diagnosticsButton, matchWrap());
        Button reconnect = new Button(this);
        reconnect.setText("Reconnect now");
        reconnect.setOnClickListener(view -> startBridge());
        advancedContainer.addView(reconnect, matchWrap());
        Button review = new Button(this);
        review.setText("Review / Confirm Learned Camera");
        review.setOnClickListener(view -> reviewCandidate());
        advancedContainer.addView(review, matchWrap());
        Button test = new Button(this);
        test.setText("Test Selected Learned Profile");
        test.setOnClickListener(view -> testSelectedProfile());
        advancedContainer.addView(test, matchWrap());
        TextView diagnosticNote = new TextView(this);
        diagnosticNote.setText("Protocol details and raw FFF4 logs are kept here for troubleshooting.");
        advancedContainer.addView(diagnosticNote, matchWrap());
        updateDiagnosticsUi();
        return scroll;
    }

    private ScrollView buildLegacyView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("OM Bridge for Android\nPhase 3B");
        title.setTextSize(24f);
        title.setTextColor(Color.BLACK);
        root.addView(title, matchWrap());

        TextView explanation = new TextView(this);
        explanation.setText("Direct OM8 BLE bridge; DJI Mimo is not required for normal use.\n"
                + "Close or release Mimo if it owns the gimbal connection.\n\n"
                + "Associate OM8 once to let Android wake the bridge when the gimbal appears.\n"
                + "The association wakes the existing GATT driver; it does not itself\n"
                + "connect or subscribe to FFF4. Start Bridge remains a manual recovery path.\n\n"
                + "Choose a validated camera profile. Learn Camera observes only the\n"
                + "explicitly selected app's Accessibility click source; it never uses\n"
                + "coordinates, OCR, screenshots, or arbitrary exported control.\n\n"
                + "Enable the AccessibilityService for camera control and learning.");
        root.addView(explanation, matchWrap());

        TextView profileLabel = new TextView(this);
        profileLabel.setText("Selected camera profile:");
        root.addView(profileLabel, matchWrap());

        profileSpinner = new Spinner(this);
        profileChoices = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>());
        profileChoices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(profileChoices);
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < displayedProfiles.size()) {
                    CameraProfile profile = displayedProfiles.get(position);
                    if (((BridgeApplication) getApplication()).selectProfile(
                            profile.getStableId())) {
                        updateStatus("Profile selected: " + profileSummary(profile));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        root.addView(profileSpinner, matchWrap());

        status = new TextView(this);
        status.setPadding(0, 24, 0, 12);
        root.addView(status, matchWrap());

        Button start = new Button(this);
        start.setText("Start Bridge");
        start.setOnClickListener(view -> startBridge());
        root.addView(start, matchWrap());

        Button associate = new Button(this);
        associate.setText("Associate OM8 for Automatic Connection");
        associate.setOnClickListener(view -> associateOm8());
        root.addView(associate, matchWrap());

        Button reconnect = new Button(this);
        reconnect.setText("Reconnect");
        reconnect.setOnClickListener(view -> startBridge());
        root.addView(reconnect, matchWrap());

        Button stop = new Button(this);
        stop.setText("Stop Bridge");
        stop.setOnClickListener(view -> stopBridge());
        root.addView(stop, matchWrap());

        Button learn = new Button(this);
        learn.setText("Learn Camera");
        learn.setOnClickListener(view -> chooseLearningTarget());
        root.addView(learn, matchWrap());

        Button mappings = new Button(this);
        mappings.setText("Edit Control Mappings");
        mappings.setOnClickListener(view -> showMappingEditor());
        root.addView(mappings, matchWrap());

        TextView diagnosticsHeading = new TextView(this);
        diagnosticsHeading.setText("Advanced / Diagnostics");
        diagnosticsHeading.setTextSize(18f);
        diagnosticsHeading.setPadding(0, 24, 0, 4);
        root.addView(diagnosticsHeading, matchWrap());

        diagnosticsState = new TextView(this);
        root.addView(diagnosticsState, matchWrap());

        diagnosticsButton = new Button(this);
        diagnosticsButton.setOnClickListener(view -> {
            verboseDiagnostics = !verboseDiagnostics;
            setOm8Diagnostics(verboseDiagnostics);
        });
        root.addView(diagnosticsButton, matchWrap());
        updateDiagnosticsUi();

        Button review = new Button(this);
        review.setText("Review / Confirm Candidate");
        review.setOnClickListener(view -> reviewCandidate());
        root.addView(review, matchWrap());

        Button test = new Button(this);
        test.setText("Test Selected Learned Profile");
        test.setOnClickListener(view -> testSelectedProfile());
        root.addView(test, matchWrap());

        learningStatus = new TextView(this);
        learningStatus.setPadding(0, 12, 0, 0);
        root.addView(learningStatus, matchWrap());
        return scroll;
    }

    private void requestRuntimePermissionsIfNeeded() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_BLUETOOTH);
        }
    }

    private void refreshProfileList() {
        if (profileChoices == null) {
            return;
        }
        BridgeApplication application = (BridgeApplication) getApplication();
        String currentId = application.getController().getSelectedProfile() == null
                ? null : application.getController().getSelectedProfile().getStableId();
        displayedProfiles.clear();
        displayedProfiles.addAll(application.getProfiles());
        profileChoices.clear();
        for (CameraProfile profile : displayedProfiles) {
            profileChoices.add(profileSummary(profile));
        }
        profileChoices.notifyDataSetChanged();
        int selected = 0;
        for (int index = 0; index < displayedProfiles.size(); index++) {
            if (displayedProfiles.get(index).getStableId().equals(currentId)) {
                selected = index;
                break;
            }
        }
        if (!displayedProfiles.isEmpty()) {
            profileSpinner.setSelection(selected, false);
        }
    }

    private String profileSummary(CameraProfile profile) {
        return profile.getDisplayName() + " [" + profile.getOrigin().name()
                + ", " + profile.getValidationStatus().name() + "]";
    }

    private void refreshConsumerUi() {
        if (status == null) {
            return;
        }
        Om8ConnectionStatusStore.Snapshot snapshot = Om8ConnectionStatusStore.read(this);
        com.androiddjibridge.core.AssociatedDevice associated =
                new CompanionAssociationStore(this).load();
        String deviceName = value(snapshot.getDeviceName());
        if ("<none>".equals(deviceName) && associated != null) {
            deviceName = value(associated.getDisplayName());
        }
        gimbalDevice.setText(associated == null
                ? "No DJI gimbal selected" : deviceName);
        status.setText(simpleConnectionState(snapshot, associated));
        boolean hasAssociation = associated != null;
        findGimbalButton.setVisibility(hasAssociation ? ViewGroup.GONE : ViewGroup.VISIBLE);
        disconnectButton.setVisibility(hasAssociation ? ViewGroup.VISIBLE : ViewGroup.GONE);
        forgetGimbalButton.setVisibility(hasAssociation ? ViewGroup.VISIBLE : ViewGroup.GONE);

        boolean accessibilityReady = VideoProKeyAccessibilityService.isConnected();
        accessibilityHint.setVisibility(accessibilityReady ? ViewGroup.GONE : ViewGroup.VISIBLE);
        accessibilityButton.setVisibility(accessibilityReady ? ViewGroup.GONE : ViewGroup.VISIBLE);
        refreshCameraApps();
        refreshLearningStatus();
        verboseDiagnostics = Om8DiagnosticsPreferences.isEnabled(this);
        updateDiagnosticsUi();
    }

    private String simpleConnectionState(
            Om8ConnectionStatusStore.Snapshot snapshot,
            com.androiddjibridge.core.AssociatedDevice associated) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && !adapter.isEnabled()) {
                return "Bluetooth off";
            }
        } catch (SecurityException ignored) {
            // Keep the persisted state if the Bluetooth permission is still being requested.
        }
        if (snapshot.isConnected()) {
            return "Connected";
        }
        String state = snapshot.getState();
        if ("SCANNING".equals(state) || "CONNECTING".equals(state)
                || "DISCOVERING".equals(state) || "ENABLING_NOTIFICATIONS".equals(state)) {
            return "Connecting…";
        }
        if ("RETRY_WAIT".equals(state)) {
            return "Reconnecting…";
        }
        return associated == null ? "No DJI gimbal connected" : "Disconnected";
    }

    private void refreshCameraApps() {
        if (cameraAppsContainer == null) {
            return;
        }
        cameraAppsContainer.removeAllViews();
        BridgeApplication application = (BridgeApplication) getApplication();
        CameraProfile selected = application.getController().getSelectedProfile();
        List<CameraProfile> profiles = application.getProfiles();
        if (profiles.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No camera profiles are configured yet.");
            cameraAppsContainer.addView(empty, matchWrap());
            return;
        }
        for (CameraProfile profile : profiles) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 8, 0, 8);
            TextView description = new TextView(this);
            boolean active = selected != null
                    && selected.getStableId().equals(profile.getStableId());
            description.setText(profile.getDisplayName() + "\n"
                    + profile.getPackageName() + "\n"
                    + profile.getOrigin().name() + " / "
                    + profile.getValidationStatus().name()
                    + (active ? "\nActive setup preset" : ""));
            row.addView(description, matchWrap());
            Button mappings = new Button(this);
            mappings.setText("Mappings");
            mappings.setOnClickListener(view -> {
                application.selectProfile(profile.getStableId());
                showMappingEditor(profile);
            });
            row.addView(mappings, matchWrap());
            cameraAppsContainer.addView(row, matchWrap());
        }
    }

    private void chooseCameraApplication() {
        List<CameraApplicationDiscovery.Candidate> choices = installedApplicationChoices();
        if (choices.isEmpty()) {
            updateLearningStatus("No eligible camera applications were discovered.");
            return;
        }
        CharSequence[] labels = new CharSequence[choices.size()];
        for (int index = 0; index < choices.size(); index++) {
            labels[index] = discoveryLabel(choices.get(index));
        }
        new AlertDialog.Builder(this)
                .setTitle("Add camera app")
                .setItems(labels, (dialog, which) -> configureCameraChoice(choices.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void configureCameraChoice(CameraApplicationDiscovery.Candidate choice) {
        BridgeApplication application = (BridgeApplication) getApplication();
        CameraProfile existing = application.getController().getProfileRegistry()
                .findByPackageName(choice.getPackageName()).orElse(null);
        if (existing != null) {
            application.selectProfile(existing.getStableId());
            showMappingEditor(existing);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(choice.getDisplayName())
                .setMessage("This app has no built-in safe adapter. You can learn one "
                        + "Accessibility-visible Record control, then review it before saving.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Learn this app", (dialog, which) -> startLearning(choice))
                .show();
    }

    private void chooseLearningTarget() {
        List<CameraApplicationDiscovery.Candidate> choices = installedApplicationChoices();
        if (choices.isEmpty()) {
            updateLearningStatus("No eligible camera applications found.");
            return;
        }
        CharSequence[] labels = new CharSequence[choices.size()];
        for (int index = 0; index < choices.size(); index++) {
            labels[index] = discoveryLabel(choices.get(index));
        }
        new AlertDialog.Builder(this)
                .setTitle("Select application to learn")
                .setItems(labels, (dialog, which) -> startLearning(choices.get(which)))
                .setNegativeButton("Cancel",
                        (dialog, which) -> VideoProKeyAccessibilityService.cancelLearning())
                .show();
    }

    private void startLearning(CameraApplicationDiscovery.Candidate choice) {
        if (!VideoProKeyAccessibilityService.startLearning(choice.getPackageName())) {
            updateLearningStatus("Learn Mode unavailable. Enable the bridge AccessibilityService first.");
            return;
        }
        updateLearningStatus("LEARN MODE ACTIVE for " + choice.getPackageName()
                + ". Switch to that app and physically tap its Record control once.\n"
                + "Return here and choose Review / Confirm Candidate.");
    }

    private void refreshLearningStatus() {
        CameraProfileLearningSession.Snapshot snapshot =
                VideoProKeyAccessibilityService.getLearningSnapshot();
        if (snapshot.getState() == CameraProfileLearningSession.State.CANDIDATE_CAPTURED) {
            updateLearningStatus("Candidate captured for " + snapshot.getSelectedPackage()
                    + ". Review it before saving.");
        } else if (snapshot.getState() == CameraProfileLearningSession.State.CONFIRMED) {
            updateLearningStatus("Candidate confirmed; save is still required.");
        }
    }

    private void reviewCandidate() {
        CameraProfileLearningSession.Snapshot snapshot =
                VideoProKeyAccessibilityService.getLearningSnapshot();
        CameraProfileCandidate candidate = snapshot.getCandidate();
        if (candidate == null
                || snapshot.getState() == CameraProfileLearningSession.State.WAITING_FOR_CONTROL) {
            updateLearningStatus("No usable candidate is waiting for review.");
            return;
        }
        String details = "Package: " + candidate.getPackageName()
                + "\nResource ID: " + value(candidate.getResourceId())
                + "\nClass: " + value(candidate.getClassName())
                + "\nContent description: " + value(candidate.getContentDescription())
                + "\nText: " + value(candidate.getText())
                + "\nParent resource: " + value(candidate.getParentResourceId())
                + "\nParent class: " + value(candidate.getParentClassName())
                + "\nActions: " + candidate.getSupportedActions()
                + "\nClickable: " + candidate.isClickable()
                + "\nEnabled: " + candidate.isEnabled()
                + "\nVisible: " + candidate.isVisibleToUser()
                + "\nCandidate usable: " + candidate.isStructurallyUsable()
                + "\nState verification: NOT LEARNED";
        new AlertDialog.Builder(this)
                .setTitle("Review learned Record control")
                .setMessage(details)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm & Save", (dialog, which) -> confirmAndSave(snapshot))
                .show();
    }

    private void confirmAndSave(CameraProfileLearningSession.Snapshot snapshot) {
        if (snapshot.getState() != CameraProfileLearningSession.State.CONFIRMED
                && !VideoProKeyAccessibilityService.confirmLearningCandidate()) {
            updateLearningStatus("Candidate confirmation failed; nothing was saved.");
            return;
        }
        CameraProfile profile = VideoProKeyAccessibilityService
                .createConfirmedLearnedProfile(applicationLabel(snapshot.getSelectedPackage()));
        if (profile == null) {
            updateLearningStatus("Profile creation failed; nothing was saved.");
            return;
        }
        if (!((BridgeApplication) getApplication()).saveLearnedProfile(profile)) {
            updateLearningStatus("Profile rejected: duplicate, built-in package, or invalid data.");
            return;
        }
        VideoProKeyAccessibilityService.cancelLearning();
        refreshConsumerUi();
        updateLearningStatus("Saved LEARNED / UNTESTED profile " + profile.getStableId()
                + ". It was not selected automatically.");
    }

    private void testSelectedProfile() {
        CameraProfile profile = selectedProfile();
        if (profile == null) {
            updateLearningStatus("No selected profile.");
            return;
        }
        if (profile.getOrigin() != CameraProfileOrigin.LEARNED) {
            updateLearningStatus("Sony remains on its specialized adapter; use the normal Sony hardware path.");
            return;
        }
        BridgeApplication application = (BridgeApplication) getApplication();
        if (!application.selectProfile(profile.getStableId())
                || !VideoProKeyAccessibilityService.requestGenericProfileTest(profile)) {
            updateLearningStatus("Learned profile test could not be queued.");
            return;
        }
        updateLearningStatus("Profile test requested. It will click only one exact, fresh, "
                + "safety-checked match in " + profile.getPackageName() + ".");
    }

    private void showMappingEditor() {
        BridgeApplication application = (BridgeApplication) getApplication();
        CameraProfile profile = application.getController().getSelectedProfile();
        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(24, 8, 24, 8);
        Spinner trigger = new Spinner(this);
        ControllerTrigger[] triggers = ControllerTrigger.values();
        String[] triggerNames = new String[triggers.length];
        for (int i = 0; i < triggers.length; i++) {
            triggerNames[i] = triggers[i].name();
        }
        trigger.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, triggerNames));
        Spinner action = new Spinner(this);
        CameraAction[] actions = CameraAction.values();
        String[] actionNames = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            actionNames[i] = actions[i].name();
        }
        action.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, actionNames));
        editor.addView(trigger, matchWrap());
        editor.addView(action, matchWrap());
        new AlertDialog.Builder(this)
                .setTitle("Mappings · " + (profile == null
                        ? "camera app" : profile.getDisplayName()))
                .setMessage("The mapping selects a semantic camera action. It never contains\n"
                        + "an intent, package, shell command, or Accessibility target.\n\n"
                        + "Sony Video Pro: Record / Stop is hardware-tested. Side-wheel\n"
                        + "rotation is unavailable, and lens switching remains disabled\n"
                        + "until a safe semantic Accessibility action is evidenced.")
                .setView(editor)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", (dialog, which) -> {
                    application.resetMappings();
                    updateStatus("Mappings reset to safe defaults for the selected profile.");
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    ControllerTrigger selectedTrigger = triggers[trigger.getSelectedItemPosition()];
                    CameraAction selectedAction = actions[action.getSelectedItemPosition()];
                    if (profile != null
                            && "jp.co.sony.mc.videopro".equals(profile.getPackageName())
                            && selectedTrigger == ControllerTrigger.SLIDER_PRESS
                            && selectedAction == CameraAction.SWITCH_LENS) {
                        updateStatus("Sony lens switching remains unavailable without a safe "
                                + "semantic camera action.");
                        return;
                    }
                    if (application.setMapping(selectedTrigger, selectedAction)) {
                        updateStatus("Mapping saved: " + selectedTrigger + " -> " + selectedAction);
                    } else {
                        updateStatus("Mapping was not saved.");
                    }
                })
                .show();
    }

    private void showMappingEditor(CameraProfile profile) {
        ((BridgeApplication) getApplication()).selectProfile(profile.getStableId());
        showMappingEditor();
    }

    private void setOm8Diagnostics(boolean enabled) {
        boolean previous = Om8DiagnosticsPreferences.isEnabled(this);
        if (!Om8DiagnosticsPreferences.setEnabled(this, enabled)) {
            verboseDiagnostics = previous;
            updateDiagnosticsUi();
            updateStatus("Could not persist OM8 raw diagnostic preference.");
            return;
        }
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_SET_DIAGNOSTICS)
                .putExtra(Om8BleListenerService.EXTRA_DIAGNOSTICS_ENABLED, enabled);
        try {
            startService(intent);
            verboseDiagnostics = enabled;
            updateDiagnosticsUi();
            updateStatus("OM8 raw diagnostics " + (enabled ? "enabled" : "disabled")
                    + ". Complete FFF4 packets will be logged.");
        } catch (RuntimeException error) {
            Om8DiagnosticsPreferences.setEnabled(this, previous);
            verboseDiagnostics = previous;
            updateDiagnosticsUi();
            updateStatus("Could not change OM8 diagnostics: " + error.getMessage());
        }
    }

    private void updateDiagnosticsUi() {
        if (diagnosticsState != null) {
            diagnosticsState.setText("OM8 Raw Diagnostics: "
                    + (verboseDiagnostics ? "ON" : "OFF"));
        }
        if (diagnosticsButton != null) {
            diagnosticsButton.setText(verboseDiagnostics
                    ? "Disable OM8 Raw Diagnostics" : "Enable OM8 Raw Diagnostics");
        }
    }

    private CameraProfile selectedProfile() {
        return ((BridgeApplication) getApplication()).getController().getSelectedProfile();
    }

    private List<CameraApplicationDiscovery.Candidate> installedApplicationChoices() {
        List<CameraApplicationDiscovery.Descriptor> descriptors = new ArrayList<>();
        collectIntentCandidates(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                CameraApplicationDiscovery.Source.LAUNCHABLE, descriptors);
        collectIntentCandidates(
                new Intent("android.media.action.VIDEO_CAPTURE"),
                CameraApplicationDiscovery.Source.VIDEO_CAPTURE, descriptors);
        collectIntentCandidates(
                new Intent("android.media.action.IMAGE_CAPTURE"),
                CameraApplicationDiscovery.Source.IMAGE_CAPTURE, descriptors);
        addBuiltInProfileCandidates(descriptors);
        return CameraApplicationDiscovery.merge(descriptors, getPackageName());
    }

    private void collectIntentCandidates(
            Intent intent,
            CameraApplicationDiscovery.Source source,
            List<CameraApplicationDiscovery.Descriptor> descriptors) {
        for (ResolveInfo info : getPackageManager().queryIntentActivities(intent, 0)) {
            if (info == null || info.activityInfo == null) {
                continue;
            }
            ApplicationInfo applicationInfo = info.activityInfo.applicationInfo;
            CharSequence label = info.loadLabel(getPackageManager());
            descriptors.add(new CameraApplicationDiscovery.Descriptor(
                    info.activityInfo.packageName,
                    label == null ? info.activityInfo.packageName : label.toString(),
                    source,
                    applicationInfo != null
                            && (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
        }
    }

    private void addBuiltInProfileCandidates(
            List<CameraApplicationDiscovery.Descriptor> descriptors) {
        BridgeApplication application = (BridgeApplication) getApplication();
        for (CameraProfile profile : application.getProfiles()) {
            if (profile.getOrigin() != CameraProfileOrigin.BUILTIN) {
                continue;
            }
            try {
                ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                        profile.getPackageName(), 0);
                CharSequence label = getPackageManager().getApplicationLabel(applicationInfo);
                descriptors.add(new CameraApplicationDiscovery.Descriptor(
                        profile.getPackageName(),
                        label == null ? profile.getDisplayName() : label.toString(),
                        CameraApplicationDiscovery.Source.BUILTIN_PROFILE,
                        (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
            } catch (PackageManager.NameNotFoundException ignored) {
                // The built-in profile is retained in the profile registry but is not
                // offered for learning when its application is not installed.
            }
        }
    }

    private String discoveryLabel(CameraApplicationDiscovery.Candidate candidate) {
        StringBuilder result = new StringBuilder()
                .append(candidate.getDisplayName())
                .append("\n")
                .append(candidate.getPackageName())
                .append(" [");
        boolean first = true;
        if (candidate.hasSource(CameraApplicationDiscovery.Source.VIDEO_CAPTURE)
                || candidate.hasSource(CameraApplicationDiscovery.Source.IMAGE_CAPTURE)) {
            result.append("CAMERA INTENT");
            first = false;
        }
        if (candidate.hasSource(CameraApplicationDiscovery.Source.LAUNCHABLE)) {
            if (!first) {
                result.append(", ");
            }
            result.append("LAUNCHABLE");
            first = false;
        }
        if (candidate.hasSource(CameraApplicationDiscovery.Source.BUILTIN_PROFILE)) {
            if (!first) {
                result.append(", ");
            }
            result.append("BUILT-IN PROFILE");
        }
        return result.append(']').toString();
    }

    private String applicationLabel(String packageName) {
        if (packageName == null) {
            return "Learned camera";
        }
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException error) {
            return packageName;
        }
    }

    private void startBridge() {
        Om8ConnectionPreferences.setManualDisconnect(this, false);
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_START_LISTENER);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            updateStatus("Connecting…");
        } catch (SecurityException error) {
            updateStatus("Bridge start rejected: " + error.getMessage());
        }
    }

    private void associateOm8() {
        if (!CompanionAssociationCoordinator.beginAssociation(
                this, REQUEST_COMPANION_ASSOCIATION)) {
            updateStatus("Device association is unavailable here. "
                    + "Use Advanced / Diagnostics to reconnect manually.");
        } else {
            updateStatus("Searching for nearby OM8 gimbals…");
        }
    }

    private void stopBridge() {
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_STOP_LISTENER);
        try {
            startService(intent);
            updateStatus("Disconnected");
        } catch (SecurityException error) {
            updateStatus("Bridge stop rejected: " + error.getMessage());
        }
    }

    private void forgetGimbal() {
        stopBridge();
        Om8ConnectionPreferences.setManualDisconnect(this, true);
        new CompanionAssociationStore(this).clear();
        updateStatus("Gimbal forgotten.");
        refreshConsumerUi();
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (RuntimeException error) {
            updateStatus("Android Accessibility settings could not be opened.");
        }
    }

    private void updateStatus(String value) {
        if (status != null) {
            status.setText(value);
        }
    }

    private void updateLearningStatus(String value) {
        if (learningStatus != null) {
            learningStatus.setText(value);
        }
    }

    private String value(String value) {
        return value == null || value.isEmpty() ? "<none>" : value;
    }

    private ViewGroup.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

}
