//
// [File: SoftNotesandCanvas/app/src/main/java/com/example/softnotesandcanvas/db/Note.java]
//
package com.example.softnotesandcanvas.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "notes")
@TypeConverters({Converters.class})
public class Note implements Serializable {

    // --- ADD THESE CONSTANTS ---
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_CANVAS = "CANVAS";
    // ---------------------------

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "userId")
    public String userId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "content")
    public String content; // Will be null for CANVAS notes

    // --- ADD THESE NEW FIELDS ---
    @ColumnInfo(name = "type", defaultValue = TYPE_TEXT)
    @NonNull
    public String type;

    @ColumnInfo(name = "canvasImagePath")
    public String canvasImagePath; // Will be null for TEXT notes
    // --------------------------

    @ServerTimestamp
    @ColumnInfo(name = "createdAt")
    public Date createdAt;

    @ServerTimestamp
    @ColumnInfo(name = "updatedAt")
    public Date updatedAt;

    @ColumnInfo(name = "lastEditedByDeviceId")
    public String lastEditedByDeviceId;

    @ColumnInfo(name = "isDeleted")
    public boolean isDeleted;

    @ColumnInfo(name = "syncStatus")
    public SyncStatus syncStatus;

    @ColumnInfo(name = "is_trashed", defaultValue = "0")
    public boolean isTrashed = false;

    /**
     * Default constructor for Room and Firestore deserialization.
     */
    public Note() {
        // Public no-arg constructor is required by Room/Firestore
        // --- ADD THESE DEFAULTS ---
        this.type = TYPE_TEXT;
        this.canvasImagePath = null;
        // --------------------------
    }

    /**
     * Convenience constructor for creating a new, unsaved TEXT note.
     * Generates a new UUID, sets timestamps, and defaults states.
     *
     * @param userId The ID of the user creating the note.
     * @param title The title of the note.
     * @param content The body content of the text note.
     * @param deviceId The unique ID of the device creating the note.
     */
    public Note(@NonNull String userId, String title, String content, @NonNull String deviceId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.createdAt = new Date();
        this.updatedAt = this.createdAt;
        this.lastEditedByDeviceId = deviceId;
        this.isDeleted = false;
        this.syncStatus = SyncStatus.SYNCING;

        // --- ADD THESE LINES ---
        // This constructor is for TEXT notes
        this.type = TYPE_TEXT;
        this.canvasImagePath = null;
        // ---------------------
    }

    // --- Overriding equals and hashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return isDeleted == note.isDeleted &&
                isTrashed == note.isTrashed && // Added isTrashed
                id.equals(note.id) &&
                Objects.equals(userId, note.userId) &&
                Objects.equals(title, note.title) &&
                Objects.equals(content, note.content) &&
                Objects.equals(createdAt, note.createdAt) &&
                Objects.equals(updatedAt, note.updatedAt) &&
                Objects.equals(lastEditedByDeviceId, note.lastEditedByDeviceId) &&
                syncStatus == note.syncStatus &&
                // --- ADD THESE LINES ---
                Objects.equals(type, note.type) &&
                Objects.equals(canvasImagePath, note.canvasImagePath);
        // ---------------------
    }

    @Override
    public int hashCode() {
        // --- UPDATE THIS LINE ---
        return Objects.hash(id, userId, title, content, type, canvasImagePath, createdAt, updatedAt, lastEditedByDeviceId, isDeleted, syncStatus, isTrashed);
        // ----------------------
    }
}