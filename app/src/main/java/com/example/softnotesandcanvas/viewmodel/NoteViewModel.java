package com.example.softnotesandcanvas.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.repository.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    // ✅ Use a single, consistent name for the repository
    private final NoteRepository mRepository;
    private final MutableLiveData<String> currentUid = new MutableLiveData<>();
    private LiveData<List<Note>> notes;

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
        notes = mRepository.getNotesForUser(uid);
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

    /**
     * Updates an existing note locally and queues it for sync.
     */
    public void update(Note note) {
        String uid = currentUid.getValue();
        if (uid == null) return;
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