package com.ble.myBle;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


class BleDevice {
    public BluetoothGatt mBleGatt;
    public BluetoothDevice mBleDevice;

    public BleDevice() {
        this.mBleGatt = null;
        this.mBleDevice = null;
    }
};

public class MyBle{

    private static final String TAG = "MyBle";


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
    public static final int BLE_CHARACTERISTIC_WRITE = 10;
    public static final int BLE_CHARACTERISTIC_READ = 11;
    public static final int BLE_CHARACTERISTIC_CHANGED = 12;


    private Context context;
    private MyBleCallback callback;

    private BluetoothAdapter adapter;

    private BleDevice mBleDevice;


    public MyBle(Context context, MyBleCallback callback) {
        this.context = context;
        this.callback = callback;

        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        //adapter = BluetoothAdapter.getDefaultAdapter();

        if(!adapter.isEnabled()) {
            adapter.enable();

            /* Intent turn_on = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(turn_on); */
        }
        mBleDevice = new BleDevice();
    }

    private void notifyOwner(int what, Object... obj){
        if(callback != null){
            callback.notify(what, obj);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d(TAG, "RecvBroadcast: "+action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {}
                notifyOwner(BLE_DEVICE_FOUND, device);

            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                notifyOwner(BLE_SCAN_STARTED);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                notifyOwner(BLE_SCAN_COMPLETED);
            }
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
        try {
            context.unregisterReceiver(receiver);
        }catch(Exception e){
            e.printStackTrace();
        }
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


    private boolean isCurrentBleGatt(BluetoothGatt gatt){
            return gatt.equals(mBleDevice.mBleGatt);
    }

    public BluetoothGatt getCurrentBleGatt(){
        return mBleDevice.mBleGatt;
    }

    public BluetoothDevice getCurrentBleDevice(){
        return mBleDevice.mBleDevice;
    }

    private BluetoothGattCallback mGattCallback =  new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //Log.d(TAG, "onConnectionStateChange: "+newState);
            super.onConnectionStateChange(gatt, status, newState);
            //if (status == BluetoothGatt.GATT_SUCCESS) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                if(isCurrentBleGatt(gatt)) {
                    gatt.discoverServices();
                    notifyOwner(BLE_DEVICE_CONNECTED, gatt);
                }
            }
            else if(newState == BluetoothProfile.STATE_CONNECTING){
                if(isCurrentBleGatt(gatt)) {
                    notifyOwner(BLE_DEVICE_CONNECTING, gatt);
                }
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                if(isCurrentBleGatt(gatt)) {
                    notifyOwner(BLE_DEVICE_DISCONNECTED, gatt);
                    if (status != BluetoothGatt.GATT_FAILURE) {
                        //gatt.close();
                    }
                    mBleDevice.mBleGatt.close();
                    mBleDevice.mBleGatt = null;
                }
            }
            else{}
            //}
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(isCurrentBleGatt(gatt)) {
                    notifyOwner(BLE_SERVICES_FOUND, gatt);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(isCurrentBleGatt(gatt)) {
                notifyOwner(BLE_CHARACTERISTIC_READ, gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(isCurrentBleGatt(gatt)) {
                notifyOwner(BLE_CHARACTERISTIC_WRITE, gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if(isCurrentBleGatt(gatt)) {
                notifyOwner(BLE_CHARACTERISTIC_CHANGED, gatt, characteristic);
            }
        }
    };


    public boolean bleConnect(BluetoothDevice device){
        if(device == null)
            return false;

        adapter.cancelDiscovery();

        if(mBleDevice.mBleGatt != null) {
            mBleDevice.mBleGatt.disconnect();
            mBleDevice.mBleGatt.close();
        }
        mBleDevice.mBleDevice = device;
        mBleDevice.mBleGatt = mBleDevice.mBleDevice.connectGatt(context, true, mGattCallback);
        if(mBleDevice.mBleGatt != null) {
            notifyOwner(BLE_DEVICE_CONNECTING, mBleDevice.mBleGatt);
            //mBleDevice.mBleGatt.connect();
        }
        return (mBleDevice.mBleGatt != null);
    }
    public boolean bleDisconnect(){
        if(mBleDevice.mBleGatt != null) {
            mBleDevice.mBleGatt.disconnect();
        }
        return true;
    }
    public boolean isBleConnected(){
        if(mBleDevice.mBleGatt == null)
            return false;
        return mBleDevice.mBleGatt.connect();
    }
}
