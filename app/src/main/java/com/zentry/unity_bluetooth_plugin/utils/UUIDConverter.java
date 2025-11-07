package com.zentry.unity_bluetooth_plugin.utils;

import java.util.UUID;

public class UUIDConverter {
    private static final String BASE_UUID = "00000000-0000-1000-8000-00805F9B34FB";
    private static final int SHORT_UUID_LENGTH = 4;
    private static final int FULL_UUID_LENGTH = 36;

    public static String normalize(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }

        uuid = uuid.toUpperCase().trim();

        if (uuid.length() == SHORT_UUID_LENGTH) {
            return expandShortUUID(uuid);
        }

        if (uuid.length() == FULL_UUID_LENGTH) {
            return uuid;
        }

        return uuid;
    }

    private static String expandShortUUID(String shortUUID) {
        return BASE_UUID.substring(0, 4) + shortUUID + BASE_UUID.substring(8);
    }

    public static boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(normalize(uuid));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
