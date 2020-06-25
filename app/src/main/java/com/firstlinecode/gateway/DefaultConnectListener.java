package com.firstlinecode.gateway;

import android.util.Log;

import com.firstlinecode.chalk.network.ConnectionException;
import com.firstlinecode.chalk.network.IConnectionListener;

public class DefaultConnectListener implements IConnectionListener {

    @Override
    public void occurred(ConnectionException exception) {
        Log.d(Toolkits.logTag(App.getAppContext()), "发生错误\n " + exception.getMessage());
    }

    @Override
    public void received(String message) {
        Log.d(Toolkits.logTag(App.getAppContext()), "接收【S --> G】: " + message);
    }

    @Override
    public void sent(String message) {
        Log.d(Toolkits.logTag(App.getAppContext()), "发送【G --> S】: " + message);
    }
}
