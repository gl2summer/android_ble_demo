package com.bsa.jhc.android_ble_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class DeviceActivity extends AppCompatActivity {

    private Button btn_ota;
    private Button btn_get_info;
    private TextView tv_log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        btn_ota = (Button)findViewById(R.id.btn_OTA);
        btn_get_info = (Button)findViewById(R.id.btn_get_info);
        tv_log = (TextView)findViewById(R.id.tv_log);

        //BleBundle bleBundle = (BleBundle) getIntent().getSerializableExtra("device");

        tv_log.append("log1\n");
    }
}
