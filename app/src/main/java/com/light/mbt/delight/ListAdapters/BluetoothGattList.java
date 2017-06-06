package com.light.mbt.delight.ListAdapters;

import android.bluetooth.BluetoothGatt;

import java.io.Serializable;

/**
 * Created by RED on 2017/5/26.
 */

public class BluetoothGattList implements Serializable {
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState;

    public int getConnectionState() {
        return mConnectionState;
    }

    public void setConnectionState(int mConnectionState) {
        this.mConnectionState = mConnectionState;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt mBluetoothGatt) {
        this.mBluetoothGatt = mBluetoothGatt;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String DeviceName) {
        this.mDeviceName = DeviceName;
    }

    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    public void setDeviceAddress(String DeviceAddress) {
        this.mDeviceAddress = DeviceAddress;
    }

}

