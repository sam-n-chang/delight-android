package com.light.mbt.delight.ListAdapters;

import java.io.Serializable;

/**
 * Created by RED on 2017/5/26.
 */

public class DeviceList implements Serializable {
    private String mDeviceName;
    private String mDeviceAddress;
    private String name;
    private long time;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

