package com.bsa.jhc.android_ble_demo;

import android.app.Application;
import android.os.Handler;
import android.os.Message;

import com.ble.myBle.MyBleCallback;
import com.ble.myBle.MyBle;

/**
 * Created by jhc on 2018/7/16.
 */

public class MyApp extends Application implements MyBleCallback {

    private MyBle myBle;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public synchronized MyBle getMyBle() {
        if(myBle == null) {
            myBle = new MyBle(this, this);
        }
        return myBle;
    }

    public void setBleHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void notify(int message, Object... obj) {
        if(handler != null){
            Message msg = new Message();
            msg.what = message;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }
}
