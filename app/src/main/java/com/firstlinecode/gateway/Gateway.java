package com.firstlinecode.gateway;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.firstlinecode.chalk.AuthFailureException;
import com.firstlinecode.chalk.IChatClient;
import com.firstlinecode.chalk.core.stream.UsernamePasswordToken;
import com.firstlinecode.chalk.network.ConnectionException;
import com.firstlinecode.gateway.utils.SpUtil;
import com.firstlinecode.sand.client.ibdr.IRegistration;
import com.firstlinecode.sand.client.ibdr.IbdrError;
import com.firstlinecode.sand.client.ibdr.IbdrPlugin;
import com.firstlinecode.sand.client.ibdr.RegistrationException;
import com.firstlinecode.sand.client.lora.DynamicAddressConfigurator;
import com.firstlinecode.sand.client.lora.IDualLoraChipsCommunicator;
import com.firstlinecode.sand.client.things.concentrator.IConcentrator;
import com.firstlinecode.sand.emulators.lora.DualLoraChipsCommunicator;
import com.firstlinecode.sand.emulators.lora.ILoraNetwork;
import com.firstlinecode.sand.emulators.lora.LoraChip;
import com.firstlinecode.sand.emulators.lora.LoraChipCreationParams;
import com.firstlinecode.sand.emulators.lora.LoraNetwork;
import com.firstlinecode.sand.protocols.core.DeviceIdentity;
import com.firstlinecode.sand.protocols.lora.DualLoraAddress;
import com.firstlinecode.sand.protocols.lora.LoraAddress;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gateway implements DynamicAddressConfigurator.Listener {
    private DeviceIdentity deviceIdentity;

    private boolean isReconnect;
    private boolean interceptReconnect;

    private DynamicAddressConfigurator addressConfigurator;
    private IConcentrator concentrator;
    private IDualLoraChipsCommunicator gatewayCommunicator;

    public Gateway() {
        ILoraNetwork network = new LoraNetwork();
        gatewayCommunicator = DualLoraChipsCommunicator.createInstance(
                network, DualLoraAddress.randomDualLoraAddress(0), new LoraChipCreationParams(
                        LoraChip.Type.HIGH_POWER));

        reconnect();
    }

    DeviceIdentity register() {
        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client register start ====");
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

            // 注册成功，保存deviceIdentity
            SpUtil.putString(App.getAppContext(), App.getAppContext().getString(R.string.gateway_config_device_identity), new Gson().toJson(deviceIdentity));
        }


        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client register end ====");
        }

        return deviceIdentity;
    }

    @SuppressLint("HardwareIds")
    private String getDeviceId() {
        // 使用Build.SERIAL生成唯一码
        String serialNum = Build.SERIAL;
        int length = serialNum.length();

        return "GE01" + serialNum.substring(length - 8, length);
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
                if (IbdrError.CONNECTION_ERROR.equals(ex.getError())) {
                    // 连接异常
                    Log.d(Toolkits.logTag(App.getAppContext()), "设备连接异常！");
                } else if (IbdrError.NOT_AUTHORIZED.equals(ex.getError())) {
                    // 未认证
                    Log.d(Toolkits.logTag(App.getAppContext()), "设备未进行认证！");
                } else if (IbdrError.CONFLICT.equals(ex.getError())) {
                    // 该网关已注册
                    deviceIdentity = getDeviceIdentityFromCache();
                    return deviceIdentity;
                }

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

        deviceIdentity = getDeviceIdentityFromCache();
        if (deviceIdentity == null) {
            if (Toolkits.showLog(App.getAppContext())) {
                Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client connect fail: deviceIdentity is null ====");
            }

            return;
        }

        doConnect();

        if (Toolkits.showLog(App.getAppContext())) {
            Log.d(Toolkits.logTag(App.getAppContext()), "==== gateway client connect end ====");
        }
    }

    private DeviceIdentity getDeviceIdentityFromCache() {
        String deviceIdentityJson = Toolkits.getDeviceIdentity(App.getAppContext());
        DeviceIdentity deviceIdentity = new Gson().fromJson(deviceIdentityJson, DeviceIdentity.class);
        if (!deviceIdentity.getDeviceName().equals(getDeviceId())) {
            return null;
        }

        return deviceIdentity;
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

            concentrator = createConcentrator();
            addressConfigurator = new DynamicAddressConfigurator(gatewayCommunicator, concentrator);
            addressConfigurator.addListener(this);
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

    private IConcentrator createConcentrator() {
        return null;
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

    synchronized void setToWorkingMode() {
        addressConfigurator.stop();
        startWorking(ChatClientSingleton.get());
    }

    private void startWorking(IChatClient iChatClient) {

    }

    synchronized void setToAddressConfigurationMode() {
        addressConfigurator.start();
        stopWorking(ChatClientSingleton.get());
    }

    private void stopWorking(IChatClient iChatClient) {

    }

    /**
     * 动态地址分配成功后回调
     *
     * @param deviceId 设备id
     * @param address  lora地址
     */
    @Override
    public void addressConfigured(String deviceId, LoraAddress address) {

    }
}
