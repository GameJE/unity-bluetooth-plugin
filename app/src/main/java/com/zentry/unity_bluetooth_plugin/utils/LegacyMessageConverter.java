package com.zentry.unity_bluetooth_plugin.utils;

import org.json.JSONObject;

public class LegacyMessageConverter {
    private static final String KEY_TYPE = "type";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_NAME = "name";
    private static final String KEY_SERVICE_UUID = "serviceUUID";
    private static final String KEY_CHARACTERISTIC_UUID = "characteristicUUID";
    private static final String KEY_DATA = "data";
    private static final String KEY_RSSI = "rssi";
    private static final String KEY_MTU = "mtu";
    private static final String KEY_ERROR = "error";

    private static final String TYPE_INITIALIZED = "OnInitialized";
    private static final String TYPE_ERROR = "OnError";
    private static final String TYPE_DEVICE_DISCOVERED = "OnDeviceDiscovered";
    private static final String TYPE_DEVICE_CONNECTED = "OnDeviceConnected";
    private static final String TYPE_DEVICE_DISCONNECTED = "OnDeviceDisconnected";
    private static final String TYPE_SERVICE_DISCOVERED = "OnServiceDiscovered";
    private static final String TYPE_CHARACTERISTIC_DISCOVERED = "OnCharacteristicDiscovered";
    private static final String TYPE_DATA_RECEIVED = "OnDataReceived";
    private static final String TYPE_MTU_CHANGED = "OnMtuChanged";
    private static final String TYPE_NOTIFICATION_STATE_CHANGED = "OnNotificationStateChanged";
    private static final String TYPE_RSSI_READ = "OnReadRSSI";

    public static String convertToLegacyFormat(String jsonMessage) {
        try {
            JSONObject json = new JSONObject(jsonMessage);
            String type = json.optString(KEY_TYPE, "");
            StringBuilder sb = new StringBuilder();

            switch (type) {
                case TYPE_INITIALIZED:
                    sb.append("Initialized");
                    break;

                case TYPE_ERROR:
                    sb.append("Error~");
                    sb.append(json.optString(KEY_ERROR, ""));
                    break;

                case TYPE_DEVICE_DISCOVERED:
                    sb.append("DiscoveredPeripheral~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_NAME, ""));
                    sb.append("~");
                    sb.append(json.optInt(KEY_RSSI, 0));
                    sb.append("~");
                    sb.append(json.optString(KEY_DATA, ""));
                    break;

                case TYPE_DEVICE_CONNECTED:
                    sb.append("ConnectedPeripheral~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    break;

                case TYPE_DEVICE_DISCONNECTED:
                    sb.append("DisconnectedPeripheral~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    break;

                case TYPE_SERVICE_DISCOVERED:
                    sb.append("DiscoveredService~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_SERVICE_UUID, ""));
                    break;

                case TYPE_CHARACTERISTIC_DISCOVERED:
                    sb.append("DiscoveredCharacteristic~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_SERVICE_UUID, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_CHARACTERISTIC_UUID, ""));
                    break;

                case TYPE_DATA_RECEIVED:
                    sb.append("DidUpdateValueForCharacteristic~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_CHARACTERISTIC_UUID, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_DATA, ""));
                    break;

                case TYPE_MTU_CHANGED:
                    sb.append("MtuChanged~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optInt(KEY_MTU, 0));
                    break;

                case TYPE_NOTIFICATION_STATE_CHANGED:
                    sb.append("DidUpdateNotificationStateForCharacteristic~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optString(KEY_CHARACTERISTIC_UUID, ""));
                    break;

                case TYPE_RSSI_READ:
                    sb.append("DidReadRSSI~");
                    sb.append(json.optString(KEY_ADDRESS, ""));
                    sb.append("~");
                    sb.append(json.optInt(KEY_RSSI, 0));
                    break;

                default:
                    return jsonMessage;
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return jsonMessage;
        }
    }
}
