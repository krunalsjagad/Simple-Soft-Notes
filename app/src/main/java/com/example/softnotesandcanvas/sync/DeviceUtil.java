package com.example.softnotesandcanvas.sync;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Utility class to provide a unique, persistent ID for this device.
 * This ID is used to tag edits for conflict resolution.
 */
public class DeviceUtil {

    private static final String PREFS_FILE = "device_prefs";
    private static final String PREF_DEVICE_ID = "device_id";
    private static volatile String sDeviceId;

    /**
     * Gets the unique device ID.
     * Generates and saves one if it doesn't exist.
     *
     * @param context The application context.
     * @return A unique, persistent device ID.
     */
    public static String getDeviceId(Context context) {
        // Use a volatile variable for fast, thread-safe double-checked locking
        if (sDeviceId == null) {
            synchronized (DeviceUtil.class) {
                if (sDeviceId == null) {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
                    sDeviceId = prefs.getString(PREF_DEVICE_ID, null);

                    if (sDeviceId == null) {
                        sDeviceId = UUID.randomUUID().toString();
                        prefs.edit().putString(PREF_DEVICE_ID, sDeviceId).apply();
                    }
                }
            }
        }
        return sDeviceId;
    }
}
