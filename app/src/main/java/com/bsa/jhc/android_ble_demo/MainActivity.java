package com.bsa.jhc.android_ble_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.ble.devcie.MyBleDevice;
import com.MyUtil;
import com.ble.myBle.MyBle;
import com.ble.devcie.Prot;
import com.ble.devcie.BleDeviceCallBack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, BleDeviceCallBack {

    /*private static final String serviceUuidString = "2ea78970-7d44-44bb-b097-26183f402400";
    private static final String characterUuidString = "2ea78970-7d44-44bb-b097-26183f402408";*/
    private static final UUID serviceUuid = UUID.fromString("00006006-0000-1000-8000-00805f9b34fb");
    private static final UUID tx_characterUuid = UUID.fromString("00008001-0000-1000-8000-00805f9b34fb");
    private static final UUID rx_characterUuid = UUID.fromString("00008002-0000-1000-8000-00805f9b34fb");
    private static final UUID rx_DescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String TAG = "MainActivity";
    /*
    * widgets
     */
    private Button btn_scan;
    private ListView lv_ble_list;
    private EditText et_filter_upper, et_filter_lower;
    private TextView tv_log;
    private Button btn_info;
    private Button btn_ota;
    private Button btn_conn;

    private List<Object> ble_devices_list = null;
    private BleAdapter lv_adapter = null;

    private MyApp myApp = null;

    private MyBle myBle = null;

    private ProgressDialog pdlg_ota = null;
    private boolean dfu_ongoing = false;

    private MyBleDevice myBleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        tv_log = findViewById(R.id.tv_log);

        et_filter_upper = findViewById(R.id.filter1);
        et_filter_lower = findViewById(R.id.filter2);
        btn_scan = findViewById(R.id.btn_scan);
        lv_ble_list = findViewById(R.id.ble_list);
        btn_info = findViewById(R.id.btn_getInfo);
        btn_ota = findViewById(R.id.btn_OTA);
        btn_conn = findViewById(R.id.btn_connect);

        btn_scan.setOnClickListener(this);
        btn_info.setOnClickListener(this);
        btn_ota.setOnClickListener(this);
        btn_conn.setOnClickListener(this);

        ble_devices_list = new ArrayList<Object>();
        lv_adapter = new BleAdapter(MainActivity.this, R.layout.simple_expandable_list_item_1, ble_devices_list);
        lv_ble_list.setAdapter(lv_adapter);
        lv_ble_list.setMinimumHeight(128);
        lv_ble_list.setOnItemClickListener(this);

        if (Build.VERSION.SDK_INT >= 6.0) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 12);
        }

        myBleDevice = new MyBleDevice();
        myBleDevice.setCallBack(this);

        myApp = (MyApp) getApplication();
        myApp.setBleHandler(handler);
        myBle = myApp.getMyBle();
        myBle.open();
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 12: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 运行时权限已授权
                }
                break;
            }
        }
    }

    private void logAppend(String message){
        if(tv_log!=null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss - ");// HH:mm:ss
            Date date = new Date(System.currentTimeMillis());//获取当前时间

            tv_log.append(simpleDateFormat.format(date) + message+"\r\n");
            //auto scroll to bottom
            int scrollAmount = tv_log.getLayout().getLineTop(tv_log.getLineCount()) - tv_log.getHeight();
            if (scrollAmount > 0)
                tv_log.scrollTo(0, scrollAmount);
            else
                tv_log.scrollTo(0, 0);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            Object []objs;
            BluetoothDevice device;
            BluetoothGatt gatt;
            BluetoothGattCharacteristic characteristic;
            //Log.d(TAG, "handler: " + msg.what);

            if (dfu_ongoing) {
                switch (msg.what) {

                    case MyBle.BLE_DEVICE_FOUND:
                        device = (BluetoothDevice) msg.obj;
                        break;

                    case MyBle.BLE_SCAN_STOPPED:
                    case MyBle.BLE_SCAN_COMPLETED:
                        break;
                }
            } else {
                switch (msg.what) {
                    case MyBle.BLE_DEVICE_FOUND:
                        objs = (Object[]) msg.obj;

                        device = (BluetoothDevice) objs[0];

                        boolean bExisted = false;
                        //Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                        List<String> filters = new ArrayList<String>();
                        String filter1 = "" + et_filter_upper.getText().toString().trim();
                        String filter2 = "" + et_filter_lower.getText().toString().trim();
                        if (filter1.length() > 0)
                            filters.add(filter1);
                        if (filter2.length() > 0)
                            filters.add(filter2);

                        if (filters.size() != 0) {
                            String device_name = "" + device.getName();
                            int i;
                            for (i = 0; i < filters.size(); i++) {
                                String filter = filters.get(i);
                                if (device_name.toUpperCase().contains(filter.toUpperCase()))
                                    break;
                            }
                            if (i >= filters.size())
                                break;
                        }

                        for (Object object : ble_devices_list) {
                            BluetoothDevice device_from_list = (BluetoothDevice) object;
                            if (device_from_list.getAddress().equals(device.getAddress())) {
                                bExisted = true;
                                break;
                            }
                        }
                        if (!bExisted) {
                            ble_devices_list.add(device);
                            lv_adapter.notifyDataSetChanged();
                        }
                        break;
                    case MyBle.BLE_SCAN_STARTED:
                        logAppend("scan start...");
                        ble_devices_list.clear();
                        lv_adapter.setSelectItemIndex(-1);
                        lv_adapter.notifyDataSetChanged();
                        btn_scan.setText(R.string.STOP_SCAN);
                        btn_scan.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                        break;
                    case MyBle.BLE_SCAN_STOPPED:
                    case MyBle.BLE_SCAN_COMPLETED:
                        logAppend("scan complete...");
                        btn_scan.setText(R.string.SCAN);
                        btn_scan.setTextColor(getResources().getColor(android.R.color.black));
                        break;
                    case MyBle.BLE_DEVICE_CONNECTED:
                        objs = (Object[]) msg.obj;
                        device = ((BluetoothGatt) objs[0]).getDevice();
                        btn_conn.setText(R.string.DISCONN);
                        logAppend("connected to " + device.getName());
                        //Toast.makeText(getApplicationContext(), "connected to"+device.getName(), Toast.LENGTH_SHORT).show();
                        break;
                    case MyBle.BLE_DEVICE_CONNECTING:
                        objs = (Object[]) msg.obj;
                        device = ((BluetoothGatt) objs[0]).getDevice();
                        logAppend("connecting to " + device.getName());
                        break;
                    case MyBle.BLE_DEVICE_DISCONNECTED:
                        objs = (Object[]) msg.obj;
                        device = ((BluetoothGatt) objs[0]).getDevice();
                        btn_conn.setText(R.string.CONN);
                        logAppend("disconnected from " + device.getName());
                        break;
                    case MyBle.BLE_SERVICES_FOUND:
                        objs = (Object[]) msg.obj;
                        gatt = (BluetoothGatt) objs[0];
                        if(myBleDevice != null) {
                            myBleDevice.matchAndSetMyBleDevice(gatt);
                        }
                        break;
                    case MyBle.BLE_CHARACTERISTIC_WRITE:
                        objs = (Object[]) msg.obj;
                        gatt = (BluetoothGatt) objs[0];
                        characteristic = (BluetoothGattCharacteristic) objs[1];
                        Log.d(TAG, "onCharacteristicWrite(" + characteristic.getValue().length +"): " + MyUtil.toHexString(characteristic.getValue()));
                        break;
                    case MyBle.BLE_CHARACTERISTIC_READ:
                        objs = (Object[]) msg.obj;
                        gatt = (BluetoothGatt) objs[0];
                        characteristic = (BluetoothGattCharacteristic) objs[1];
                        logAppend("char read:" + new String(characteristic.getValue()));
                        Log.d(TAG, "onCharacteristicRead(" + characteristic.getValue().length +"): " + MyUtil.toHexString(characteristic.getValue()));
                        break;
                    case MyBle.BLE_CHARACTERISTIC_CHANGED:
                        objs = (Object[]) msg.obj;
                        gatt = (BluetoothGatt) objs[0];
                        characteristic = (BluetoothGattCharacteristic) objs[1];
                        if(myBleDevice != null) {
                            myBleDevice.recvFromMyBleDevice(gatt, characteristic);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_scan:
                if(myBle.isScanning()) {
                    myBle.scanStop();
                }
                else{
                    myBle.scanStart();
                }
                break;
            case R.id.btn_OTA:
                if(myBle.isBleConnected()){
                    BluetoothGatt gatt = myBle.getCurrentBleGatt();
                    if(myBleDevice != null){
                        myBleDevice.sendToMyBleDevice((byte)0x0e,Prot.PKT_DATA_SET, new byte[]{0});
                        dfu_ongoing = true;
                        pdlg_ota = ProgressDialog.show(MainActivity.this, "DFU", "ongoing...");
                    }
                }
                break;
            case R.id.btn_getInfo:
                if(myBle.isBleConnected()){
                    BluetoothGatt gatt = myBle.getCurrentBleGatt();
                    if(myBleDevice != null) {
                        myBleDevice.sendToMyBleDevice((byte) 0x02, Prot.PKT_DATA_GET, new byte[]{0});
                    }
                }
                break;
            case R.id.btn_connect:
                if(myBle.isBleConnected()) {
                    myBle.bleDisconnect();
                } else {
                    int index = lv_adapter.getSelectedItemIndex();
                    if((index >= 0) && (index < ble_devices_list.size())) {
                        BluetoothDevice device = (BluetoothDevice) ble_devices_list.get(index);
                        boolean rel = myBle.bleConnect(device);
                    }
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(adapterView.getId() == R.id.ble_list) {
            lv_adapter.setSelectItemIndex(i);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBle.bleDisconnect();
        myBle.close();
        myApp.setBleHandler(null);
    }

    @Override
    public void deviceReceive(byte cmd, byte dir, byte[] data) {
        logAppend("recv " + MyUtil.toHexString(data));
    }
}
