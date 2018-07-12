package com.bsa.jhc.android_ble_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.LocaleDisplayNames;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ble.MyBle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    /*
    * widgets
     */
    private Button btn_scan;
    private ListView lv_ble_list;
    private EditText et_filter1, et_filter2;

    private List<Object> ble_devices_list = null;
    private BleAdapter lv_adapter = null;

    MyBle myBle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        et_filter1 = (EditText) findViewById(R.id.filter1);
        et_filter2 = (EditText) findViewById(R.id.filter2);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        lv_ble_list = (ListView) findViewById(R.id.ble_list);

        btn_scan.setOnClickListener(this);

        ble_devices_list = new ArrayList<Object>();
        lv_adapter = new BleAdapter(MainActivity.this, R.layout.simple_expandable_list_item_1, ble_devices_list);
        lv_ble_list.setAdapter(lv_adapter);
        lv_ble_list.setMinimumHeight(128);
        lv_ble_list.setOnItemClickListener(this);

        myBle = new MyBle(this, handler);
        myBle.open();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            BluetoothDevice device;
            Log.d("handler", ""+msg.what);

            switch (msg.what){
                case MyBle.BLE_DEVICE_FOUND:
                    boolean bExisted = false;
                    device = (BluetoothDevice) msg.obj;
                    //Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                    List<String> filters = new ArrayList<String>();
                    String filter1 = ""+et_filter1.getText().toString().trim();
                    String filter2 = ""+et_filter2.getText().toString().trim();
                    if(filter1.length() > 0)
                        filters.add(filter1);
                    if(filter2.length() > 0)
                        filters.add(filter2);

                    if(filters.size() != 0) {
                        String device_name = "" + device.getName();
                        int i;
                        for (i = 0; i < filters.size(); i++) {
                            String filter = filters.get(i);
                            if (device_name.contains(filter))
                                break;
                        }
                        if(i >= filters.size())
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
                    ble_devices_list.clear();
                    lv_adapter.notifyDataSetChanged();
                    btn_scan.setText(R.string.STOP_SCAN);
                    btn_scan.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    break;
                case MyBle.BLE_SCAN_STOPPED:
                case MyBle.BLE_SCAN_COMPLETED:
                    btn_scan.setText(R.string.SCAN);
                    btn_scan.setTextColor(getResources().getColor(android.R.color.black));
                    break;
                case MyBle.BLE_DEVICE_CONNECTED:
                    device = ((BluetoothGatt) msg.obj).getDevice();
                    Toast.makeText(getApplicationContext(), "connected to"+device.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case MyBle.BLE_DEVICE_DISCONNECTED:
                    device = ((BluetoothGatt) msg.obj).getDevice();
                    Toast.makeText(getApplicationContext(), "disconnected from"+device.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case MyBle.BLE_SERVICES_FOUND:
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    List<BluetoothGattService> l = gatt.getServices();
                    for(BluetoothGattService s:l){
                        Log.d("service",""+s.getUuid());
                        List<BluetoothGattCharacteristic> cs = s.getCharacteristics();
                        for(BluetoothGattCharacteristic c:cs)
                            Log.d("characteristic", ""+c.getUuid());
                    }
                    break;
                default:break;
            }
        }
    };

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

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_scan:

                if (Build.VERSION.SDK_INT >= 6.0) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 12);
                }
                if(myBle.isScanning())
                    myBle.scanStop();
                else
                    myBle.scanStart();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(adapterView.getId() == R.id.ble_list) {
            BluetoothDevice device = (BluetoothDevice)ble_devices_list.get(i);
            boolean rel = myBle.bleConnect(device);
            //Toast.makeText(this, "connect to "+device.getName()+"  "+device.getAddress()+" "+rel, Toast.LENGTH_SHORT).show();
        }
    }
}
