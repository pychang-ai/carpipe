package org.schabi.newpipe.backup;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Takes the daily backup.
 *
 * <p>It waits for the phone to be charging on an unmetered network, so the backup never eats
 * the driver's data or battery. If the conditions do not come round, the work simply waits;
 * the settings screen shows when the last one succeeded so a long gap is visible.
 */
public class DailyBackupWorker extends Worker {
    private static final String TAG = "DailyBackupWorker";
    private static final String WORK_NAME = "dropbox_daily_backup";

    /**
     * @param context used by the worker framework
     * @param params  supplied by the worker framework
     */
    public DailyBackupWorker(@NonNull final Context context,
                             @NonNull final WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!DropboxAccount.isLinked(getApplicationContext())) {
            return Result.success();
        }
        try {
            BackupManager.backUpNow(getApplicationContext(), LocalDateTime.now());
            return Result.success();
        } catch (final Exception e) {
            Log.w(TAG, "the daily backup did not go through", e);
            // a failure here is usually the network being away, which is worth another try
            return Result.retry();
        }
    }

    /**
     * Starts or stops the daily backup.
     *
     * @param context used to reach the worker framework
     * @param wanted  true to back up daily, false to stop
     */
    public static void setEnabled(@NonNull final Context context, final boolean wanted) {
        final WorkManager work = WorkManager.getInstance(context);
        if (!wanted) {
            work.cancelUniqueWork(WORK_NAME);
            return;
        }

        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build();

        work.enqueueUniquePeriodicWork(WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(DailyBackupWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build());
    }
}
