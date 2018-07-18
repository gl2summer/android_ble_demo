package com.ble;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;


public class MyBle{

    public static final int BLE_ENABLED = 0;
    public static final int BLE_DISABLED = 1;
    public static final int BLE_SCAN_STARTED = 2;
    public static final int BLE_SCAN_STOPPED = 3;
    public static final int BLE_SCAN_COMPLETED = 4;
    public static final int BLE_DEVICE_FOUND = 5;
    public static final int BLE_DEVICE_CONNECTED = 6;
    public static final int BLE_SERVICES_FOUND = 7;
    public static final int BLE_DEVICE_CONNECTING = 8;
    public static final int BLE_DEVICE_DISCONNECTED = 9;
    public static final int BLE_DATA_SENDED = 10;
    public static final int BLE_DATA_READED = 11;


    private Context context = null;
    private Handler handler = null;

    private BluetoothAdapter adapter = null;
    private BluetoothGatt mBluetoothGatt = null;


    public MyBle(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        //adapter = BluetoothAdapter.getDefaultAdapter();

        if(!adapter.isEnabled())
            adapter.enable();
        /*
        Intent turn_on = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turn_on, 0);
        //Toast.makeText(MainActivity.this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
         */
        /*if(!adapter.isEnabled())
            return true;
        return adapter.disable();*/
    }

    private void notifyOwner(int what, Object obj){
        if((adapter != null) && (handler != null)){
            Message message = new Message();
            message.what = what;
            message.obj = obj;
            handler.sendMessage(message);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {}
                notifyOwner(BLE_DEVICE_FOUND, device);
            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                notifyOwner(BLE_SCAN_STARTED, null);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                notifyOwner(BLE_SCAN_COMPLETED, null);
            }
        }
    };


    private BluetoothGattCallback mGattCallback =  new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    notifyOwner(BLE_DEVICE_CONNECTED, gatt);
                    gatt.discoverServices();
                }
                else {
                    notifyOwner(BLE_DEVICE_DISCONNECTED, gatt);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            notifyOwner(101,null);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //gatt.getServices();
                /*BluetoothGattService service = gatt.getService(UUID.fromString(serviceUuid));
                mCharacteristic = service.getCharacteristic(UUID.fromString(characterUuid));
                mCharacteristicNotice = service.getCharacteristic(UUID.fromString(characterUuidNotice));

                //开启通知
                mBluetoothGatt.setCharacteristicNotification(mCharacteristicNotice, true);
                BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(UUID.fromString(clientUuid));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);*/

                notifyOwner(BLE_SERVICES_FOUND, gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            notifyOwner(102,null);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            notifyOwner(103,null);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            notifyOwner(104,null);
        }
    };

    public boolean open() {

        if(!adapter.isEnabled())
            return false;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        context.registerReceiver(receiver, filter);

        return true;
    }
    public boolean close(){
        context.unregisterReceiver(receiver);
        return true;
    }

    public boolean isOpened(){
        return adapter.isEnabled();
    }


    public boolean scanStart(){
        if(adapter.isDiscovering())
            adapter.cancelDiscovery();

        return adapter.startDiscovery();
    }

    public boolean scanStop(){
        if(!adapter.isDiscovering())
            return true;
        return adapter.cancelDiscovery();
    }

    public boolean isScanning(){
        return adapter.isDiscovering();
    }

    public boolean bleConnect(BluetoothDevice device){
        adapter.cancelDiscovery();
        bleDisconnect();
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        //if(mBluetoothGatt != null)
        //    mBluetoothGatt.connect();
        return (mBluetoothGatt!=null);
    }
    public boolean bleDisconnect(){
        if(isBleConnected()) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        return true;
    }
    public boolean isBleConnected(){
        if(mBluetoothGatt == null)
            return false;
        return mBluetoothGatt.connect();
    }
}
