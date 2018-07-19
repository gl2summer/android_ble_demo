package com.bsa.jhc.android_ble_demo;

import android.app.Application;

import com.ble.MyBle;

/**
 * Created by jhc on 2018/7/16.
 */

public class MyApp extends Application {

    MyBle myBle = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public synchronized MyBle getMyBle() {
        if(myBle == null)
            myBle = new MyBle(this);
        return myBle;
    }
}
