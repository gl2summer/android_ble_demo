package com.bsa.jhc.android_ble_demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jhc on 2018/7/10.
 */

public class BleAdapter extends ArrayAdapter {

    private final int resourceId;
    private int selectedItemIndex = -1;

    public BleAdapter(Context context, int textViewResourceId, List<Object> objects) {
        super(context, textViewResourceId, objects);
         resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BluetoothDevice device = (BluetoothDevice) getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
        TextView name = (TextView) view.findViewById(R.id.device_name);
        name.setText(device.getName()+" - "+device.getAddress());
        if(position == selectedItemIndex){
            view.setBackgroundColor(Color.RED);
        } else{
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        return view;
    }
    public void setSelectItemIndex(int index){
        selectedItemIndex = index;
        notifyDataSetInvalidated();
        //notifyDataSetChanged();
        Log.d("", "setSelectItemIndex: "+index);
    }
    public int getSelectedItemIndex(){
        return selectedItemIndex;
    }
}
