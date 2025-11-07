package com.zentry.unity_bluetooth_plugin;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.zentry.unity_bluetooth_plugin.connection.BLEConnectionManager;
import com.zentry.unity_bluetooth_plugin.gatt.BLEGattManager;
import com.zentry.unity_bluetooth_plugin.scan.BLEScanManager;

public class UnityBLEPlugin {
    private static final String TAG = "UnityBLEPlugin";

    public static void Initialize(Context context, boolean asCentral, boolean asPeripheral) {
        BLEManager.Initialize(context, asCentral, asPeripheral);
    }

    public static void DeInitialize() {
        BLEManager.DeInitialize();
    }

    public static void BluetoothEnable(boolean enable) {
        BLEManager.BluetoothEnable(enable);
    }

    public static String BluetoothState() {
        return BLEManager.BluetoothState();
    }

    public static void ScanForPeripheralsWithServices(String[] serviceUUIDs, boolean clearPeripheralList) {
        BLEScanManager.ScanForPeripheralsWithServices(serviceUUIDs, clearPeripheralList);
    }

    public static void StopScan() {
        BLEScanManager.StopScan();
    }

    public static void ConnectToPeripheral(String address) {
        BLEConnectionManager.ConnectToPeripheral(address);
    }

    public static void DisconnectPeripheral(String address) {
        BLEConnectionManager.DisconnectPeripheral(address);
    }

    public static void DisconnectAll() {
        BLEConnectionManager.DisconnectAll();
    }

    public static void SubscribeCharacteristic(String nameOrAddress, String serviceUUID, String characteristicUUID) {
        String address = resolveAddress(nameOrAddress);
        if (address != null) {
            BLEGattManager.SubscribeCharacteristic(address, serviceUUID, characteristicUUID);
        } else {
            Log.e(TAG, "resolveAddress returned null for: " + nameOrAddress);
        }
    }

    public static void UnSubscribeCharacteristic(String nameOrAddress, String serviceUUID, String characteristicUUID) {
        String address = resolveAddress(nameOrAddress);
        if (address != null) {
            BLEGattManager.UnSubscribeCharacteristic(address, serviceUUID, characteristicUUID);
        }
    }

    private static String resolveAddress(String nameOrAddress) {
        BLEConnectionManager connectionManager = BLEConnectionManager.getInstance();

        if (connectionManager.isConnected(nameOrAddress)) {
            return nameOrAddress;
        }

        String address = connectionManager.getAddressFromName(nameOrAddress);

        if (address != null && connectionManager.isConnected(address)) {
            return address;
        }

        Log.e(TAG, "resolveAddress - failed to resolve: " + nameOrAddress);
        return null;
    }

    public static void ReadCharacteristic(String nameOrAddress, String serviceUUID, String characteristicUUID) {
        String address = resolveAddress(nameOrAddress);
        if (address != null) {
            BLEGattManager.ReadCharacteristic(address, serviceUUID, characteristicUUID);
        }
    }

    public static void WriteCharacteristic(String nameOrAddress, String serviceUUID, String characteristicUUID, String dataBase64, boolean withResponse) {
        String address = resolveAddress(nameOrAddress);
        if (address != null) {
            byte[] data = Base64.decode(dataBase64, Base64.NO_WRAP);
            BLEGattManager.WriteCharacteristic(address, serviceUUID, characteristicUUID, data, withResponse);
        }
    }

    public static void RequestMtu(String nameOrAddress, int mtu) {
        String address = resolveAddress(nameOrAddress);
        if (address != null) {
            BLEGattManager.RequestMtu(address, mtu);
        }
    }

    public static boolean IsDeviceConnected(String address) {
        return BLEConnectionManager.getInstance().isConnected(address);
    }

    public static boolean IsScanning() {
        return BLEScanManager.getInstance().isScanning();
    }

    public static void SetConnectionPriority(String nameOrAddress, int priority) {
        Log.d(TAG, "[DEBUG] SetConnectionPriority 호출됨 - nameOrAddress: " + nameOrAddress + ", priority: " + priority);

        String address = resolveAddress(nameOrAddress);
        if (address != null) {
            Log.d(TAG, "[DEBUG] Device 주소 확인 완료, BLEConnectionManager.SetConnectionPriority 호출 - address: " + address);
            BLEConnectionManager.SetConnectionPriority(address, priority);
        } else {
            Log.e(TAG, "[ERROR] Cannot set connection priority - device not found: " + nameOrAddress);
        }
    }
}
