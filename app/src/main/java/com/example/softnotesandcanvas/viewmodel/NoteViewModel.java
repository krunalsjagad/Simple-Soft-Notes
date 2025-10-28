package com.example.softnotesandcanvas.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// ✅ Import the single, consolidated Note model from the 'db' package
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.repository.NoteRepository;

import java.util.List;
import java.util.UUID;

// ✅ Updated to use the consolidated 'db.Note' model
public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository repo;
    private final MutableLiveData<String> currentUid = new MutableLiveData<>();
    private LiveData<List<Note>> notes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repo = new NoteRepository(application);
    }

    /**
     * Sets the user ID and initializes the Firestore listener.
     * This loads the notes from the local DB and listens for remote changes.
     * @param uid The Firebase user ID.
     */
    public void loadNotesForUser(String uid) {
        if (uid == null || uid.equals(currentUid.getValue())) {
            return;
        }
        currentUid.setValue(uid);
        // ✅ Start the Firestore listener for real-time updates
        repo.startFirestoreListener(uid);
        // ✅ Get the LiveData for notes, which is now sourced from the local DB
        // but updated by the Firestore listener.
        notes = repo.getNotesForUser(uid);
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    /**
     * Creates a new note, saves it locally, and queues it for sync.
     * @param title The title of the note.
     * @param content The content of the note.
     */
    public void insert(String title, String content) {
        String uid = currentUid.getValue();
        if (uid == null) return; // User must be logged in
        repo.insert(title, content, uid);
    }

    /**
     * Updates an existing note, saves it locally, and queues it for sync.
     * @param note The note object with updated title and/or content.
     */
    public void update(Note note) {
        String uid = currentUid.getValue();
        if (uid == null) return; // User must be logged in
        repo.update(note);
    }

    /**
     * Soft-deletes a note locally and queues the delete operation for sync.
     * @param note The note to delete.
     */
    public void delete(Note note) {
        repo.delete(note);
    }
}
