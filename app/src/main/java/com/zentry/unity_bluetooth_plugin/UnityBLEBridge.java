package com.zentry.unity_bluetooth_plugin;

import android.util.Log;
import com.unity3d.player.UnityPlayer;
import com.zentry.unity_bluetooth_plugin.models.BLEMessage;
import com.zentry.unity_bluetooth_plugin.utils.ThreadHelper;
import com.zentry.unity_bluetooth_plugin.utils.LegacyMessageConverter;

public class UnityBLEBridge {
    private static final String TAG = "UnityBLEBridge";
    private static final String UNITY_GAME_OBJECT_NAME = "BluetoothLEReceiver";
    private static final String UNITY_CALLBACK_METHOD = "OnBluetoothMessage";
    private static final boolean USE_BINARY_DIRECT_PATH = true;

    private static UnityBLEBridge instance;
    private boolean isInitialized = false;
    private static IBLERawDataCallback rawDataCallback;

    private UnityBLEBridge() {
    }

    public static UnityBLEBridge getInstance() {
        if (instance == null) {
            synchronized (UnityBLEBridge.class) {
                if (instance == null) {
                    instance = new UnityBLEBridge();
                }
            }
        }
        return instance;
    }

    public void initialize() {
        isInitialized = true;
        Log.d(TAG, "Unity BLE Bridge initialized");
    }

    public static void SetRawDataCallback(IBLERawDataCallback callback) {
        rawDataCallback = callback;
        Log.d(TAG, "Raw data callback registered: " + (callback != null ? "enabled" : "disabled"));
    }

    public void sendMessage(BLEMessage message) {
        if (!isInitialized) {
            Log.w(TAG, "Bridge not initialized, message not sent: " + message.toJson());
            return;
        }

        final String jsonMessage = message.toJson();
        final String legacyMessage = LegacyMessageConverter.convertToLegacyFormat(jsonMessage);

        ThreadHelper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, UNITY_CALLBACK_METHOD, legacyMessage);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send message to Unity: " + e.getMessage());
                }
            }
        });
    }

    public void sendInitialized() {
        sendMessage(BLEMessage.createInitialized());
    }

    public void sendError(String error) {
        sendMessage(BLEMessage.createError(error));
    }

    public void sendDeviceDiscovered(String address, String name, int rssi, byte[] advertisingData) {
        sendMessage(BLEMessage.createDeviceDiscovered(address, name, rssi, advertisingData));
    }

    public void sendDeviceConnected(String address) {
        sendMessage(BLEMessage.createDeviceConnected(address));
    }

    public void sendDeviceDisconnected(String address) {
        sendMessage(BLEMessage.createDeviceDisconnected(address));
    }

    public void sendServiceDiscovered(String address, String serviceUUID) {
        sendMessage(BLEMessage.createServiceDiscovered(address, serviceUUID));
    }

    public void sendCharacteristicDiscovered(String address, String serviceUUID, String characteristicUUID) {
        sendMessage(BLEMessage.createCharacteristicDiscovered(address, serviceUUID, characteristicUUID));
    }

    public void sendDataReceived(String address, String characteristicUUID, byte[] data) {
        if (USE_BINARY_DIRECT_PATH && rawDataCallback != null) {
            rawDataCallback.onDataReceived(address, characteristicUUID, data);
        } else {
            sendMessage(BLEMessage.createDataReceived(address, characteristicUUID, data));
        }
    }

    public void sendMtuChanged(String address, int mtu) {
        sendMessage(BLEMessage.createMtuChanged(address, mtu));
    }

    public void sendNotificationStateChanged(String address, String characteristicUUID) {
        sendMessage(BLEMessage.createNotificationStateChanged(address, characteristicUUID));
    }

    public void sendRssiRead(String address, int rssi) {
        sendMessage(BLEMessage.createRssiRead(address, rssi));
    }
}
