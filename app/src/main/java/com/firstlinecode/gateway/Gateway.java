package com.firstlinecode.gateway;

import android.text.TextUtils;
import android.util.Log;

import com.firstlinecode.chalk.AuthFailureException;
import com.firstlinecode.chalk.IChatClient;
import com.firstlinecode.chalk.core.stream.UsernamePasswordToken;
import com.firstlinecode.chalk.network.ConnectionException;
import com.firstlinecode.gateway.utils.SpUtil;
import com.firstlinecode.sand.client.ibdr.IRegistration;
import com.firstlinecode.sand.client.ibdr.IbdrPlugin;
import com.firstlinecode.sand.client.ibdr.RegistrationException;
import com.firstlinecode.sand.protocols.core.DeviceIdentity;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gateway {
    private DeviceIdentity deviceIdentity;
    private boolean isReconnect;
    private boolean interceptReconnect;

    public Gateway() {
        reconnect();
    }

    void register() {
        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client register start ====");
        }

        String deviceIdFromCache = Toolkits.getDeviceId(App.getAppContext());
        String deviceIdentityFromCache = Toolkits.getDeviceIdentity(App.getAppContext());
        if (!TextUtils.isEmpty(deviceIdFromCache) && !TextUtils.isEmpty(deviceIdentityFromCache)) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "the gateway[" + deviceIdFromCache + "] is registered!");
            }
            return;
        }

        String deviceId = getDeviceId();
        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "gateway serial is: " + deviceId);
        }

        deviceIdentity = doRegister(deviceId);
        if (deviceIdentity != null) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "DeviceIdentity[" + deviceIdentity.getDeviceName()
                        + ", " + deviceIdentity.getCredentials() + "]");
            }

            // 注册成功，保存deviceId和deviceIdentity
            SpUtil.putString(App.getAppContext(), App.getAppContext().getString(R.string.gateway_config_device_id), deviceId);
            SpUtil.putString(App.getAppContext(), App.getAppContext().getString(R.string.gateway_config_device_identity), new Gson().toJson(deviceIdentity));
        }


        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client register end ====");
        }
    }

    private String getDeviceId() {
        // TODO 使用Build.SERIAL生成唯一码
//        return "GE01" + ThingTool.generateRandomDeviceId(8);
        return "GE01" + "12345678";
    }

    private DeviceIdentity doRegister(String deviceId) {
        // 开始注册网关
        IChatClient chatClient = ChatClientSingleton.get();
        // 注册插件
        chatClient.register(IbdrPlugin.class);
        // 创建注册相关API server
        IRegistration registration = chatClient.createApi(IRegistration.class);
        // 添加连接时监听器
        DefaultConnectListener connectListener = new DefaultConnectListener();
        registration.addConnectionListener(connectListener);

        try {
            // 设备注册
            return registration.register(deviceId);
        } catch (RegistrationException ex) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "设备[" + deviceId + "]无法注册，原因为：" + ex.getMessage());
            }
            return null;
        } finally {
            registration.removeConnectionListener(connectListener);
            chatClient.close();
        }
    }

    void connect() {
        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client connect start ====");
        }

        if (deviceIdentity == null) {
            String deviceIdentityJson = Toolkits.getDeviceIdentity(App.getAppContext());
            deviceIdentity = new Gson().fromJson(deviceIdentityJson, DeviceIdentity.class);
        }

        doConnect();

        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client connect end ====");
        }
    }

    private void doConnect() {
        IChatClient chatClient = ChatClientSingleton.get();
        if (chatClient.isConnected()) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "网关已经处于连接状态");
            }

            return;
        }

        String deviceName = deviceIdentity.getDeviceName();
        String credentials = deviceIdentity.getCredentials();
        DefaultConnectListener connectListener = new DefaultConnectListener();
        chatClient.addConnectionListener(connectListener);

        try {
            chatClient.connect(new UsernamePasswordToken(deviceName, credentials));
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "网关连接成功");
            }

        } catch (ConnectionException e) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "网关连接异常：" + e.getMessage());
            }

            chatClient.close();
            chatClient = null;
        } catch (AuthFailureException e) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "网关设备认证失败：" + e.getMessage());
            }

            chatClient.close();
            chatClient = null;
        } catch (Exception e) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "其他异常：" + e.getMessage());
            }

            chatClient.close();
            chatClient = null;
        } finally {
            isReconnect = true;
            interceptReconnect = false;
            if (chatClient != null) {
                chatClient.removeConnectionListener(connectListener);
            }
        }
    }

    private void reconnect() {
        // TODO 使用线程池工具替换Executors
        ExecutorService threadPool = Executors.newCachedThreadPool();
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    boolean isDisconnect = !ChatClientSingleton.get().isConnected();
                    if (isReconnect && isDisconnect) {
                        doConnect();
                    }

                    if (interceptReconnect) {
                        break;
                    }
                }
            }
        });
    }
}
