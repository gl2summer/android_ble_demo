package com.ble.devcie;

/**
 * Created by jhc on 2018/8/2.
 */

public interface BleDeviceCallBack {
    void deviceReceive(byte cmd, byte dir, byte[] data);
}
