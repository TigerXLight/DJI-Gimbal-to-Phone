package com.androiddjibridge.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androiddjibridge.om8.Om8BleListenerService;

/** Small Phase 1 control surface; it intentionally exposes no DJI command controls. */
public final class MainActivity extends Activity {
    private static final int REQUEST_BLUETOOTH = 4101;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        requestNearbyDevicesIfNeeded();
    }

    private LinearLayout buildView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("OM Bridge for Android\nPhase 1");
        title.setTextSize(24f);
        title.setTextColor(Color.BLACK);
        root.addView(title, matchWrap());

        TextView explanation = new TextView(this);
        explanation.setText("Direct OM8 BLE bridge; DJI Mimo is not required for normal use.\n"
                + "Close or release Mimo if it owns the gimbal connection.\n\n"
                + "Unofficial project. Not affiliated with or endorsed by DJI.\n"
                + "DJI and Osmo are trademarks of their respective owner.\n\n"
                + "Current adapter: Sony Video Pro (jp.co.sony.mc.videopro)\n"
                + "Enable the AccessibilityService for Sony control.");
        root.addView(explanation, matchWrap());

        status = new TextView(this);
        status.setText("Bridge status: STOPPED\nGimbal driver: STOPPED\n"
                + "Camera profile: Sony Video Pro (selected)\n"
                + "Camera adapter: waiting for AccessibilityService");
        status.setPadding(0, 24, 0, 24);
        root.addView(status, matchWrap());

        Button start = new Button(this);
        start.setText("Start Bridge");
        start.setOnClickListener(view -> startBridge());
        root.addView(start, matchWrap());

        Button reconnect = new Button(this);
        reconnect.setText("Reconnect");
        reconnect.setOnClickListener(view -> startBridge());
        root.addView(reconnect, matchWrap());

        Button stop = new Button(this);
        stop.setText("Stop Bridge");
        stop.setOnClickListener(view -> stopBridge());
        root.addView(stop, matchWrap());
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

    private void startBridge() {
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_START_LISTENER);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            status.setText("Bridge status: STARTING / SCANNING\n"
                    + "Gimbal driver: scanning for OM8-\n"
                    + "Camera profile: Sony Video Pro (selected)\n"
                    + "Complete bridge: waiting for gimbal + camera service");
        } catch (SecurityException error) {
            status.setText("Bridge start rejected: " + error.getMessage());
        }
    }

    private void stopBridge() {
        Intent intent = new Intent(this, Om8BleListenerService.class)
                .setAction(Om8BleListenerService.ACTION_STOP_LISTENER);
        try {
            startService(intent);
            status.setText("Bridge status: STOPPED\nGimbal driver: STOPPED\n"
                    + "Camera profile: Sony Video Pro (selected)\n"
                    + "Camera adapter: unavailable");
        } catch (SecurityException error) {
            status.setText("Bridge stop rejected: " + error.getMessage());
        }
    }

    private ViewGroup.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
