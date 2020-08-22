package com.firstlinecode.gateway;

import android.content.Context;

import com.firstlinecode.chalk.core.stream.StandardStreamConfig;
import com.firstlinecode.gateway.utils.SpUtil;

public class Toolkits {
    static boolean showLog(Context context) {
        return SpUtil.getBoolean(context, context.getString(R.string.log_config_show),
                Boolean.parseBoolean(context.getString(R.string.log_config_default_show)));
    }
    static String logTag(Context context) {
        return SpUtil.getString(context, context.getString(R.string.log_config_tag),
                context.getString(R.string.log_config_default_tag));
    }

    static StandardStreamConfig getStreamConfig(Context context) {
        String host = SpUtil.getString(context, context.getString(R.string.stream_config_host),
                context.getString(R.string.stream_config_default_host));
        int port = SpUtil.getInt(context, context.getString(R.string.stream_config_port),
                Integer.parseInt(context.getString(R.string.stream_config_default_port)));

        boolean enableTls = SpUtil.getBoolean(context, context.getString(R.string.stream_config_enable_tls),
                Boolean.parseBoolean(context.getString(R.string.stream_config_default_enable_tls)));

        StandardStreamConfig streamConfig = new StandardStreamConfig(host, port);
        streamConfig.setTlsPreferred(enableTls);
        streamConfig.setResource("gateway");

        return streamConfig;
    }

    static String getDeviceIdentity(Context context) {
        return SpUtil.getString(context, context.getString(R.string.gateway_config_device_identity),
                "");
    }
}
