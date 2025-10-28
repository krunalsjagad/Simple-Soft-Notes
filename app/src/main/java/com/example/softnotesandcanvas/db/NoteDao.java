package com.example.softnotesandcanvas.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object (DAO) for the Note entity.
 * This is where all database operations are defined.
 */
@Dao
public interface NoteDao {

    /**
     * Inserts a new note or replaces an existing one.
     * If a note with the same 'id' already exists, it will be replaced.
     *
     * @param note The note to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateNote(Note note);

    /**
     * Gets all non-deleted notes, ordered by the last update time (newest first).
     * Returns as LiveData, so the UI can automatically update when the data changes.
     *
     * @return A LiveData list of all active notes.
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<Note>> getAllActiveNotes();

    /**
     * ✅ ADDED: Gets all non-deleted notes *for a specific user*.
     * This is the query the ViewModel will observe.
     *
     * @param userId The user's Firebase UID.
     * @return A LiveData list of active notes for that user.
     */
    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<Note>> getAllActiveNotesForUser(String userId);


    /**
     * Gets a single note by its ID. Used by the editor.
     * Returns as LiveData for reactive UI updates.
     *
     * @param noteId The ID of the note to retrieve.
     * @return A LiveData-wrapped Note object.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<Note> getNoteByIdLiveData(String noteId);

    /**
     * Gets a single note by its ID.
     * This is a synchronous (non-LiveData) version for use in background
     * threads, like the SyncWorker.
     *
     * @param noteId The ID of the note to retrieve.
     * @return The Note object.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(String noteId);

    /**
     * Performs a "soft delete" by setting the 'isDeleted' flag to true
     * and updating the timestamp and sync status. The note will be
     * synced (to delete on remote) and then filtered from the main list.
     *
     * @param noteId The ID of the note to soft-delete.
     * @param timestamp The time of the deletion.
     * @param deviceId The ID of the device performing the deletion.
     */
    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :timestamp, lastEditedByDeviceId = :deviceId, syncStatus = 'SYNCING' WHERE id = :noteId")
    void softDeleteNote(String noteId, Date timestamp, String deviceId);

    /**
     * Updates the sync status of a specific note.
     * This will be called by the SyncWorker on success, failure, or conflict.
     *
     * @param noteId The ID of the note to update.
     * @param status The new SyncStatus.
     */
    @Query("UPDATE notes SET syncStatus = :status WHERE id = :noteId")
    void updateSyncStatus(String noteId, SyncStatus status);

    /**
     * Deletes all notes from the table. Used for "Clear Cache" or "Sign Out".
     */
    @Query("DELETE FROM notes")
    void nukeTable();
}
