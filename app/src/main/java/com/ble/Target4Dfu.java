package com.ble;

import android.bluetooth.BluetoothDevice;

/**
 * Created by jhc on 2018/7/31.
 */

public class Target4Dfu {

    public static final int BEGIN = 0;
    public static final int SCAN_UPPER = 1;
    public static final int CONN_UPPER = 2;
    public static final int SCAN_LOWER = 3;
    public static final int CONN_LOWER = 4;
    public static final int DFU_ONGOING = 5;
    public static final int DFU_SUCCESS = 6;
    public static final int DFU_FAILED = 7;


    private String upper_filter;
    private String lower_filter;
    private String serial_filter;

    private int state = BEGIN;

    private MyBle myBle;
    private BluetoothDevice device;

    public boolean start(MyBle myBle, BluetoothDevice device){

        this.myBle = myBle;
        this.device = device;

        state = BEGIN;

        boolean started = true;
        String device_name = device.getName();
        if (device_name.toUpperCase().contains(upper_filter.toUpperCase())){
            state = CONN_UPPER;
        } else if(device_name.toUpperCase().contains(lower_filter.toUpperCase())) {
            state = CONN_LOWER;
        } else {
            started = false;
        }

        return started;
    }
}
