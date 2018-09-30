package me.goverse.blelinker;

import android.text.TextUtils;

public class BtDevice {

    public BtDevice(String name, String macAddress, int rssi) {
        this.name = name;
        this.macAddress = macAddress;
        this.rssi = rssi;
    }

    public BtDevice(String name, String macAddress) {
        this.name = name;
        this.macAddress = macAddress;
    }

    public String name;
    public String macAddress;
    public int rssi;

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + rssi;
        result = 31 * result + macAddress.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if ((null == obj) || (obj.getClass() != BtDevice.class))
            return false;
        BtDevice btDevice = (BtDevice) obj;
        if (!TextUtils.isEmpty(btDevice.macAddress) && !TextUtils.isEmpty(btDevice.name)) {

            return btDevice.name.equals(name) &&
                    btDevice.macAddress.equals(macAddress) &&
                    btDevice.rssi == rssi;
        }
        return false;
    }
}