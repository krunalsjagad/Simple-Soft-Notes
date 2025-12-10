package com.example.softnotesandcanvas.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.repository.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    // ✅ Use a single, consistent name for the repository
    private final NoteRepository mRepository;
    private final MutableLiveData<String> currentUid = new MutableLiveData<>();
    // ✅ CHANGED: Use MediatorLiveData to switch between all notes and search results
    private final MediatorLiveData<List<Note>> notes = new MediatorLiveData<>();
    private LiveData<List<Note>> currentSource; // Tracks the current active LiveData source

    public NoteViewModel(@NonNull Application application) {
        super(application);
        // ✅ Initialize the correct repository variable
        mRepository = new NoteRepository(application);
    }

    /**
     * Sets the user ID and initializes the data loading and sync listeners.
     * @param uid The Firebase user ID.
     */
    public void loadNotesForUser(String uid) {
        if (uid == null || uid.equals(currentUid.getValue())) {
            return;
        }
        currentUid.setValue(uid);
        mRepository.startFirestoreListener(uid);
        // Correctly get the active notes for the main screen
        updateNotesSource(uid, "");
    }

    /**
     * ✅ NEW: Called when the user types in the search bar.
     */
    public void search(String query) {
        String uid = currentUid.getValue();
        if (uid != null) {
            updateNotesSource(uid, query);
        }
    }

    /**
     * Helper to switch the data source of the 'notes' LiveData.
     */
    private void updateNotesSource(String uid, String query) {
        // 1. Remove the old source so we don't get updates from the wrong query
        if (currentSource != null) {
            notes.removeSource(currentSource);
        }

        // 2. Decide which source to use
        if (query == null || query.trim().isEmpty()) {
            currentSource = mRepository.getNotesForUser(uid);
        } else {
            currentSource = mRepository.searchNotes(uid, query);
        }

        // 3. Add the new source
        notes.addSource(currentSource, notes::setValue);
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    /**
     * ✅ NEW: Gets the list of trashed notes for the TrashActivity.
     * @param userId The current user's ID.
     * @return LiveData list of trashed notes.
     */
    public LiveData<List<Note>> getTrashedNotes(String userId) {
        // This will call the getTrashedNotesForUser method in your repository
        return mRepository.getTrashedNotes(userId);
    }

    /**
     * Creates a new note, saves it locally, and queues it for sync.
     */
    public void insert(String title, String content) {
        String uid = currentUid.getValue();
        if (uid == null) return;
        mRepository.insert(title, content, uid);
    }

    // --- ADD THIS NEW METHOD ---
    /**
     * Inserts a pre-constructed Note object.
     * @param note The Note to insert.
     */
    public void insert(Note note) {
        // Note object must have userId, deviceId, etc., set BEFORE calling this.
        mRepository.insert(note);
    }

    /**
     * Updates an existing note locally and queues it for sync.
     */
    public void update(Note note) {
        mRepository.update(note);
    }

    /**
     * ✅ MODIFIED: Moves a note to the trash instead of permanently deleting it.
     * This method will be called when a user swipes a note.
     * @param note The note to move to the trash.
     */
    public void trash(Note note) {
        // This should call the 'trash' method in your repository,
        // which in turn calls the 'trashNote' method in your DAO.
        mRepository.trash(note);
    }

    /**
     * ✅ NEW: Restores a note from the trash.
     */
    public void restore(Note note) {
        mRepository.restore(note);
    }

    /**
     * ✅ NEW: Permanently deletes a note (marks as deleted).
     */
    public void deletePermanently(Note note) {
        mRepository.deletePermanently(note);
    }
}