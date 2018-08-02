package com.ble;

/**
 * Created by jhc on 2018/8/1.
 */

public interface BleCallback {
    void notify(int message, Object... obj);
}
