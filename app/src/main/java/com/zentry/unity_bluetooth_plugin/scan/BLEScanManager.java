package com.zentry.unity_bluetooth_plugin.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.zentry.unity_bluetooth_plugin.BLEManager;
import com.zentry.unity_bluetooth_plugin.UnityBLEBridge;
import com.zentry.unity_bluetooth_plugin.models.BLEDevice;
import com.zentry.unity_bluetooth_plugin.utils.ThreadHelper;
import com.zentry.unity_bluetooth_plugin.utils.UUIDConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BLEScanManager {
    private static final String TAG = "BLEScanManager";
    private static final long SCAN_TIMEOUT_MS = 30000;

    private static BLEScanManager instance;
    private BluetoothLeScanner scanner;
    private UnityBLEBridge bridge;
    private ScanCallback scanCallback;
    private Map<String, BLEDevice> discoveredDevices;
    private boolean isScanning = false;

    private BLEScanManager() {
        discoveredDevices = new HashMap<>();
    }

    public static BLEScanManager getInstance() {
        if (instance == null) {
            synchronized (BLEScanManager.class) {
                if (instance == null) {
                    instance = new BLEScanManager();
                }
            }
        }
        return instance;
    }

    public static void ScanForPeripheralsWithServices(String[] serviceUUIDs, boolean clearPeripheralList) {
        BLEScanManager manager = getInstance();
        manager.startScanInternal(serviceUUIDs, clearPeripheralList);
    }

    private void startScanInternal(String[] serviceUUIDs, boolean clearPeripheralList) {
        BLEManager bleManager = BLEManager.getInstance();
        bridge = bleManager.getBridge();

        if (!bleManager.isInitialized()) {
            bridge.sendError("BLE not initialized");
            return;
        }

        if (!BLEManager.CheckPermissions()) {
            bridge.sendError("Bluetooth permissions not granted");
            Log.e(TAG, "Missing required Bluetooth permissions");
            return;
        }

        if (isScanning) {
            Log.w(TAG, "Scan already in progress");
            return;
        }

        BluetoothAdapter adapter = bleManager.getBluetoothAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            bridge.sendError("Bluetooth adapter not available or not enabled");
            return;
        }

        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            bridge.sendError("Failed to get BluetoothLeScanner");
            return;
        }

        if (clearPeripheralList) {
            discoveredDevices.clear();
        }

        List<ScanFilter> filters = buildScanFilters(serviceUUIDs);
        ScanSettings settings = buildScanSettings();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                handleScanResult(result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult result : results) {
                    handleScanResult(result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                isScanning = false;
                bridge.sendError("Scan failed with error code: " + errorCode);
            }
        };

        try {
            scanner.startScan(filters, settings, scanCallback);
            isScanning = true;
            Log.d(TAG, "BLE scan started");

            ThreadHelper.runOnMainThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isScanning) {
                        StopScan();
                    }
                }
            }, SCAN_TIMEOUT_MS);
        } catch (Exception e) {
            isScanning = false;
            bridge.sendError("Failed to start scan: " + e.getMessage());
            Log.e(TAG, "Failed to start scan", e);
        }
    }

    public static void StopScan() {
        BLEScanManager manager = getInstance();
        manager.stopScanInternal();
    }

    private void stopScanInternal() {
        if (!isScanning) {
            return;
        }

        if (scanner != null && scanCallback != null) {
            try {
                scanner.stopScan(scanCallback);
                Log.d(TAG, "BLE scan stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping scan", e);
            }
        }

        isScanning = false;
        scanCallback = null;
    }

    private List<ScanFilter> buildScanFilters(String[] serviceUUIDs) {
        List<ScanFilter> filters = new ArrayList<>();

        if (serviceUUIDs != null && serviceUUIDs.length > 0) {
            for (String uuidString : serviceUUIDs) {
                String normalizedUUID = UUIDConverter.normalize(uuidString);
                if (normalizedUUID != null) {
                    try {
                        UUID uuid = UUID.fromString(normalizedUUID);
                        ScanFilter filter = new ScanFilter.Builder()
                                .setServiceUuid(new ParcelUuid(uuid))
                                .build();
                        filters.add(filter);
                    } catch (Exception e) {
                        Log.e(TAG, "Invalid UUID: " + uuidString, e);
                    }
                }
            }
        }

        return filters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }

        return builder.build();
    }

    private void handleScanResult(ScanResult result) {
        if (result == null || result.getDevice() == null) {
            return;
        }

        String address = result.getDevice().getAddress();
        String name = extractDeviceName(result);
        int rssi = result.getRssi();
        byte[] advertisingData = parseAdvertisingData(result);

        BLEDevice device = discoveredDevices.get(address);
        if (device == null) {
            device = new BLEDevice(address, name);
            discoveredDevices.put(address, device);
            device.setRssi(rssi);
            device.setAdvertisingData(advertisingData);

            bridge.sendDeviceDiscovered(address, name, rssi, advertisingData);
            Log.d(TAG, "Device discovered: " + address + " (" + name + ") RSSI: " + rssi);
        } else {
            device.setRssi(rssi);
            device.setAdvertisingData(advertisingData);
        }
    }

    private String extractDeviceName(ScanResult result) {
        String name = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (result.getScanRecord() != null) {
                name = result.getScanRecord().getDeviceName();
            }
        }

        if (name == null) {
            name = result.getDevice().getName();
        }

        return name;
    }

    private byte[] parseAdvertisingData(ScanResult result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (result.getScanRecord() != null) {
                return result.getScanRecord().getBytes();
            }
        }
        return null;
    }

    public boolean isScanning() {
        return isScanning;
    }
}
