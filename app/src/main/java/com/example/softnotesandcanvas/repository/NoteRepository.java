package com.example.softnotesandcanvas.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.softnotesandcanvas.db.AppDatabase;
import com.example.softnotesandcanvas.db.NoteDao;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.db.SyncStatus;
import com.example.softnotesandcanvas.sync.DeviceUtil;
import com.example.softnotesandcanvas.sync.SyncManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages both local (Room) and remote (Firestore) data, acting as the Single Source of Truth.
 */
public class NoteRepository {
    private static final String TAG = "NoteRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_NOTES = "notes";

    private final NoteDao noteDao;
    private final ExecutorService io;
    private final FirebaseFirestore firestore;
    private final SyncManager syncManager;
    private final String deviceId;
    // ✅ Store application context to use in methods
    private final Application application;

    private ListenerRegistration firestoreListener;

    public NoteRepository(Application app) {
        // ✅ Assign the application context to the class field
        this.application = app;
        AppDatabase db = AppDatabase.getInstance(app);
        noteDao = db.noteDao();
        io = Executors.newSingleThreadExecutor();
        firestore = FirebaseFirestore.getInstance();
        syncManager = new SyncManager(app.getApplicationContext());
        deviceId = DeviceUtil.getDeviceId(app.getApplicationContext());
    }

    /**
     * ✅ CORRECTED: Use the renamed DAO method to get active notes.
     */
    public LiveData<List<Note>> getNotesForUser(String uid) {
        return noteDao.getActiveNotesForUser(uid);
    }

    /**
     * ✅ CORRECTED: This method is now correctly wired to the DAO.
     */
    public LiveData<List<Note>> getTrashedNotes(String userId) {
        return noteDao.getTrashedNotesForUser(userId);
    }

    /**
     * ✅ CORRECTED: This method now correctly moves a note to the trash.
     * It uses the correct executor 'io' and the 'application' context.
     */
    public void trash(Note note) {
        io.execute(() -> {
            noteDao.trashNote(note.id, new Date(), DeviceUtil.getDeviceId(application));
            // Also schedule a sync to update Firestore
            syncManager.scheduleSync(note.id);
        });
    }

    /**
     * ✅ NEW: Restores a note from the trash.
     */
    public void restore(Note note) {
        io.execute(() -> {
            noteDao.restoreNote(note.id, new Date(), DeviceUtil.getDeviceId(application));
            syncManager.scheduleSync(note.id);
        });
    }

    /**
     * ✅ NEW: Marks a note for permanent deletion.
     */
    public void deletePermanently(Note note) {
        io.execute(() -> {
            noteDao.markAsDeleted(note.id, new Date(), DeviceUtil.getDeviceId(application));
            syncManager.scheduleSync(note.id);
        });
    }

    public void startFirestoreListener(String uid) {
        if (uid == null) return;

        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        Query query = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_NOTES)
                .orderBy("updatedAt", Query.Direction.DESCENDING);

        firestoreListener = query.addSnapshotListener(io, (snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Firestore listener failed.", e);
                return;
            }
            if (snapshots == null) {
                return;
            }

            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                Note remoteNote = dc.getDocument().toObject(Note.class);

                if (deviceId.equals(remoteNote.lastEditedByDeviceId)) {
                    Log.d(TAG, "Ignoring echo of our own change for note: " + remoteNote.id);
                    continue;
                }

                Note localNote = noteDao.getNoteById(remoteNote.id);
                boolean isConflict = false;

                if (localNote != null && localNote.syncStatus != SyncStatus.SYNCED) {
                    if (remoteNote.updatedAt != null && remoteNote.updatedAt.after(localNote.updatedAt)) {
                        isConflict = true;
                    } else {
                        Log.w(TAG, "Conflict detected, but local is newer. Ignoring remote change for: " + remoteNote.id);
                        continue;
                    }
                }

                switch (dc.getType()) {
                    case ADDED:
                    case MODIFIED:
                        if (isConflict) {
                            Log.w(TAG, "CONFLICT detected! Marking note for resolution: " + remoteNote.id);
                            noteDao.updateSyncStatus(remoteNote.id, SyncStatus.CONFLICT);
                        } else {
                            Log.d(TAG, "Remote change applied locally: " + remoteNote.id);
                            remoteNote.syncStatus = SyncStatus.SYNCED;
                            noteDao.insertOrUpdateNote(remoteNote);
                        }
                        break;
                    case REMOVED:
                        if (isConflict) {
                            noteDao.updateSyncStatus(remoteNote.id, SyncStatus.CONFLICT);
                        } else {
                            remoteNote.syncStatus = SyncStatus.SYNCED;
                            noteDao.insertOrUpdateNote(remoteNote);
                        }
                        break;
                }
            }
        });
    }

    public void stopFirestoreListener() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    public void insert(String title, String content, String uid) {
        io.execute(() -> {
            Note note = new Note(uid, title, content, deviceId);
            note.syncStatus = SyncStatus.SYNCING;
            noteDao.insertOrUpdateNote(note);
            syncManager.scheduleSync(note.id);
        });
    }

    // --- THIS METHOD IS NOW FIXED ---
    /**
     * Inserts a pre-constructed Note object (e.g., a new Canvas note).
     * @param note The Note to insert.
     */
    public void insert(Note note) {
        // We assume the note object is already complete
        // FIX: Changed 'executor' to 'io'
        io.execute(() -> {
            noteDao.insertOrUpdateNote(note);
            syncManager.scheduleSync(note.id);
        });
    }

    public void update(Note note) {
        io.execute(() -> {
            note.updatedAt = new Date();
            note.lastEditedByDeviceId = deviceId;
            note.syncStatus = SyncStatus.SYNCING;
            noteDao.insertOrUpdateNote(note);
            syncManager.scheduleSync(note.id);
        });
    }

    /**
     * ✅ DEPRECATED: This method is replaced by trash(Note note).
     * You can now safely remove this from your repository.
     */
    // public void delete(Note note) { ... }

    public void updateSyncStatus(String noteId, SyncStatus status) {
        io.execute(() -> noteDao.updateSyncStatus(noteId, status));
    }

    public void clearAll() {
        io.execute(() -> {
            stopFirestoreListener();
            syncManager.cancelAllSyncs();
            noteDao.nukeTable();
        });
    }
}