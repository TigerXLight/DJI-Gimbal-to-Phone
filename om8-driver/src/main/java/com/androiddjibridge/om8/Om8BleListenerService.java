package com.androiddjibridge.om8;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.androiddjibridge.core.ControllerEvent;
import com.androiddjibridge.core.ControllerEventSink;
import com.androiddjibridge.core.GimbalDriver;
import com.androiddjibridge.core.GimbalProfile;
import com.androiddjibridge.core.BridgeRuntime;
import com.androiddjibridge.core.BridgeController;
import com.androiddjibridge.core.ReconnectBackoff;

import java.util.Locale;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Temporary receive-only DJI OM8 listener. It never writes DJI command data;
 * the only GATT write is the standard notification CCCD enablement.
 */
public final class Om8BleListenerService extends Service implements GimbalDriver {
    private static final String TAG = "Om8BleListenerService";
    public static final String ACTION_START_LISTENER =
            "com.androiddjibridge.OM8_LISTENER_START";
    public static final String ACTION_AUTO_START_LISTENER =
            "com.androiddjibridge.OM8_LISTENER_AUTO_START";
    public static final String ACTION_STOP_LISTENER =
            "com.androiddjibridge.OM8_LISTENER_STOP";
    public static final String ACTION_SET_DIAGNOSTICS =
            "com.androiddjibridge.OM8_SET_DIAGNOSTICS";
    public static final String EXTRA_DIAGNOSTICS_ENABLED =
            "com.androiddjibridge.extra.DIAGNOSTICS_ENABLED";
    public static final String EXTRA_START_REASON =
            "com.androiddjibridge.extra.START_REASON";

    private static final int NOTIFICATION_ID = 4201;
    private static final String CHANNEL_ID = "om8_listener";
    private static final long SCAN_TIMEOUT_MS = 15_000L;
    private static final long RETRY_BASE_DELAY_MS = 1_000L;
    private static final long RETRY_MAX_DELAY_MS = 30_000L;
    private static final long NOTIFICATION_STALL_TIMEOUT_MS = 10_000L;
    private static final long NOTIFICATION_WATCHDOG_PERIOD_MS = 5_000L;
    private static final String OM8_NAME_PREFIX = "OM8-";
    private static final UUID OM8_SERVICE_UUID = UUID.fromString(
            "0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID OM8_NOTIFICATION_UUID = UUID.fromString(
            "0000fff4-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CONFIGURATION_UUID = UUID.fromString(
            "00002902-0000-1000-8000-00805f9b34fb");

    private final IBinder binder = new LocalBinder();
    private Handler handler;
    private BridgeDiagnosticLogger logger;
    private volatile ControllerEventSink eventSink;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private volatile BluetoothGatt gatt;
    private BluetoothGattCharacteristic notificationCharacteristic;
    private ScanCallback scanCallback;
    private final Om8RecButtonEdgeDetector edgeDetector = new Om8RecButtonEdgeDetector();
    private long om8PressSequence;
    private long lastOm8PressSequence = -1L;

    private boolean foregroundStarted;
    private volatile boolean listenerReady;
    private volatile boolean verboseDiagnostics;

    private enum ListenerState {
        DISCONNECTED,
        SCANNING,
        CONNECTING,
        DISCOVERING,
        ENABLING_NOTIFICATIONS,
        READY,
        RETRY_WAIT,
        STOPPED
    }

    private volatile ListenerState listenerState = ListenerState.STOPPED;
    private boolean listenerRequested;
    private boolean selected;
    private int scanAttempt;
    private int retryAttempt;
    private long scanStartedNanos = -1L;
    private long disconnectNanos = -1L;
    private long rediscoveredNanos = -1L;
    private long gattConnectedNanos = -1L;
    private long readyNanos = -1L;
    private long gattGenerationCounter;
    private long currentGattGeneration = -1L;
    private boolean connectionPriorityHighRequested;
    private boolean connectionPriorityHighRequestResult;
    private volatile long lastNotificationNanos = -1L;
    private Runnable scanTimeoutRunnable;
    private Runnable retryRunnable;
    private Runnable notificationWatchdogRunnable;
    private BluetoothDevice lastSeenDevice;
    private String lastSeenName = "";
    private String lastSeenAddress = "";

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_OFF
                    || state == BluetoothAdapter.STATE_TURNING_OFF) {
                if (listenerRequested) {
                    log("OM8 Bluetooth unavailable/off; cancelling scan and retry work");
                    listenerReady = false;
                    cancelRetry();
                    stopScan();
                    disposeCurrentGatt();
                    setListenerState(ListenerState.DISCONNECTED);
                }
            } else if (state == BluetoothAdapter.STATE_ON && listenerRequested) {
                log("OM8 Bluetooth available; resuming listener recovery");
                scheduleRetry("bluetooth-on", 0L);
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt callbackGatt, int status, int newState) {
            postLifecycle(() -> handleConnectionStateChange(callbackGatt, status, newState));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt callbackGatt, int status) {
            postLifecycle(() -> handleServicesDiscovered(callbackGatt, status));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt callbackGatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status) {
            postLifecycle(() -> handleDescriptorWrite(callbackGatt, descriptor, status));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt callbackGatt,
                                             BluetoothGattCharacteristic characteristic) {
            long callbackEnter = SystemClock.elapsedRealtimeNanos();
            boolean callbackOnMainLooper = Looper.myLooper() == Looper.getMainLooper();
            long callbackThreadId = Thread.currentThread().getId();
            if (callbackGatt == gatt && listenerReady
                    && characteristic != null
                    && OM8_NOTIFICATION_UUID.equals(characteristic.getUuid())) {
                lastNotificationNanos = callbackEnter;
                handleNotification(characteristic.getValue(), callbackEnter,
                        callbackOnMainLooper, callbackThreadId);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt callbackGatt,
                                             BluetoothGattCharacteristic characteristic,
                                             byte[] value) {
            long callbackEnter = SystemClock.elapsedRealtimeNanos();
            boolean callbackOnMainLooper = Looper.myLooper() == Looper.getMainLooper();
            long callbackThreadId = Thread.currentThread().getId();
            if (callbackGatt == gatt && listenerReady
                    && characteristic != null
                    && OM8_NOTIFICATION_UUID.equals(characteristic.getUuid())) {
                lastNotificationNanos = callbackEnter;
                handleNotification(value, callbackEnter,
                        callbackOnMainLooper, callbackThreadId);
            }
        }
    };

    private void handleConnectionStateChange(BluetoothGatt callbackGatt,
                                              int status,
                                              int newState) {
        log("GATT connection state status=" + status + " newState=" + newState);
        if (callbackGatt != gatt) {
            log("GATT stale callback ignored");
            return;
        }
        if (newState == BluetoothProfile.STATE_CONNECTED
                && status == BluetoothGatt.GATT_SUCCESS) {
            listenerReady = false;
            notificationCharacteristic = null;
            gattConnectedNanos = SystemClock.elapsedRealtimeNanos();
            setListenerState(ListenerState.DISCOVERING);
            log("GATT connected");
            requestHighConnectionPriority(callbackGatt);
            boolean requested;
            try {
                requested = callbackGatt.discoverServices();
            } catch (SecurityException error) {
                failCurrentGatt("services-discovery-permission-failure");
                return;
            } catch (RuntimeException error) {
                failCurrentGatt("services-discovery-" + error.getClass().getSimpleName());
                return;
            }
            log("services discovery requested=" + requested);
            if (!requested) {
                failCurrentGatt("services-discovery-rejected");
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED
                || status != BluetoothGatt.GATT_SUCCESS) {
            failCurrentGatt(newState == BluetoothProfile.STATE_DISCONNECTED
                    ? "gatt-disconnected"
                    : "gatt-error");
        }
    }

    private void handleServicesDiscovered(BluetoothGatt callbackGatt, int status) {
        log("services discovered status=" + status);
        if (callbackGatt != gatt) {
            log("GATT stale services callback ignored");
            return;
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            failCurrentGatt("services-discovery-status-" + status);
            return;
        }

        try {
        BluetoothGattService service = callbackGatt.getService(OM8_SERVICE_UUID);
        log("FFF0 found=" + (service != null));
        if (service == null) {
            failCurrentGatt("fff0-not-found");
            return;
        }

        notificationCharacteristic = service.getCharacteristic(OM8_NOTIFICATION_UUID);
        log("FFF4 found=" + (notificationCharacteristic != null)
                + (notificationCharacteristic == null
                ? ""
                : " properties=0x" + Integer.toHexString(
                notificationCharacteristic.getProperties())));
        if (notificationCharacteristic == null) {
            failCurrentGatt("fff4-not-found");
            return;
        }

        int properties = notificationCharacteristic.getProperties();
        boolean notify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        boolean indicate = (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
        if (!notify && !indicate) {
            failCurrentGatt("fff4-notifiable-property-missing");
            return;
        }

        setListenerState(ListenerState.ENABLING_NOTIFICATIONS);
        boolean localEnabled;
        try {
            localEnabled = callbackGatt.setCharacteristicNotification(
                    notificationCharacteristic, true);
        } catch (SecurityException error) {
            failCurrentGatt("notification-registration-permission-failure");
            return;
        }
        log("notification registration result=" + localEnabled);
        if (!localEnabled) {
            failCurrentGatt("notification-registration-rejected");
            return;
        }

        BluetoothGattDescriptor cccd = notificationCharacteristic.getDescriptor(
                CLIENT_CONFIGURATION_UUID);
        if (cccd == null) {
            failCurrentGatt("fff4-cccd-not-found");
            return;
        }
        byte[] enableValue = notify
                ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        boolean submitted;
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                submitted = callbackGatt.writeDescriptor(cccd, enableValue)
                        == BluetoothStatusCodes.SUCCESS;
            } else {
                cccd.setValue(enableValue);
                submitted = callbackGatt.writeDescriptor(cccd);
            }
        } catch (SecurityException error) {
            failCurrentGatt("cccd-write-permission-failure");
            return;
        }
        log("CCCD write submitted=" + submitted
            + " value=" + BridgeDiagnosticLogger.toHex(enableValue));
        if (!submitted) {
            failCurrentGatt("cccd-write-rejected");
        }
        } catch (SecurityException error) {
            failCurrentGatt("gatt-setup-permission-failure");
        } catch (RuntimeException error) {
            failCurrentGatt("gatt-setup-" + error.getClass().getSimpleName());
        }
    }

    private void handleDescriptorWrite(BluetoothGatt callbackGatt,
                                       BluetoothGattDescriptor descriptor,
                                       int status) {
        log("CCCD write result status=" + status
                + " uuid=" + (descriptor == null ? "<null>" : descriptor.getUuid()));
        if (callbackGatt != gatt || descriptor == null
                || !CLIENT_CONFIGURATION_UUID.equals(descriptor.getUuid())) {
            return;
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            listenerReady = true;
            readyNanos = SystemClock.elapsedRealtimeNanos();
            lastNotificationNanos = -1L;
            retryAttempt = 0;
            setListenerState(ListenerState.READY);
            log("OM8 LISTENER READY service=" + OM8_SERVICE_UUID
                    + " notificationCharacteristic=" + OM8_NOTIFICATION_UUID
                    + " gattGeneration=" + currentGattGeneration
                    + " connectionPriorityHighRequested="
                    + connectionPriorityHighRequested
                    + " connectionPriorityHighRequestResult="
                    + connectionPriorityHighRequestResult
                    + " timeSinceDisconnectNanos=" + elapsedSince(disconnectNanos, readyNanos)
                    + " timeToRediscoveryNanos=" + elapsedSince(disconnectNanos, rediscoveredNanos)
                    + " timeToGattConnectedNanos=" + elapsedSince(rediscoveredNanos, gattConnectedNanos)
                    + " timeToReadyNanos=" + elapsedSince(rediscoveredNanos, readyNanos));
            updateNotification("OM8 listener ready");
            scheduleNotificationWatchdog();
        } else {
            failCurrentGatt("cccd-write-status-" + status);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        logger = new BridgeDiagnosticLogger(this);
        IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(bluetoothStateReceiver, bluetoothFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(bluetoothStateReceiver, bluetoothFilter);
        }
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = manager == null ? null : manager.getAdapter();
        createNotificationChannel();

        if (!hasBluetoothPermissions()) {
            log("OM8 listener unavailable: BLUETOOTH_SCAN/BLUETOOTH_CONNECT not granted");
            stopSelf();
            return;
        }
        if (Build.VERSION.SDK_INT >= 34
                && checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
                != PackageManager.PERMISSION_GRANTED) {
            log("OM8 listener unavailable: FOREGROUND_SERVICE_CONNECTED_DEVICE not granted");
            stopSelf();
            return;
        }

        try {
            startForeground(NOTIFICATION_ID, buildNotification("OM8 listener starting"));
            foregroundStarted = true;
        } catch (SecurityException error) {
            log("OM8 listener foreground start failed=" + error.getMessage());
            stopSelf();
            return;
        }
        log("OM8 listener service created log=" + logger.getLogFile().getAbsolutePath());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!foregroundStarted) {
            log("OM8 listener command ignored because foreground service did not start");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_START_LISTENER.equals(intent.getAction())) {
            beginListener();
        } else if (intent != null && ACTION_AUTO_START_LISTENER.equals(intent.getAction())) {
            log("OM8 automatic listener start reason="
                    + intent.getStringExtra(EXTRA_START_REASON));
            beginListenerIfNeeded();
        } else if (intent != null && ACTION_STOP_LISTENER.equals(intent.getAction())) {
            log("OM8 listener stop requested");
            stopListener("explicit-stop");
            stopSelf(startId);
        } else if (intent != null && ACTION_SET_DIAGNOSTICS.equals(intent.getAction())) {
            verboseDiagnostics = intent.getBooleanExtra(EXTRA_DIAGNOSTICS_ENABLED, false);
            log("OM8 diagnostics enabled=" + verboseDiagnostics);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        listenerRequested = false;
        cancelRetry();
        stopScan();
        listenerReady = false;
        notificationCharacteristic = null;
        disposeCurrentGatt();
        setListenerState(ListenerState.STOPPED);
        try {
            unregisterReceiver(bluetoothStateReceiver);
        } catch (IllegalArgumentException ignored) {
            // The receiver may not have been registered if service startup failed early.
        }
        super.onDestroy();
    }

    public final class LocalBinder extends Binder {
        public Om8BleListenerService getService() {
            return Om8BleListenerService.this;
        }
    }

    @Override
    public String id() {
        return "dji-om8";
    }

    @Override
    public String displayName() {
        return "DJI Osmo Mobile 8 / OM8";
    }

    @Override
    public GimbalProfile profile() {
        return Om8GimbalProfile.proven();
    }

    @Override
    public void start() {
        beginListener();
    }

    @Override
    public void stop() {
        stopListener("driver-stop");
    }

    @Override
    public GimbalDriver.ConnectionState connectionState() {
        switch (listenerState) {
            case SCANNING:
                return ConnectionState.SCANNING;
            case CONNECTING:
                return ConnectionState.CONNECTING;
            case DISCOVERING:
                return ConnectionState.DISCOVERING;
            case ENABLING_NOTIFICATIONS:
                return ConnectionState.ENABLING_NOTIFICATIONS;
            case READY:
                return ConnectionState.READY;
            case RETRY_WAIT:
                return ConnectionState.RETRY_WAIT;
            case DISCONNECTED:
                return ConnectionState.DISCONNECTED;
            case STOPPED:
            default:
                return ConnectionState.STOPPED;
        }
    }

    @Override
    public EnumSet<GimbalDriver.Capability> supportedCapabilities() {
        return EnumSet.of(GimbalDriver.Capability.RECORD);
    }

    @Override
    public void setEventSink(ControllerEventSink sink) {
        eventSink = sink;
    }

    private void beginListener() {
        listenerRequested = true;
        cancelRetry();
        if (!hasBluetoothPermissions()) {
            logFailure("OM8 listener cannot scan without BLUETOOTH_SCAN and BLUETOOTH_CONNECT");
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            logFailure("OM8 listener cannot scan because Bluetooth is disabled");
            setListenerState(ListenerState.DISCONNECTED);
            return;
        }
        stopScan();
        disposeCurrentGatt();
        notificationCharacteristic = null;
        selected = false;
        listenerReady = false;
        edgeDetector.update(false);
        retryAttempt = 0;
        scanAttempt = 0;
        startScanAttempt();
    }

    private void beginListenerIfNeeded() {
        if (listenerRequested) {
            log("OM8 automatic listener start ignored; existing lifecycle active state="
                    + listenerState);
            return;
        }
        beginListener();
    }

    private void startScanAttempt() {
        if (!listenerRequested) {
            return;
        }
        if (!hasBluetoothPermissions()) {
            listenerReady = false;
            setListenerState(ListenerState.DISCONNECTED);
            logFailure("OM8 listener cannot scan without BLUETOOTH_SCAN and BLUETOOTH_CONNECT");
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            listenerReady = false;
            setListenerState(ListenerState.DISCONNECTED);
            log("OM8 Bluetooth unavailable/off; retry is paused until Bluetooth is enabled");
            return;
        }
        if (scanCallback != null || gatt != null
                || listenerState == ListenerState.CONNECTING
                || listenerState == ListenerState.DISCOVERING
                || listenerState == ListenerState.ENABLING_NOTIFICATIONS
                || listenerState == ListenerState.READY) {
            log("OM8 recovery start ignored; scan or GATT operation already active state="
                    + listenerState);
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            scheduleRetry("scanner-unavailable", nextRetryDelayMs());
            return;
        }

        selected = false;
        scanAttempt++;
        scanStartedNanos = SystemClock.elapsedRealtimeNanos();
        setListenerState(ListenerState.SCANNING);
        updateNotification("Scanning for OM8-");
        log("OM8 scan started attempt=" + scanAttempt
                + " namePrefix=" + OM8_NAME_PREFIX
                + " service=" + OM8_SERVICE_UUID
                + " notification=" + OM8_NOTIFICATION_UUID
                + " timeSinceDisconnectNanos="
                + elapsedSince(disconnectNanos, scanStartedNanos)
                + " lastSeenName=" + (lastSeenName.isEmpty() ? "<none>" : lastSeenName)
                + " lastSeenAddress=" + (lastSeenAddress.isEmpty()
                ? "<none>" : lastSeenAddress));

        ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                postLifecycle(() -> handleScanResult(this, result));
            }

            @Override
            public void onScanFailed(int errorCode) {
                postLifecycle(() -> handleScanFailure(this, errorCode));
            }
        };
        scanCallback = callback;
        try {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanner.startScan(null, settings, callback);
            scanTimeoutRunnable = () -> handleScanTimeout(callback);
            handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);
        } catch (SecurityException error) {
            if (scanCallback == callback) {
                scanCallback = null;
            }
            logFailure("OM8 scan permission failure=" + error.getMessage());
            scheduleRetry("scan-permission-failure", nextRetryDelayMs());
        } catch (RuntimeException error) {
            if (scanCallback == callback) {
                scanCallback = null;
            }
            logFailure("OM8 scan start failed=" + error.getClass().getSimpleName());
            scheduleRetry("scan-start-failure", nextRetryDelayMs());
        }
    }

    private void handleScanTimeout(ScanCallback callback) {
        if (!listenerRequested || callback != scanCallback
                || listenerState != ListenerState.SCANNING || selected || gatt != null) {
            return;
        }
        long durationNanos = elapsedSince(scanStartedNanos, SystemClock.elapsedRealtimeNanos());
        stopScan();
        listenerReady = false;
        setListenerState(ListenerState.DISCONNECTED);
        log("OM8_UNAVAILABLE_NOT_ADVERTISING"
                + " scan timed out; device is not currently advertising"
                + " possible causes=powered-off,out-of-range,another-client-such-as-DJI-Mimo"
                + " scanAttempt=" + scanAttempt
                + " scanDurationNanos=" + durationNanos);
        scheduleRetry("not-advertising", nextRetryDelayMs());
    }

    private void handleScanFailure(ScanCallback callback, int errorCode) {
        if (!listenerRequested || callback != scanCallback) {
            return;
        }
        long durationNanos = elapsedSince(scanStartedNanos, SystemClock.elapsedRealtimeNanos());
        stopScan();
        listenerReady = false;
        setListenerState(ListenerState.DISCONNECTED);
        logFailure("OM8 scan failed status=" + errorCode
                + " scanAttempt=" + scanAttempt
                + " scanDurationNanos=" + durationNanos);
        scheduleRetry("scan-failed-" + errorCode, nextRetryDelayMs());
    }

    private void handleScanResult(ScanCallback callback, ScanResult result) {
        if (callback != scanCallback || result == null || selected || !listenerRequested
                || listenerState != ListenerState.SCANNING) {
            return;
        }
        BluetoothDevice device = result.getDevice();
        String name = null;
        try {
            name = device.getName();
        } catch (SecurityException error) {
            log("OM8 scan result name permission failure=" + error.getMessage());
        }
        if (name == null && result.getScanRecord() != null) {
            name = result.getScanRecord().getDeviceName();
        }
        if (name == null) {
            return;
        }
        String trimmedName = name.trim();
        String upperName = trimmedName.toUpperCase(Locale.US);
        if (!upperName.startsWith(OM8_NAME_PREFIX)) {
            return;
        }

        String address;
        try {
            address = device.getAddress();
        } catch (SecurityException error) {
            address = "<permission-error>";
        }
        log("OM8 scan result name=" + trimmedName
                + " address=" + address
                + " rssi=" + result.getRssi());
        selected = true;
        rediscoveredNanos = SystemClock.elapsedRealtimeNanos();
        lastSeenDevice = device;
        lastSeenName = trimmedName;
        lastSeenAddress = address;
        stopScan();
        connectToCandidate(device, trimmedName, address);
    }

    private void connectToCandidate(BluetoothDevice device, String name, String address) {
        if (!listenerRequested || gatt != null) {
            log("OM8 GATT connection skipped; listener or GATT already active");
            return;
        }
        setListenerState(ListenerState.CONNECTING);
        log("OM8 selected device name=" + name + " address=" + address);
        updateNotification("Connecting to " + name);
        log("GATT connecting");
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                gatt = device.connectGatt(
                        this,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE,
                        BluetoothDevice.PHY_LE_1M_MASK,
                        handler);
                log("GATT callback handler=main transport=LE phy=LE_1M");
            } else {
                gatt = device.connectGatt(
                        this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                log("GATT callback handler=platform-default transport=LE");
            }
            if (gatt != null) {
                currentGattGeneration = ++gattGenerationCounter;
                connectionPriorityHighRequested = false;
                connectionPriorityHighRequestResult = false;
                log("GATT object created generation=" + currentGattGeneration
                        + " identity=" + System.identityHashCode(gatt));
            }
        } catch (SecurityException error) {
            logFailure("GATT connecting permission failure=" + error.getMessage());
            failCurrentGatt("gatt-connect-permission-failure");
            return;
        }
        if (gatt == null) {
            logFailure("GATT connecting returned null");
            failCurrentGatt("gatt-connect-returned-null");
        }
    }

    private void requestHighConnectionPriority(BluetoothGatt callbackGatt) {
        if (callbackGatt == null || callbackGatt != gatt) {
            log("OM8_CONNECTION_PRIORITY_REQUEST ignored reason=stale-gatt");
            return;
        }
        if (connectionPriorityHighRequested) {
            return;
        }
        connectionPriorityHighRequested = true;
        boolean submitted = false;
        try {
            submitted = callbackGatt.requestConnectionPriority(
                    BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        } catch (SecurityException error) {
            log("OM8_CONNECTION_PRIORITY_REQUEST priority=HIGH"
                    + " submitted=false reason=permission-failure");
        } catch (RuntimeException error) {
            log("OM8_CONNECTION_PRIORITY_REQUEST priority=HIGH"
                    + " submitted=false reason=" + error.getClass().getSimpleName());
        }
        connectionPriorityHighRequestResult = submitted;
        log("OM8_CONNECTION_PRIORITY_REQUEST"
                + " priority=HIGH"
                + " submitted=" + submitted
                + " gattGeneration=" + currentGattGeneration
                + " gattIdentity=" + System.identityHashCode(callbackGatt)
                + " timeSinceGattConnectedNanos="
                + elapsedSince(gattConnectedNanos, SystemClock.elapsedRealtimeNanos()));
    }

    private void handleGattFailure(BluetoothGatt callbackGatt, String reason) {
        if (callbackGatt != null && callbackGatt != gatt) {
            log("GATT stale failure ignored reason=" + reason);
            return;
        }
        listenerReady = false;
        notificationCharacteristic = null;
        disconnectNanos = SystemClock.elapsedRealtimeNanos();
        setListenerState(ListenerState.DISCONNECTED);
        BluetoothGatt current = callbackGatt == null ? gatt : callbackGatt;
        if (callbackGatt == null || gatt == callbackGatt) {
            gatt = null;
            currentGattGeneration = -1L;
            connectionPriorityHighRequested = false;
            connectionPriorityHighRequestResult = false;
        }
        closeGatt(current);
        edgeDetector.update(false);
        log("OM8 GATT disposed reason=" + reason
                + " timeSinceDisconnectNanos=0");
        scheduleRetry(reason, nextRetryDelayMs());
    }

    private void failCurrentGatt(String reason) {
        handleGattFailure(gatt, reason);
    }

    private void scheduleRetry(String reason, long requestedDelayMs) {
        if (!listenerRequested) {
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            setListenerState(ListenerState.DISCONNECTED);
            log("OM8_STATE DISCONNECTED reason=bluetooth-unavailable retry-paused");
            return;
        }
        if (retryRunnable != null) {
            return;
        }
        long delayMs = Math.max(0L, requestedDelayMs);
        setListenerState(ListenerState.RETRY_WAIT);
        updateNotification("OM8 retry in " + delayMs + " ms");
        log("OM8_STATE RETRY_WAIT reason=" + reason
                + " attempt=" + (retryAttempt + 1)
                + " delayMs=" + delayMs);
        retryRunnable = () -> {
            retryRunnable = null;
            if (!listenerRequested) {
                return;
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                setListenerState(ListenerState.DISCONNECTED);
                log("OM8_STATE DISCONNECTED reason=bluetooth-unavailable retry-paused");
                return;
            }
            retryAttempt++;
            startScanAttempt();
        };
        handler.postDelayed(retryRunnable, delayMs);
    }

    private void scheduleNotificationWatchdog() {
        cancelNotificationWatchdog();
        notificationWatchdogRunnable = () -> {
            notificationWatchdogRunnable = null;
            if (!listenerRequested || listenerState != ListenerState.READY || gatt == null) {
                return;
            }
            long now = SystemClock.elapsedRealtimeNanos();
            long last = lastNotificationNanos;
            long stallNanos = NOTIFICATION_STALL_TIMEOUT_MS * 1_000_000L;
            long notificationBaseline = last >= 0L ? last : readyNanos;
            if (notificationBaseline >= 0L
                    && now - notificationBaseline >= stallNanos) {
                log("OM8 notification channel stalled ageNanos="
                        + (now - notificationBaseline));
                failCurrentGatt("notification-channel-stalled");
                return;
            }
            scheduleNotificationWatchdog();
        };
        handler.postDelayed(notificationWatchdogRunnable,
                NOTIFICATION_WATCHDOG_PERIOD_MS);
    }

    private void cancelNotificationWatchdog() {
        if (notificationWatchdogRunnable != null && handler != null) {
            handler.removeCallbacks(notificationWatchdogRunnable);
        }
        notificationWatchdogRunnable = null;
    }

    private long nextRetryDelayMs() {
        return ReconnectBackoff.delayForAttempt(retryAttempt + 1);
    }

    private void cancelRetry() {
        if (retryRunnable != null && handler != null) {
            handler.removeCallbacks(retryRunnable);
        }
        retryRunnable = null;
    }

    private void stopListener(String reason) {
        listenerRequested = false;
        cancelRetry();
        cancelNotificationWatchdog();
        stopScan();
        listenerReady = false;
        notificationCharacteristic = null;
        disposeCurrentGatt();
        edgeDetector.update(false);
        setListenerState(ListenerState.STOPPED);
        log("OM8 listener stopped reason=" + reason);
    }

    private void disposeCurrentGatt() {
        cancelNotificationWatchdog();
        BluetoothGatt current = gatt;
        gatt = null;
        listenerReady = false;
        currentGattGeneration = -1L;
        connectionPriorityHighRequested = false;
        connectionPriorityHighRequestResult = false;
        lastNotificationNanos = -1L;
        notificationCharacteristic = null;
        closeGatt(current);
    }

    private void postLifecycle(Runnable action) {
        Handler lifecycleHandler = handler;
        if (lifecycleHandler != null) {
            lifecycleHandler.post(action);
        }
    }

    private void setListenerState(ListenerState state) {
        if (listenerState == state) {
            return;
        }
        listenerState = state;
        publishGimbalState(state);
        log("OM8_STATE " + state.name());
    }

    private void publishGimbalState(ListenerState state) {
        BridgeController controller = BridgeRuntime.controller();
        if (controller == null || state == null) {
            return;
        }
        try {
            controller.setGimbalConnectionState(
                    GimbalDriver.ConnectionState.valueOf(state.name()));
        } catch (IllegalArgumentException ignored) {
            // Listener and core state names are intentionally kept aligned.
        }
    }

    private long elapsedSince(long startNanos, long endNanos) {
        if (startNanos < 0L || endNanos < 0L || endNanos < startNanos) {
            return -1L;
        }
        return endNanos - startNanos;
    }

    private synchronized void handleNotification(byte[] value,
                                                  long callbackEnter,
                                                  boolean callbackOnMainLooper,
                                                  long callbackThreadId) {
        if (value == null) {
            log("OM8 FFF4 notification ignored: null value");
            return;
        }
        long parseStart = SystemClock.elapsedRealtimeNanos();
        Om8DumlStatusParser.Result parsed = Om8DumlStatusParser.parse(value);
        long parseEnd = SystemClock.elapsedRealtimeNanos();
        if (!parsed.isValid()) {
            if (verboseDiagnostics) {
                logger.packet("OM8_FFF4_RX_INVALID", value);
                log("OM8 FFF4 notification ignored reason=" + parsed.getReason()
                        + " length=" + value.length);
            }
            return;
        }

        long edgeDetectStart = SystemClock.elapsedRealtimeNanos();
        Om8RecButtonEdgeDetector.Event edge =
                edgeDetector.update(parsed.isRecordButtonDown());
        long edgeDetectEnd = SystemClock.elapsedRealtimeNanos();
        if (edge == Om8RecButtonEdgeDetector.Event.PRESS) {
            long sequence = ++om8PressSequence;
            lastOm8PressSequence = sequence;
            ControllerEvent event = new ControllerEvent(
                    ControllerEvent.Type.RECORD_DOWN,
                    id(),
                    sequence,
                    callbackEnter,
                    edgeDetectEnd,
                    callbackOnMainLooper,
                    callbackThreadId);
            long dispatchStart = SystemClock.elapsedRealtimeNanos();
            boolean accepted = eventSink != null
                    ? eventSink.onControllerEvent(event)
                    : BridgeRuntime.dispatch(event);
            long dispatchEnd = SystemClock.elapsedRealtimeNanos();
            int recBit = parsed.getRecordByte() & Om8DumlStatusParser.RECORD_BIT;
            if (verboseDiagnostics) {
                byte[] frame = parsed.getFrame();
                logger.packet("OM8_FFF4_RX", frame);
            }
            String frameHex = verboseDiagnostics
                    ? BridgeDiagnosticLogger.toHex(parsed.getFrame()) : "";
            log("OM8_REC_PRESS"
                    + " controllerEventSequence=" + sequence
                    + " recBit=0x" + String.format(Locale.US, "%02X", recBit)
                    + " t_ble_callback_enter=" + callbackEnter
                    + " t_event_detected=" + edgeDetectEnd
                    + " callbackToDetectionNanos=" + (edgeDetectEnd - callbackEnter)
                    + " bridgeDispatchNanos=" + (dispatchEnd - dispatchStart)
                    + " accepted=" + accepted
                    + (verboseDiagnostics ? " frame=" + frameHex : ""));
            if (!accepted) {
                log("BRIDGE_EVENT_REJECTED reason=no-selected-camera-adapter-or-camera-service");
            }
        } else if (edge == Om8RecButtonEdgeDetector.Event.RELEASE) {
            byte[] frame = parsed.getFrame();
            if (verboseDiagnostics) {
                logger.packet("OM8_FFF4_RX", frame);
            }
            log("OM8_REC_RELEASE controllerEventSequence=" + lastOm8PressSequence
                    + (verboseDiagnostics ? " frame=" + BridgeDiagnosticLogger.toHex(frame) : ""));
            ControllerEvent event = new ControllerEvent(
                    ControllerEvent.Type.RECORD_UP,
                    id(),
                    lastOm8PressSequence,
                    callbackEnter,
                    edgeDetectEnd,
                    callbackOnMainLooper,
                    callbackThreadId);
            ControllerEventSink sink = eventSink;
            if (sink != null) {
                sink.onControllerEvent(event);
            }
        } else if (verboseDiagnostics) {
            logger.packet("OM8_FFF4_RX", parsed.getFrame());
            log("OM8 status heartbeat cmdSet=0x04 cmdId=0x57");
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void logFailure(String message) {
        log(message);
        updateNotification("OM8 listener: " + message);
    }

    private void log(String message) {
        Log.i(TAG, message);
        if (logger != null) {
            logger.log("OM8", message);
        }
    }

    private void stopScan() {
        if (scanTimeoutRunnable != null && handler != null) {
            handler.removeCallbacks(scanTimeoutRunnable);
        }
        scanTimeoutRunnable = null;
        if (scanner != null && scanCallback != null) {
            try {
                scanner.stopScan(scanCallback);
            } catch (SecurityException error) {
                log("OM8 scan stop permission failure=" + error.getMessage());
            }
        }
        scanCallback = null;
    }

    private void closeGatt(BluetoothGatt callbackGatt) {
        if (callbackGatt == null) {
            return;
        }
        try {
            callbackGatt.disconnect();
        } catch (Exception error) {
            log("GATT disconnect exception=" + error.getClass().getSimpleName());
        }
        try {
            callbackGatt.close();
        } catch (Exception error) {
            log("GATT close exception=" + error.getClass().getSimpleName());
        }
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "OM8 listener", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Foreground status for the receive-only OM8 diagnostic");
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            launch = new Intent();
        }
        PendingIntent pending = PendingIntent.getActivity(
                this,
                1,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle("OM Bridge for Android")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        if (!foregroundStarted) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }
}
