package com.zentry.unity_bluetooth_plugin;

public interface IBLERawDataCallback {
    void onDataReceived(String address, String characteristicUUID, byte[] data);
}
