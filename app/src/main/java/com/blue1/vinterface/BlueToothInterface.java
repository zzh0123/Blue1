package com.blue1.vinterface;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ZengZeHong on 2017/5/15.
 */

public interface BlueToothInterface {
    void getBlutToothDevices(BluetoothDevice device , int rssi);
    void searchFinish();
}
