package com.example.softnotesandcanvas.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Manages the enqueuing of sync jobs using WorkManager.
 * This class abstracts the WorkManager implementation details
 * from the rest of the application (e.g., the Repository).
 */
public class SyncManager {

    private static final String SYNC_WORK_TAG = "sync_note";
    private static final String UNIQUE_WORK_PREFIX = "sync_";
    private final WorkManager workManager;

    public SyncManager(Context context) {
        this.workManager = WorkManager.getInstance(context.getApplicationContext());
    }

    /**
     * Schedules a one-time sync job for a specific note.
     *
     * @param noteId The ID of the note to sync.
     */
    public void scheduleSync(@NonNull String noteId) {
        // 1. Create constraints: Job only runs when network is connected.
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // 2. Create input data: Pass the noteId to the worker.
        Data inputData = new Data.Builder()
                .putString(SyncWorker.KEY_NOTE_ID, noteId)
                .build();

        // 3. Create the work request.
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(SYNC_WORK_TAG)
                // 4. Set exponential backoff for retries.
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                )
                .build();

        // 5. Enqueue as unique work.
        // If a sync job for this note is already pending, REPLACE it.
        // This ensures we only sync the *latest* version of the note.
        workManager.enqueueUniqueWork(
                UNIQUE_WORK_PREFIX + noteId,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
        );
    }

    /**
     * Cancels all pending sync jobs.
     * Used for sign-out.
     */
    public void cancelAllSyncs() {
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG);
    }
}
