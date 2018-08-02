package com.ble.devcie;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.MyUtil;

import java.util.List;
import java.util.UUID;

/**
 * Created by jhc on 2018/8/2.
 */

public class MyBleDevice {

    private String TAG = "MyBleDevice";

    private static final UUID serviceUuid = UUID.fromString("00006006-0000-1000-8000-00805f9b34fb");
    private static final UUID tx_characterUuid = UUID.fromString("00008001-0000-1000-8000-00805f9b34fb");
    private static final UUID rx_characterUuid = UUID.fromString("00008002-0000-1000-8000-00805f9b34fb");
    private static final UUID rx_DescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic txChar;
    private BluetoothGattCharacteristic rxChar;

    private BleDeviceCallBack callBack;

    public void setCallBack(BleDeviceCallBack callBack) {
        this.callBack = callBack;
    }

    public boolean matchAndSetMyBleDevice(BluetoothGatt gatt){
        BluetoothGattCharacteristic txChar = null;
        BluetoothGattCharacteristic rxChar = null;

        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(serviceUuid)) {
                Log.d(TAG, "target service found: " + service.getUuid());

                List<BluetoothGattCharacteristic> cs = service.getCharacteristics();
                for (BluetoothGattCharacteristic mChar : cs) {
                    if (mChar.getUuid().equals(tx_characterUuid)) {
                        txChar = mChar;
                        Log.d(TAG, "target tx-char found: " + mChar.getUuid());
                    } else if (mChar.getUuid().equals(rx_characterUuid)) {
                        rxChar = mChar;
                        Log.d(TAG, "target rx-char found: " + mChar.getUuid());

                        //enable notification
                        gatt.setCharacteristicNotification(mChar, true);
                        BluetoothGattDescriptor descriptor = mChar.getDescriptor(rx_DescriptorUuid);
                        if(descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            boolean r = gatt.writeDescriptor(descriptor);
                        }
                    }
                    if((txChar != null) && (rxChar != null)) {
                        this.gatt = gatt;
                        this.service = service;
                        this.txChar = txChar;
                        this.rxChar = rxChar;
                        break;
                    }
                }
            }
        }
        return ((txChar != null) && (rxChar != null));
    }


    public boolean sendToMyBleDevice(byte cmd, byte dir, byte[] data){

        if(gatt == null)
            return false;

        BluetoothGattService service = gatt.getService(serviceUuid);
        if(service == null)
            return false;

        BluetoothGattCharacteristic txChar = service.getCharacteristic(tx_characterUuid);
        BluetoothGattCharacteristic rxChar = service.getCharacteristic(rx_characterUuid);

        if ((txChar == null) || (rxChar == null))
            return false;

        int tryTimes;

        //build pack, and send them to 8001
        byte[] values = Prot.pack(cmd, dir, data);
        int length = values.length;

            while(length > 0) {
                int copyLength = (length > 20) ? 20: length;
                byte[] tmpValues = new byte[copyLength];
                System.arraycopy(values, 0, tmpValues, 0, copyLength);
                length -= copyLength;

                txChar.setValue(tmpValues);
                tryTimes = 50;
                while (--tryTimes > 0) {
                    if (gatt.writeCharacteristic(txChar))
                        break;
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            Log.d(TAG, "sendToMyBleDevice: " + tryTimes);
            if(tryTimes == 0)
                return false;
        }

        //send 0x03 to 8002
        byte[] confirmByte =  new byte[]{0x03};
        rxChar.setValue(confirmByte);
        tryTimes = 50;
        while(--tryTimes > 0) {
            if(gatt.writeCharacteristic(rxChar))
                break;

            try{
                Thread.sleep(20);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.d(TAG, "sendToMyBleDevice: "+ tryTimes);
        if(tryTimes == 0)
            return false;

        return true;
    }

    public boolean recvFromMyBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){

        if((gatt == null) || (this.gatt != gatt))
            return false;

        if(callBack != null){
            callBack.deviceReceive((byte)0, (byte)1,characteristic.getValue());
        }
        return true;
    }
}
