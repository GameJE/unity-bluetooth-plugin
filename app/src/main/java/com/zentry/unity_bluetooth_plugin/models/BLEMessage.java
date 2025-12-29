package com.zentry.unity_bluetooth_plugin.models;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

public class BLEMessage {
    private static final String KEY_TYPE = "type";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_NAME = "name";
    private static final String KEY_SERVICE_UUID = "serviceUUID";
    private static final String KEY_CHARACTERISTIC_UUID = "characteristicUUID";
    private static final String KEY_DATA = "data";
    private static final String KEY_RSSI = "rssi";
    private static final String KEY_MTU = "mtu";
    private static final String KEY_ERROR = "error";

    public static final String TYPE_INITIALIZED = "OnInitialized";
    public static final String TYPE_ERROR = "OnError";
    public static final String TYPE_DEVICE_DISCOVERED = "OnDeviceDiscovered";
    public static final String TYPE_DEVICE_CONNECTED = "OnDeviceConnected";
    public static final String TYPE_DEVICE_DISCONNECTED = "OnDeviceDisconnected";
    public static final String TYPE_SERVICE_DISCOVERED = "OnServiceDiscovered";
    public static final String TYPE_CHARACTERISTIC_DISCOVERED = "OnCharacteristicDiscovered";
    public static final String TYPE_DATA_RECEIVED = "OnDataReceived";
    public static final String TYPE_MTU_CHANGED = "OnMtuChanged";
    public static final String TYPE_NOTIFICATION_STATE_CHANGED = "OnNotificationStateChanged";
    public static final String TYPE_RSSI_READ = "OnReadRSSI";

    private final String type;
    private final JSONObject data;

    private BLEMessage(String type) {
        this.type = type;
        this.data = new JSONObject();
        try {
            this.data.put(KEY_TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static BLEMessage createInitialized() {
        return new BLEMessage(TYPE_INITIALIZED);
    }

    public static BLEMessage createError(String error) {
        BLEMessage message = new BLEMessage(TYPE_ERROR);
        message.putString(KEY_ERROR, error);
        return message;
    }

    public static BLEMessage createDeviceDiscovered(String address, String name, int rssi, byte[] advertisingData) {
        BLEMessage message = new BLEMessage(TYPE_DEVICE_DISCOVERED);
        message.putString(KEY_ADDRESS, address);
        message.putString(KEY_NAME, name);
        message.putInt(KEY_RSSI, rssi);
        if (advertisingData != null) {
            message.putString(KEY_DATA, Base64.encodeToString(advertisingData, Base64.NO_WRAP));
        }
        return message;
    }

    public static BLEMessage createDeviceConnected(String address) {
        BLEMessage message = new BLEMessage(TYPE_DEVICE_CONNECTED);
        message.putString(KEY_ADDRESS, address);
        return message;
    }

    public static BLEMessage createDeviceDisconnected(String address) {
        BLEMessage message = new BLEMessage(TYPE_DEVICE_DISCONNECTED);
        message.putString(KEY_ADDRESS, address);
        return message;
    }

    public static BLEMessage createServiceDiscovered(String address, String serviceUUID) {
        BLEMessage message = new BLEMessage(TYPE_SERVICE_DISCOVERED);
        message.putString(KEY_ADDRESS, address);
        message.putString(KEY_SERVICE_UUID, serviceUUID);
        return message;
    }

    public static BLEMessage createCharacteristicDiscovered(String address, String serviceUUID, String characteristicUUID) {
        BLEMessage message = new BLEMessage(TYPE_CHARACTERISTIC_DISCOVERED);
        message.putString(KEY_ADDRESS, address);
        message.putString(KEY_SERVICE_UUID, serviceUUID);
        message.putString(KEY_CHARACTERISTIC_UUID, characteristicUUID);
        return message;
    }

    public static BLEMessage createDataReceived(String address, String characteristicUUID, byte[] data) {
        BLEMessage message = new BLEMessage(TYPE_DATA_RECEIVED);
        message.putString(KEY_ADDRESS, address);
        message.putString(KEY_CHARACTERISTIC_UUID, characteristicUUID);
        message.putString(KEY_DATA, Base64.encodeToString(data, Base64.NO_WRAP));
        return message;
    }

    public static BLEMessage createMtuChanged(String address, int mtu) {
        BLEMessage message = new BLEMessage(TYPE_MTU_CHANGED);
        message.putString(KEY_ADDRESS, address);
        message.putInt(KEY_MTU, mtu);
        return message;
    }

    public static BLEMessage createNotificationStateChanged(String address, String characteristicUUID) {
        BLEMessage message = new BLEMessage(TYPE_NOTIFICATION_STATE_CHANGED);
        message.putString(KEY_ADDRESS, address);
        message.putString(KEY_CHARACTERISTIC_UUID, characteristicUUID);
        return message;
    }

    public static BLEMessage createRssiRead(String address, int rssi) {
        BLEMessage message = new BLEMessage(TYPE_RSSI_READ);
        message.putString(KEY_ADDRESS, address);
        message.putInt(KEY_RSSI, rssi);
        return message;
    }

    private void putString(String key, String value) {
        try {
            data.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void putInt(String key, int value) {
        try {
            data.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toJson() {
        return data.toString();
    }
}
