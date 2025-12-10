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
     * Gets all non-trashed, non-deleted notes for a specific user.
     * This is the main query for the MainActivity list.
     *
     * @param userId The user's Firebase UID.
     * @return A LiveData list of active notes for that user.
     */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<Note>> getActiveNotesForUser(String userId);

    /**
     * ✅ UPDATED: Gets all trashed, non-deleted notes for a specific user.
     * This query is for the TrashActivity.
     *
     * @param userId The user's Firebase UID.
     * @return A LiveData list of trashed notes for that user.
     */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
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
     * ✅ NEW: Restores a note from the trash by setting 'is_trashed' flag to false.
     * This is used for the "Undo" action and restoring from the trash.
     *
     * @param noteId The ID of the note to restore.
     * @param timestamp The time of the restoration.
     * @param deviceId The ID of the device performing the action.
     */
    @Query("UPDATE notes SET is_trashed = 0, updatedAt = :timestamp, lastEditedByDeviceId = :deviceId, syncStatus = 'SYNCING' WHERE id = :noteId")
    void restoreNote(String noteId, Date timestamp, String deviceId);

    /**
     * ✅ NEW: Marks a note as permanently deleted.
     * This will be picked up by the sync manager to delete from Firestore
     * and will be hidden from all UIs.
     *
     * @param noteId The ID of the note to mark as deleted.
     * @param timestamp The time of the deletion.
     * @param deviceId The ID of the device performing the action.
     */
    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :timestamp, lastEditedByDeviceId = :deviceId, syncStatus = 'SYNCING' WHERE id = :noteId")
    void markAsDeleted(String noteId, Date timestamp, String deviceId);


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
    /**
     * ✅ NEW: Searches for notes that match the query in title or content.
     * Checks against userId and ensures notes are not trashed or deleted.
     */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 AND isDeleted = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<Note>> searchNotes(String userId, String query);

    // ------------------------------------------------------------
    // ✅ NEW FILTERING QUERIES (Text-only, Canvas-only, with/without search)
    // ------------------------------------------------------------

    /** 1. Get ONLY Text Notes (canvasImagePath is empty or null) */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 AND isDeleted = 0 AND (canvasImagePath IS NULL OR canvasImagePath = '') ORDER BY updatedAt DESC")
    LiveData<List<Note>> getTextNotesOnly(String userId);

    /** 2. Get ONLY Canvas Notes (canvasImagePath is not empty) */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 AND isDeleted = 0 AND (canvasImagePath IS NOT NULL AND canvasImagePath != '') ORDER BY updatedAt DESC")
    LiveData<List<Note>> getCanvasNotesOnly(String userId);

    /** 3. SEARCH within Text Notes Only */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 AND isDeleted = 0 AND (canvasImagePath IS NULL OR canvasImagePath = '') AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<Note>> searchTextNotes(String userId, String query);

    /** 4. SEARCH within Canvas Notes Only */
    @Query("SELECT * FROM notes WHERE userId = :userId AND is_trashed = 0 AND isDeleted = 0 AND (canvasImagePath IS NOT NULL AND canvasImagePath != '') AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<Note>> searchCanvasNotes(String userId, String query);

}