package org.schabi.newpipe.speedcam;

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

import java.util.concurrent.TimeUnit;

/**
 * Keeps the camera list current.
 *
 * <p>The police add and remove cameras at no fixed interval, so a monthly look is enough; the
 * file is around sixty kilobytes, and it is only fetched on an unmetered network. A failed
 * attempt costs nothing, because the list already on the phone stays in use.
 */
public class SpeedCameraDataWorker extends Worker {
    private static final String TAG = "SpeedCameraDataWorker";
    private static final String WORK_NAME = "speedcam_data_update";
    private static final int EVERY_DAYS = 30;

    /**
     * @param context used by the worker framework
     * @param params  supplied by the worker framework
     */
    public SpeedCameraDataWorker(@NonNull final Context context,
                                 @NonNull final WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SpeedCameraUpdater.update(getApplicationContext());
            return Result.success();
        } catch (final Exception e) {
            Log.w(TAG, "the camera list was not updated this time", e);
            return Result.retry();
        }
    }

    /**
     * Starts or stops the monthly check.
     *
     * @param context used to reach the worker framework
     * @param wanted  true while camera warnings are switched on
     */
    public static void setEnabled(@NonNull final Context context, final boolean wanted) {
        final WorkManager work = WorkManager.getInstance(context);
        if (!wanted) {
            work.cancelUniqueWork(WORK_NAME);
            return;
        }

        work.enqueueUniquePeriodicWork(WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(
                        SpeedCameraDataWorker.class, EVERY_DAYS, TimeUnit.DAYS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.UNMETERED)
                                .build())
                        .build());
    }
}
