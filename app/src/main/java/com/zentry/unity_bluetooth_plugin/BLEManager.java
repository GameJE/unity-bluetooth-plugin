package com.zentry.unity_bluetooth_plugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class BLEManager {
    private static final String TAG = "BLEManager";
    private static final int DEFAULT_MTU = 23;
    private static final int MAX_MTU_ANDROID = 512;

    private static BLEManager instance;
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private UnityBLEBridge bridge;

    private boolean isInitialized = false;
    private boolean isCentralMode = false;

    private BLEManager() {
    }

    public static BLEManager getInstance() {
        if (instance == null) {
            synchronized (BLEManager.class) {
                if (instance == null) {
                    instance = new BLEManager();
                }
            }
        }
        return instance;
    }

    public static void Initialize(Context context, boolean asCentral, boolean asPeripheral) {
        BLEManager manager = getInstance();
        manager.initializeInternal(context, asCentral, asPeripheral);
    }

    private void initializeInternal(Context context, boolean asCentral, boolean asPeripheral) {
        if (isInitialized) {
            Log.w(TAG, "BLEManager already initialized");
            return;
        }

        this.context = context.getApplicationContext();
        this.bridge = UnityBLEBridge.getInstance();
        this.isCentralMode = asCentral;

        if (!checkBLESupport()) {
            bridge.sendError("BLE is not supported on this device");
            return;
        }

        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            bridge.sendError("Failed to get BluetoothManager");
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            bridge.sendError("Failed to get BluetoothAdapter");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bridge.sendError("Bluetooth is not enabled");
            return;
        }

        bridge.initialize();
        isInitialized = true;

        Log.d(TAG, "BLE Manager initialized successfully (Central: " + asCentral + ", Peripheral: " + asPeripheral + ")");
        bridge.sendInitialized();
    }

    public static void DeInitialize() {
        BLEManager manager = getInstance();
        manager.deInitializeInternal();
    }

    private void deInitializeInternal() {
        if (!isInitialized) {
            return;
        }

        isInitialized = false;
        isCentralMode = false;
        bluetoothAdapter = null;
        bluetoothManager = null;

        Log.d(TAG, "BLE Manager deinitialized");
    }

    public static void BluetoothEnable(boolean enable) {
        BLEManager manager = getInstance();
        manager.bluetoothEnableInternal(enable);
    }

    private void bluetoothEnableInternal(boolean enable) {
        if (bluetoothAdapter == null) {
            bridge.sendError("BluetoothAdapter not initialized");
            return;
        }

        if (enable) {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Requesting Bluetooth enable");
            }
        } else {
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Requesting Bluetooth disable");
            }
        }
    }

    public static String BluetoothState() {
        BLEManager manager = getInstance();
        return manager.getBluetoothStateInternal();
    }

    private String getBluetoothStateInternal() {
        if (bluetoothAdapter == null) {
            return "Unknown";
        }

        int state = bluetoothAdapter.getState();
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                return "PoweredOff";
            case BluetoothAdapter.STATE_ON:
                return "PoweredOn";
            case BluetoothAdapter.STATE_TURNING_OFF:
                return "TurningOff";
            case BluetoothAdapter.STATE_TURNING_ON:
                return "TurningOn";
            default:
                return "Unknown";
        }
    }

    private boolean checkBLESupport() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "BLE is not supported");
            return false;
        }
        return true;
    }

    public Context getContext() {
        return context;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public UnityBLEBridge getBridge() {
        return bridge;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isCentralMode() {
        return isCentralMode;
    }

    public static boolean CheckPermissions() {
        BLEManager manager = getInstance();
        return manager.checkPermissionsInternal();
    }

    private boolean checkPermissionsInternal() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot check permissions");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasScanPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean hasConnectPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Android 12+ Permissions - SCAN: " + hasScanPermission + ", CONNECT: " + hasConnectPermission);
            return hasScanPermission && hasConnectPermission;
        } else {
            boolean hasLocationPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Android 11- Permissions - LOCATION: " + hasLocationPermission);
            return hasLocationPermission;
        }
    }

    public static String[] GetRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            return new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }
}
