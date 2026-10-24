package com.androiddjibridge.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.androiddjibridge.sony.VideoProKeyAccessibilityService;

import java.util.ArrayList;
import java.util.List;

/** Small bridge control surface with an explicit, package-scoped Learn Camera flow. */
public final class MainActivity extends Activity {
    private static final int REQUEST_BLUETOOTH = 4101;
    private static final int REQUEST_COMPANION_ASSOCIATION = 4102;
    private TextView status;
    private TextView learningStatus;
    private Spinner profileSpinner;
    private ArrayAdapter<String> profileChoices;
    private final List<CameraProfile> displayedProfiles = new ArrayList<>();
    private boolean verboseDiagnostics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        requestNearbyDevicesIfNeeded();
        refreshProfileList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CompanionAssociationCoordinator.observePresence(this);
        if (profileSpinner != null) {
            refreshProfileList();
            refreshLearningStatus();
            updateStatus("Automatic OM8: " + CompanionAssociationCoordinator.status(this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_COMPANION_ASSOCIATION) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            updateStatus("OM8 association was cancelled; manual Start Bridge remains available.");
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
            updateStatus("OM8 associated; automatic connection wake-up is enabled.");
        } else {
            updateStatus("OM8 association result could not be validated.");
        }
    }

    private LinearLayout buildView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

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

        Button diagnostics = new Button(this);
        diagnostics.setText("Enable OM8 Raw Diagnostics");
        diagnostics.setOnClickListener(view -> {
            verboseDiagnostics = !verboseDiagnostics;
            diagnostics.setText(verboseDiagnostics
                    ? "Disable OM8 Raw Diagnostics" : "Enable OM8 Raw Diagnostics");
            setOm8Diagnostics(verboseDiagnostics);
        });
        root.addView(diagnostics, matchWrap());

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
        return root;
    }

    private void requestNearbyDevicesIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31
                && (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_BLUETOOTH);
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
        refreshProfileList();
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
                .setTitle("Controller mapping for selected profile")
                .setMessage("The mapping selects a semantic camera action. It never contains\n"
                        + "an intent, package, shell command, or Accessibility target.")
                .setView(editor)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", (dialog, which) -> {
                    application.resetMappings();
                    updateStatus("Mappings reset to safe defaults for the selected profile.");
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    ControllerTrigger selectedTrigger = triggers[trigger.getSelectedItemPosition()];
                    CameraAction selectedAction = actions[action.getSelectedItemPosition()];
                    if (application.setMapping(selectedTrigger, selectedAction)) {
                        updateStatus("Mapping saved: " + selectedTrigger + " -> " + selectedAction);
                    } else {
                        updateStatus("Mapping was not saved.");
                    }
                })
                .show();
    }

    private void setOm8Diagnostics(boolean enabled) {
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_SET_DIAGNOSTICS)
                .putExtra(Om8BleListenerService.EXTRA_DIAGNOSTICS_ENABLED, enabled);
        try {
            startService(intent);
            updateStatus("OM8 raw diagnostics " + (enabled ? "enabled" : "disabled")
                    + " for the current listener.");
        } catch (SecurityException error) {
            updateStatus("Could not change OM8 diagnostics: " + error.getMessage());
        }
    }

    private CameraProfile selectedProfile() {
        int position = profileSpinner == null ? -1 : profileSpinner.getSelectedItemPosition();
        return position >= 0 && position < displayedProfiles.size()
                ? displayedProfiles.get(position) : null;
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
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_START_LISTENER);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            updateStatus("Bridge status: STARTING / SCANNING");
        } catch (SecurityException error) {
            updateStatus("Bridge start rejected: " + error.getMessage());
        }
    }

    private void associateOm8() {
        if (!CompanionAssociationCoordinator.beginAssociation(
                this, REQUEST_COMPANION_ASSOCIATION)) {
            updateStatus("OM8 association is unavailable on this Android device; "
                    + "manual Start Bridge remains available.");
        } else {
            updateStatus("Searching for an OM8- device for system association...");
        }
    }

    private void stopBridge() {
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_STOP_LISTENER);
        try {
            startService(intent);
            updateStatus("Bridge status: STOPPED");
        } catch (SecurityException error) {
            updateStatus("Bridge stop rejected: " + error.getMessage());
        }
    }

    private void updateStatus(String value) {
        if (status != null) {
            BridgeApplication application = (BridgeApplication) getApplication();
            status.setText(value + "\nReadiness: "
                    + application.getController().getReadiness().getState()
                    + "\nAutomatic OM8: " + CompanionAssociationCoordinator.status(this));
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
