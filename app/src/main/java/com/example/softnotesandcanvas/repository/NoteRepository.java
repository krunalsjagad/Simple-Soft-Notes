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
 * This is the refactored repository, acting as the Single Source of Truth.
 * It manages both the local Room database (via NoteDao) and the remote
 * FirebaseFirestore, implementing the "offline-first" architecture.
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

    private ListenerRegistration firestoreListener;

    public NoteRepository(Application app) {
        AppDatabase db = AppDatabase.getInstance(app);
        noteDao = db.noteDao();
        io = Executors.newSingleThreadExecutor();
        firestore = FirebaseFirestore.getInstance();
        syncManager = new SyncManager(app.getApplicationContext());
        deviceId = DeviceUtil.getDeviceId(app.getApplicationContext());
    }

    public LiveData<List<Note>> getNotesForUser(String uid) {
        return noteDao.getAllActiveNotesForUser(uid);
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

                // Ignore changes that originated from this device
                if (deviceId.equals(remoteNote.lastEditedByDeviceId)) {
                    Log.d(TAG, "Ignoring echo of our own change for note: " + remoteNote.id);
                    continue;
                }

                // ✅ --- CONFLICT DETECTION LOGIC ---
                // Before writing, check the status of the local note.
                Note localNote = noteDao.getNoteById(remoteNote.id);
                boolean isConflict = false;

                if (localNote != null && localNote.syncStatus != SyncStatus.SYNCED) {
                    // The local note has pending changes (SYNCING, OFFLINE, or CONFLICT).
                    // We must check timestamps to see who wins.
                    if (remoteNote.updatedAt != null && remoteNote.updatedAt.after(localNote.updatedAt)) {
                        // The remote note is *newer* than our local, un-synced note.
                        // This is a CONFLICT. We must preserve the local changes.
                        isConflict = true;
                    } else {
                        // The local note is newer or same. Let the local version win.
                        // The local version will be synced up by the SyncWorker eventually.
                        // So, we *ignore* this incoming remote change.
                        Log.w(TAG, "Conflict detected, but local is newer. Ignoring remote change for: " + remoteNote.id);
                        continue;
                    }
                }
                // --- END CONFLICT DETECTION ---

                switch (dc.getType()) {
                    case ADDED:
                    case MODIFIED:
                        if (isConflict) {
                            // ✅ Mark the note as CONFLICT, but do *not* overwrite
                            // the user's local content.
                            Log.w(TAG, "CONFLICT detected! Marking note for resolution: " + remoteNote.id);
                            noteDao.updateSyncStatus(remoteNote.id, SyncStatus.CONFLICT);
                        } else {
                            // No conflict. It's safe to accept the server version.
                            Log.d(TAG, "Remote change applied locally: " + remoteNote.id);
                            remoteNote.syncStatus = SyncStatus.SYNCED;
                            noteDao.insertOrUpdateNote(remoteNote);
                        }
                        break;
                    case REMOVED:
                        // This case handles a document being deleted on remote.
                        // With our soft-delete logic, this should just be a
                        // MODIFIED event (isDeleted=true).
                        // We will apply the same conflict logic.
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

    public void update(Note note) {
        io.execute(() -> {
            note.updatedAt = new Date();
            note.lastEditedByDeviceId = deviceId;
            note.syncStatus = SyncStatus.SYNCING;
            noteDao.insertOrUpdateNote(note);
            syncManager.scheduleSync(note.id);
        });
    }

    public void delete(Note note) {
        io.execute(() -> {
            noteDao.softDeleteNote(note.id, new Date(), deviceId);
            syncManager.scheduleSync(note.id);
        });
    }

    public void updateSyncStatus(String noteId, SyncStatus status) {
        io.execute(() -> noteDao.updateSyncStatus(noteId, status));
    }

    public void clearAll() {
        io.execute(() -> {
            stopFirestoreListener(); // Stop listening before wiping
            syncManager.cancelAllSyncs(); // Cancel pending jobs
            noteDao.nukeTable(); // Wipe local data
        });
    }
}

