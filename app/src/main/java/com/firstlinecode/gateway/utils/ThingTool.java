package com.firstlinecode.gateway.utils;

import java.util.UUID;

/**
 * @Author: xb.zou
 * @Date: 2020/3/25
 * @Desc: to-> 设备工具类
 */
public class ThingTool {
    public static String generateRandomDeviceId() {
        return generateRandomDeviceId(12);
    }

    /**
     * 生成设备ID
     */
    public static String generateRandomDeviceId(int length) {
        if (length <= 16) {
            return String.format("%016X", UUID.randomUUID().getLeastSignificantBits())
                    .substring(16 - length, 16);
        }

        if (length > 32) {
            length = 32;
        }

        UUID uuid = UUID.randomUUID();
        String uuidStr = String.format("%016X%016X", uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        return uuidStr.substring(32 - length, length);
    }
}
