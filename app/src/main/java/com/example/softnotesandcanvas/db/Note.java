package com.example.softnotesandcanvas.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity class that defines the 'notes' table in the Room database.
 * This class also serves as the data model object for the application.
 *
 * ✅ This is now the *single* source of truth for the Note model.
 * The redundant `model/Note.java` has been removed.
 */
@Entity(tableName = "notes")
// Tell Room to use our Converters class for this entity
@TypeConverters({Converters.class})
public class Note {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "userId")
    public String userId;

    @ColumnInfo(name = "title")
    public String title;

    // ✅ Renamed 'body' to 'content' to standardize the model
    @ColumnInfo(name = "content")
    public String content;

    // ✅ Added @ServerTimestamp for Firestore
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

    /**
     * Default constructor for Room and Firestore deserialization.
     */
    public Note() {
        // Public no-arg constructor is required by Room/Firestore
    }

    /**
     * Convenience constructor for creating a new, unsaved note.
     * Generates a new UUID, sets timestamps, and defaults states.
     *
     * @param userId The ID of the user creating the note.
     * @param title The title of the note.
     * @param content The body content of the note.
     * @param deviceId The unique ID of the device creating the note.
     */
    public Note(@NonNull String userId, String title, String content, @NonNull String deviceId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.content = content; // ✅ Updated field
        this.createdAt = new Date();
        this.updatedAt = this.createdAt;
        this.lastEditedByDeviceId = deviceId;
        this.isDeleted = false;
        this.syncStatus = SyncStatus.SYNCING; // New notes start as "SYNCING"
    }

    // --- Overriding equals and hashCode ---
    // This is good practice for entities to help ListAdapters and
    // other components differentiate between note objects.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return isDeleted == note.isDeleted &&
                id.equals(note.id) &&
                Objects.equals(userId, note.userId) &&
                Objects.equals(title, note.title) &&
                Objects.equals(content, note.content) && // ✅ Updated field
                Objects.equals(createdAt, note.createdAt) &&
                Objects.equals(updatedAt, note.updatedAt) &&
                Objects.equals(lastEditedByDeviceId, note.lastEditedByDeviceId) &&
                syncStatus == note.syncStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, title, content, createdAt, updatedAt, lastEditedByDeviceId, isDeleted, syncStatus); // ✅ Updated field
    }
}
