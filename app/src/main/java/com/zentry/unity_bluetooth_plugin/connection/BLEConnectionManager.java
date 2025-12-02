package com.zentry.unity_bluetooth_plugin.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.zentry.unity_bluetooth_plugin.BLEManager;
import com.zentry.unity_bluetooth_plugin.UnityBLEBridge;
import com.zentry.unity_bluetooth_plugin.utils.ThreadHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BLEConnectionManager {
    private static final String TAG = "BLEConnectionManager";
    private static final int CONNECTION_TIMEOUT_MS = 15000;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private static BLEConnectionManager instance;
    private Map<String, BluetoothGatt> connectedDevices;
    private Map<String, Integer> retryCountMap;
    private Map<String, String> deviceNameToAddressMap;
    private UnityBLEBridge bridge;

    private BLEConnectionManager() {
        connectedDevices = new HashMap<>();
        retryCountMap = new HashMap<>();
        deviceNameToAddressMap = new HashMap<>();
    }

    public static BLEConnectionManager getInstance() {
        if (instance == null) {
            synchronized (BLEConnectionManager.class) {
                if (instance == null) {
                    instance = new BLEConnectionManager();
                }
            }
        }
        return instance;
    }

    public static void ConnectToPeripheral(String address) {
        BLEConnectionManager manager = getInstance();
        manager.connectInternal(address);
    }

    private void connectInternal(String address) {
        BLEManager bleManager = BLEManager.getInstance();
        bridge = bleManager.getBridge();

        if (!bleManager.isInitialized()) {
            bridge.sendError("BLE not initialized");
            return;
        }

        if (connectedDevices.containsKey(address)) {
            Log.w(TAG, "Device already connected: " + address);
            return;
        }

        BluetoothAdapter adapter = bleManager.getBluetoothAdapter();
        if (adapter == null) {
            bridge.sendError("Bluetooth adapter not available");
            return;
        }

        BluetoothDevice device = adapter.getRemoteDevice(address);
        if (device == null) {
            bridge.sendError("Device not found: " + address);
            return;
        }

        retryCountMap.put(address, 0);
        connectWithRetry(device);
    }

    private void connectWithRetry(final BluetoothDevice device) {
        final String address = device.getAddress();
        Integer retryCount = retryCountMap.get(address);

        if (retryCount == null) {
            retryCount = 0;
        }

        if (retryCount >= MAX_RETRY_COUNT) {
            retryCountMap.remove(address);
            bridge.sendError("Failed to connect after " + MAX_RETRY_COUNT + " attempts: " + address);
            return;
        }

        Context context = BLEManager.getInstance().getContext();
        BluetoothGattCallback gattCallback = createGattCallback(address);

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothGatt gatt;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                    } else {
                        gatt = device.connectGatt(context, false, gattCallback);
                    }

                    if (gatt != null) {
                        Log.d(TAG, "Connection initiated: " + address);
                    } else {
                        handleConnectionRetry(device);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initiate connection", e);
                    handleConnectionRetry(device);
                }
            }
        });
    }

    private void handleConnectionRetry(final BluetoothDevice device) {
        final String address = device.getAddress();
        Integer retryCount = retryCountMap.get(address);
        if (retryCount != null) {
            retryCountMap.put(address, retryCount + 1);
            ThreadHelper.runOnMainThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    connectWithRetry(device);
                }
            }, RETRY_DELAY_MS);
        }
    }

    private BluetoothGattCallback createGattCallback(final String address) {
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Device connected: " + address);
                    connectedDevices.put(address, gatt);
                    retryCountMap.remove(address);

                    String deviceName = gatt.getDevice().getName();
                    if (deviceName != null && !deviceName.isEmpty()) {
                        deviceNameToAddressMap.put(deviceName.toUpperCase(), address);
                    }

                    bridge.sendDeviceConnected(address);

                    ThreadHelper.runOnMainThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            gatt.discoverServices();
                        }
                    }, 600);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Device disconnected: " + address);
                    connectedDevices.remove(address);

                    String deviceName = gatt.getDevice().getName();
                    if (deviceName != null && !deviceName.isEmpty()) {
                        deviceNameToAddressMap.remove(deviceName.toUpperCase());
                    }

                    bridge.sendDeviceDisconnected(address);

                    if (gatt != null) {
                        gatt.close();
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered: " + address);

                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        String serviceUuid = service.getUuid().toString().toUpperCase();
                        bridge.sendServiceDiscovered(address, serviceUuid);

                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            String charUuid = characteristic.getUuid().toString().toUpperCase();
                            bridge.sendCharacteristicDiscovered(address, serviceUuid, charUuid);
                        }
                    }
                } else {
                    Log.e(TAG, "Service discovery failed: " + status);
                    bridge.sendError("Service discovery failed for device: " + address);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = characteristic.getValue();
                    String charUuid = characteristic.getUuid().toString().toUpperCase();
                    bridge.sendDataReceived(address, charUuid, data);
                    Log.d(TAG, "Characteristic read: " + charUuid);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                byte[] originalData = characteristic.getValue();
                if (originalData == null || originalData.length == 0) {
                    return;
                }

                byte[] dataCopy = new byte[originalData.length];
                System.arraycopy(originalData, 0, dataCopy, 0, originalData.length);

                String charUuid = characteristic.getUuid().toString().toUpperCase();
                bridge.sendDataReceived(address, charUuid, dataCopy);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    bridge.sendMtuChanged(address, mtu);
                    Log.d(TAG, "MTU changed: " + mtu);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                    if (characteristic != null) {
                        String charUuid = characteristic.getUuid().toString().toLowerCase();
                        bridge.sendNotificationStateChanged(address, charUuid);
                        Log.d(TAG, "Notification state changed for: " + charUuid);
                    }
                }
            }
        };
    }

    public static void DisconnectPeripheral(String address) {
        BLEConnectionManager manager = getInstance();
        manager.disconnectInternal(address);
    }

    private void disconnectInternal(String address) {
        BluetoothGatt gatt = connectedDevices.get(address);
        if (gatt != null) {
            ThreadHelper.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    gatt.disconnect();
                    Log.d(TAG, "Disconnect requested: " + address);
                }
            });
        } else {
            Log.w(TAG, "Device not connected: " + address);
        }
    }

    public static void DisconnectAll() {
        BLEConnectionManager manager = getInstance();
        manager.disconnectAllInternal();
    }

    private void disconnectAllInternal() {
        for (String address : connectedDevices.keySet()) {
            disconnectInternal(address);
        }
    }

    public BluetoothGatt getGatt(String address) {
        return connectedDevices.get(address);
    }

    public boolean isConnected(String address) {
        return connectedDevices.containsKey(address);
    }

    public String getAddressFromName(String deviceName) {
        if (deviceName == null) {
            return null;
        }
        return deviceNameToAddressMap.get(deviceName.toUpperCase());
    }

    public static boolean SetConnectionPriority(String address, int priority) {
        Log.d(TAG, "[DEBUG] SetConnectionPriority static 메서드 호출 - address: " + address + ", priority: " + priority);
        BLEConnectionManager manager = getInstance();
        return manager.setConnectionPriorityInternal(address, priority);
    }

    private boolean setConnectionPriorityInternal(String address, int priority) {
        Log.d(TAG, "[DEBUG] setConnectionPriorityInternal 시작 - address: " + address + ", priority: " + priority);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        BluetoothGatt gatt = connectedDevices.get(address);
        if (gatt == null) {
            Log.w(TAG, "[WARNING] Cannot set connection priority - device not connected: " + address);
            Log.d(TAG, "[DEBUG] connectedDevices 크기: " + connectedDevices.size());
            return false;
        }

        int resolvedPriority = resolvePriority(priority);
        if (resolvedPriority == -1) {
            return false;
        }

        Log.d(TAG, "[DEBUG] requestConnectionPriority resolved - address: " + address + ", resolvedPriority: " + resolvedPriority + ", rawPriority: " + priority);

        if (ThreadHelper.isMainThread()) {
            boolean requestResult = gatt.requestConnectionPriority(resolvedPriority);
            Log.d(TAG, "[DEBUG] requestConnectionPriority mainThread result - address: " + address + ", result: " + requestResult);
            return requestResult;
        }

        AtomicBoolean result = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean requestResult = gatt.requestConnectionPriority(resolvedPriority);
                    Log.d(TAG, "[DEBUG] requestConnectionPriority posted result - address: " + address + ", result: " + requestResult);
                    result.set(requestResult);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return latch.getCount() == 0 && result.get();
    }

    private int resolvePriority(int priority) {
        // Android/Unity 상수(0=BALANCED, 1=HIGH, 2=LOW_POWER) 우선 처리
        switch (priority) {
            case BluetoothGatt.CONNECTION_PRIORITY_BALANCED:
                return BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
            case BluetoothGatt.CONNECTION_PRIORITY_HIGH:
                return BluetoothGatt.CONNECTION_PRIORITY_HIGH;
            case BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER:
                return BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
            default:
                break;
        }

        // Legacy 입력: 1=LOW_POWER, 2=BALANCED, 3=HIGH
        if (priority >= 1 && priority <= 3) {
            switch (priority) {
                case 1:
                    return BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
                case 2:
                    return BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
                case 3:
                    return BluetoothGatt.CONNECTION_PRIORITY_HIGH;
                default:
                    return -1;
            }
        }

        return -1;
    }
}
