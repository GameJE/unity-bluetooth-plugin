package com.zentry.unity_bluetooth_plugin.gatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.util.Log;

import com.zentry.unity_bluetooth_plugin.BLEManager;
import com.zentry.unity_bluetooth_plugin.UnityBLEBridge;
import com.zentry.unity_bluetooth_plugin.connection.BLEConnectionManager;
import com.zentry.unity_bluetooth_plugin.utils.ThreadHelper;
import com.zentry.unity_bluetooth_plugin.utils.UUIDConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BLEGattManager {
    private static final String TAG = "BLEGattManager";
    private static final String CCCD_UUID = "00002902-0000-1000-8000-00805F9B34FB";
    private static final int DEFAULT_MTU = 23;
    private static final int MAX_MTU = 512;

    private static BLEGattManager instance;
    private UnityBLEBridge bridge;
    private Map<String, Map<String, BluetoothGattCallback>> characteristicCallbacks;

    private BLEGattManager() {
        characteristicCallbacks = new HashMap<>();
    }

    public static BLEGattManager getInstance() {
        if (instance == null) {
            synchronized (BLEGattManager.class) {
                if (instance == null) {
                    instance = new BLEGattManager();
                }
            }
        }
        return instance;
    }

    public static void SubscribeCharacteristic(String address, String serviceUUID, String characteristicUUID) {
        BLEGattManager manager = getInstance();
        manager.subscribeInternal(address, serviceUUID, characteristicUUID);
    }

    private void subscribeInternal(String address, String serviceUUID, String characteristicUUID) {
        bridge = BLEManager.getInstance().getBridge();
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();

        BluetoothGatt gatt = connectionManager.getGatt(address);

        if (gatt == null) {
            Log.e(TAG, "Device not connected: " + address);
            bridge.sendError("Device not connected: " + address);
            return;
        }

        String normalizedServiceUUID = UUIDConverter.normalize(serviceUUID);
        String normalizedCharUUID = UUIDConverter.normalize(characteristicUUID);

        BluetoothGattService service = gatt.getService(UUID.fromString(normalizedServiceUUID));
        if (service == null) {
            bridge.sendError("Service not found: " + serviceUUID);
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(normalizedCharUUID));
        if (characteristic == null) {
            bridge.sendError("Characteristic not found: " + characteristicUUID);
            return;
        }

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean result = gatt.setCharacteristicNotification(characteristic, true);
                if (!result) {
                    bridge.sendError("Failed to enable notification for: " + characteristicUUID);
                    return;
                }

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCCD_UUID));
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    Log.d(TAG, "Subscription requested: " + characteristicUUID);
                } else {
                    Log.w(TAG, "CCCD descriptor not found for: " + characteristicUUID);
                }
            }
        });
    }

    public static void UnSubscribeCharacteristic(String address, String serviceUUID, String characteristicUUID) {
        BLEGattManager manager = getInstance();
        manager.unsubscribeInternal(address, serviceUUID, characteristicUUID);
    }

    private void unsubscribeInternal(String address, String serviceUUID, String characteristicUUID) {
        bridge = BLEManager.getInstance().getBridge();
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();
        BluetoothGatt gatt = connectionManager.getGatt(address);

        if (gatt == null) {
            bridge.sendError("Device not connected: " + address);
            return;
        }

        String normalizedServiceUUID = UUIDConverter.normalize(serviceUUID);
        String normalizedCharUUID = UUIDConverter.normalize(characteristicUUID);

        BluetoothGattService service = gatt.getService(UUID.fromString(normalizedServiceUUID));
        if (service == null) {
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(normalizedCharUUID));
        if (characteristic == null) {
            return;
        }

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                gatt.setCharacteristicNotification(characteristic, false);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCCD_UUID));
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    Log.d(TAG, "Unsubscription requested: " + characteristicUUID);
                }
            }
        });
    }

    public static void ReadCharacteristic(String address, String serviceUUID, String characteristicUUID) {
        BLEGattManager manager = getInstance();
        manager.readInternal(address, serviceUUID, characteristicUUID);
    }

    private void readInternal(String address, String serviceUUID, String characteristicUUID) {
        bridge = BLEManager.getInstance().getBridge();
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();
        BluetoothGatt gatt = connectionManager.getGatt(address);

        if (gatt == null) {
            Log.e(TAG, "Device not connected: " + address);
            bridge.sendError("Device not connected: " + address);
            return;
        }

        String normalizedServiceUUID = UUIDConverter.normalize(serviceUUID);
        String normalizedCharUUID = UUIDConverter.normalize(characteristicUUID);

        BluetoothGattService service = gatt.getService(UUID.fromString(normalizedServiceUUID));
        if (service == null) {
            Log.e(TAG, "Service not found: " + serviceUUID);
            bridge.sendError("Service not found: " + serviceUUID);
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(normalizedCharUUID));
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found: " + characteristicUUID);
            bridge.sendError("Characteristic not found: " + characteristicUUID);
            return;
        }

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean result = gatt.readCharacteristic(characteristic);
                if (!result) {
                    Log.e(TAG, "Failed to read characteristic: " + characteristicUUID);
                    bridge.sendError("Failed to read characteristic: " + characteristicUUID);
                }
            }
        });
    }

    public static void WriteCharacteristic(String address, String serviceUUID, String characteristicUUID, byte[] data, boolean withResponse) {
        BLEGattManager manager = getInstance();
        manager.writeInternal(address, serviceUUID, characteristicUUID, data, withResponse);
    }

    private void writeInternal(String address, String serviceUUID, String characteristicUUID, byte[] data, boolean withResponse) {
        bridge = BLEManager.getInstance().getBridge();
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();
        BluetoothGatt gatt = connectionManager.getGatt(address);

        if (gatt == null) {
            bridge.sendError("Device not connected: " + address);
            return;
        }

        String normalizedServiceUUID = UUIDConverter.normalize(serviceUUID);
        String normalizedCharUUID = UUIDConverter.normalize(characteristicUUID);

        BluetoothGattService service = gatt.getService(UUID.fromString(normalizedServiceUUID));
        if (service == null) {
            bridge.sendError("Service not found: " + serviceUUID);
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(normalizedCharUUID));
        if (characteristic == null) {
            bridge.sendError("Characteristic not found: " + characteristicUUID);
            return;
        }

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                int writeType = withResponse ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;

                boolean result;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    int status = gatt.writeCharacteristic(characteristic, data, writeType);
                    result = (status == 0);
                } else {
                    characteristic.setValue(data);
                    characteristic.setWriteType(writeType);
                    result = gatt.writeCharacteristic(characteristic);
                }

                if (!result) {
                    bridge.sendError("Failed to write characteristic: " + characteristicUUID);
                }
                Log.d(TAG, "Write requested: " + characteristicUUID + " (" + data.length + " bytes)");
            }
        });
    }

    public static void RequestMtu(String address, int mtu) {
        BLEGattManager manager = getInstance();
        manager.requestMtuInternal(address, mtu);
    }

    public static void ReadRSSI(String address) {
        BLEGattManager manager = getInstance();
        manager.readRssiInternal(address);
    }

    private void readRssiInternal(String address) {
        bridge = BLEManager.getInstance().getBridge();
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();
        BluetoothGatt gatt = connectionManager.getGatt(address);

        if (gatt == null) {
            bridge.sendError("Device not connected: " + address);
            return;
        }

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean result = gatt.readRemoteRssi();
                if (!result) {
                    bridge.sendError("Failed to read RSSI: " + address);
                }
            }
        });
    }

    private void requestMtuInternal(String address, int mtu) {
        bridge = BLEManager.getInstance().getBridge();
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();
        BluetoothGatt gatt = connectionManager.getGatt(address);

        if (gatt == null) {
            bridge.sendError("Device not connected: " + address);
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "MTU request not supported on this Android version");
            bridge.sendMtuChanged(address, DEFAULT_MTU);
            return;
        }

        int requestMtu = Math.min(mtu, MAX_MTU);

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean result = gatt.requestMtu(requestMtu);
                if (!result) {
                    bridge.sendError("Failed to request MTU: " + requestMtu);
                }
                Log.d(TAG, "MTU requested: " + requestMtu);
            }
        });
    }
}
