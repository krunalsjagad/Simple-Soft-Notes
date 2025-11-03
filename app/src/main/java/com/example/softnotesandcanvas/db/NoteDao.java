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
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateNote(Note note);

    /**
     * Gets all non-trashed notes for a specific user.
     * This is the main query for the MainActivity list.
     *
     * @param userId The user's Firebase UID.
     * @return A LiveData list of active notes for that user.
     */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 ORDER BY updatedAt DESC")
    LiveData<List<Note>> getActiveNotesForUser(String userId);

    /**
     * ✅ NEW: Gets all trashed notes for a specific user.
     * This query is for the TrashActivity.
     *
     * @param userId The user's Firebase UID.
     * @return A LiveData list of trashed notes for that user.
     */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 1 ORDER BY updatedAt DESC")
    LiveData<List<Note>> getTrashedNotesForUser(String userId);

    /**
     * Gets a single note by its ID. Used by the editor.
     *
     * @param noteId The ID of the note to retrieve.
     * @return A LiveData-wrapped Note object.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<Note> getNoteByIdLiveData(String noteId);

    /**
     * Gets a single note by its ID (synchronous version).
     *
     * @param noteId The ID of the note to retrieve.
     * @return The Note object.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(String noteId);

    /**
     * ✅ NEW: Moves a note to the trash by setting the 'is_trashed' flag to true.
     * This is what you will call when a user swipes a note.
     *
     * @param noteId The ID of the note to move to trash.
     */
    @Query("UPDATE notes SET is_trashed = 1 WHERE id = :noteId")
    void trashNoteById(int noteId);

    /**
     * ✅ MODIFIED: Moves a note to the trash by setting the 'isDeleted' flag to true.
     * This is what you will call when a user swipes a note.
     *
     * @param noteId The ID of the note to move to trash.
     * @param timestamp The time of the deletion.
     * @param deviceId The ID of the device performing the deletion.
     */
    @Query("UPDATE notes SET is_trashed = 1, updatedAt = :timestamp, lastEditedByDeviceId = :deviceId, syncStatus = 'SYNCING' WHERE id = :noteId")
    void trashNote(String noteId, Date timestamp, String deviceId);

    /**
     * Updates the sync status of a specific note.
     */
    @Query("UPDATE notes SET syncStatus = :status WHERE id = :noteId")
    void updateSyncStatus(String noteId, SyncStatus status);

    /**
     * Deletes all notes from the table. Used for "Sign Out".
     */
    @Query("DELETE FROM notes")
    void nukeTable();
}