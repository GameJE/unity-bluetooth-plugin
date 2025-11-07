package com.zentry.unity_bluetooth_plugin.models;

public class BLEDevice {
    private final String address;
    private final String name;
    private int rssi;
    private byte[] advertisingData;

    public BLEDevice(String address, String name) {
        this.address = address;
        this.name = name != null ? name : "Unknown Device";
        this.rssi = 0;
        this.advertisingData = null;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public byte[] getAdvertisingData() {
        return advertisingData;
    }

    public void setAdvertisingData(byte[] data) {
        this.advertisingData = data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BLEDevice device = (BLEDevice) obj;
        return address.equals(device.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
