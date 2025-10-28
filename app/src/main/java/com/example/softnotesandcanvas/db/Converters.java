package com.example.softnotesandcanvas.db;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * Type converters for Room to handle types it doesn't natively support,
 * like Date and our custom SyncStatus enum.
 */
public class Converters {

    // --- Date Converters ---

    /**
     * Converts a Long timestamp back into a Date object.
     * @param value The timestamp from the database.
     * @return A Date object, or null if the timestamp was null.
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * Converts a Date object into a Long timestamp for database storage.
     * @param date The Date object.
     * @return A Long timestamp, or null if the Date was null.
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // --- SyncStatus Converters ---

    /**
     * Converts a String from the database back into a SyncStatus enum.
     * @param value The String value from the database (e.g., "SYNCED").
     * @return The corresponding SyncStatus enum, or SYNCED by default if invalid.
     */
    @TypeConverter
    public static SyncStatus fromString(String value) {
        if (value == null) {
            return SyncStatus.SYNCED;
        }
        try {
            return SyncStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Handle invalid data from DB, default to SYNCED
            return SyncStatus.SYNCED;
        }
    }

    /**
     * Converts a SyncStatus enum into a String for database storage.
     * @param status The SyncStatus enum.
     * @return A String representation (e.g., "SYNCING").
     */
    @TypeConverter
    public static String statusToString(SyncStatus status) {
        return status == null ? null : status.name();
    }
}