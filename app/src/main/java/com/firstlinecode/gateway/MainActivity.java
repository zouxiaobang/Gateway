package com.firstlinecode.gateway;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

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
        gateway.register();
        gateway.connect();
    }
}
