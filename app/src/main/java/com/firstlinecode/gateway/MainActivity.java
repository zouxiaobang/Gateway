package com.firstlinecode.gateway;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.firstlinecode.sand.protocols.core.DeviceIdentity;

public class MainActivity extends AppCompatActivity {
    private Gateway gateway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gateway = new Gateway();

        new Thread(){
            @Override
            public void run() {
                register();
            }
        }.start();
    }

    private void register() {
        DeviceIdentity deviceIdentity = gateway.register();
        if (deviceIdentity != null) {
            gateway.connect();
        }
    }
}
