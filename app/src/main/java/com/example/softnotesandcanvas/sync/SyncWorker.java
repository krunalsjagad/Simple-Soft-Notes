package com.example.softnotesandcanvas.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.softnotesandcanvas.db.AppDatabase;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.db.NoteDao;
import com.example.softnotesandcanvas.db.SyncStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * A WorkManager Worker responsible for syncing a single Note
 * to Firestore based on its ID.
 */
public class SyncWorker extends Worker {

    public static final String KEY_NOTE_ID = "KEY_NOTE_ID";
    private static final String TAG = "SyncWorker";

    private final NoteDao noteDao;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // Get the singleton instance of the database and DAO
        noteDao = AppDatabase.getInstance(context.getApplicationContext()).noteDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        // 1. Get the note ID from the input data
        String noteId = getInputData().getString(KEY_NOTE_ID);
        if (noteId == null) {
            Log.e(TAG, "No noteId provided. Failing job.");
            return Result.failure();
        }

        Log.d(TAG, "Starting sync for note: " + noteId);

        // 2. Fetch the note from the local Room database
        // This is a synchronous call, which is allowed inside doWork()
        Note note = noteDao.getNoteById(noteId);

        if (note == null) {
            Log.w(TAG, "Note " + noteId + " not found in local DB. Assuming deleted or invalid. Success.");
            // If the note doesn't exist, we can't sync it.
            // Mark as success to avoid retrying a job that will never work.
            return Result.success();
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("notes").document(note.id);
            Task<?> firestoreTask;

            // 3. Decide whether to delete or set (create/update) the document
            if (note.isDeleted) {
                Log.d(TAG, "Note " + noteId + " is soft-deleted. Deleting from Firestore.");
                firestoreTask = docRef.delete();
            } else {
                Log.d(TAG, "Note " + noteId + " is active. Setting in Firestore.");
                // The note object from Room already has all fields
                // (including lastEditedByDeviceId) set by the Repository.
                firestoreTask = docRef.set(note);
            }

            // 4. Block this background thread until the Firebase task completes
            // Tasks.await() is a synchronous call.
            Tasks.await(firestoreTask);

            // 5. On success, update the note's local status to SYNCED
            // We only do this if the task was successful.
            Log.d(TAG, "Successfully synced note: " + noteId);
            if (note.isDeleted) {
                // If the note was deleted and synced, we can remove it from local DB
                // This is optional, but good for cleanup.
                // For now, we'll just mark it as synced.
                // noteDao.deleteNoteById(noteId);
            } else {
                noteDao.updateSyncStatus(noteId, SyncStatus.SYNCED);
            }
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Sync failed for note: " + noteId, e);

            // Check if this is a transient error (like network)
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e.getCause()).getCode();
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                    Log.w(TAG, "Transient error. Retrying sync for " + noteId);
                    return Result.retry();
                }
            }

            // For other errors (like PERMISSION_DENIED), don't retry.
            // We can also flag the note as CONFLICT or OFFLINE here.
            noteDao.updateSyncStatus(noteId, SyncStatus.OFFLINE);
            return Result.failure();
        }
    }
}
