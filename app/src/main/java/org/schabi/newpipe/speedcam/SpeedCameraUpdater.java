package org.schabi.newpipe.speedcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches a newer camera list.
 *
 * <p>The police publish updates at no fixed interval, so the list shipped inside the app goes
 * stale. The file is fetched from the project's own repository rather than straight from the
 * government site, because the checking script runs there first: a published file with a
 * coordinate in the sea or a missing column never reaches a phone.
 *
 * <p>A download only replaces the list once it has passed the same checks again here. If it
 * fails, the previous list stays in use, which is the whole point: an app that quietly warns
 * about nothing would be worse than one that warns about slightly old cameras.
 */
public final class SpeedCameraUpdater {
    private static final String TAG = "SpeedCameraUpdater";
    private static final String SOURCE =
            "https://raw.githubusercontent.com/pychang-ai/carpipe/dev/"
                    + "app/src/main/assets/speed_cameras.csv";
    static final String FILE = "speed_cameras.csv";
    private static final String KEY_UPDATED_AT = "speedcam_data_updated_at";

    /** The shipped list holds around 1900 cameras; far fewer means the file is not it. */
    static final int MIN_CAMERAS = 1000;

    private SpeedCameraUpdater() {
    }

    /**
     * The downloaded list, if one has been stored.
     *
     * @param context used to reach the app's own storage
     * @return the file, which may not exist
     */
    @NonNull
    public static File file(@NonNull final Context context) {
        return new File(context.getFilesDir(), FILE);
    }

    /**
     * When the list was last replaced by a download.
     *
     * @param context used to read the stored time
     * @return milliseconds since the epoch, or 0 when the shipped list is still in use
     */
    public static long updatedAt(@NonNull final Context context) {
        return prefs(context).getLong(KEY_UPDATED_AT, 0);
    }

    /**
     * Fetches the list and keeps it if it is usable.
     *
     * @param context used to store the file
     * @return the number of cameras now in use
     * @throws IOException when the download fails or what came back is not a usable list
     */
    public static int update(@NonNull final Context context) throws IOException {
        final File candidate = new File(context.getCacheDir(), "speed_cameras_new.csv");
        download(candidate);

        final List<SpeedCamera> cameras = SpeedCameraStore.read(candidate);
        if (!isUsable(cameras)) {
            Files.deleteIfExists(candidate.toPath());
            throw new IOException("the downloaded list did not look like a camera list");
        }

        Files.move(candidate.toPath(), file(context).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        prefs(context).edit().putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply();
        SpeedCameraStore.forget();

        Log.i(TAG, "camera list replaced, now holding " + cameras.size());
        return cameras.size();
    }

    /**
     * Decides whether a freshly read list is fit to warn a driver with.
     *
     * @param cameras what was read from the downloaded file
     * @return true when the list can replace the one in use
     */
    static boolean isUsable(@NonNull final List<SpeedCamera> cameras) {
        if (cameras.size() < MIN_CAMERAS) {
            return false;
        }
        for (final SpeedCamera camera : cameras) {
            if (!Geo.isInTaiwan(camera.latitude(), camera.longitude())) {
                return false;
            }
        }
        return true;
    }

    private static void download(@NonNull final File into) throws IOException {
        final Request request = new Request.Builder().url(SOURCE).build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("the camera list could not be fetched, "
                        + response.code());
            }
            final ResponseBody body = response.body();
            try (InputStream in = body.byteStream();
                 OutputStream out = Files.newOutputStream(into.toPath())) {
                in.transferTo(out);
            }
        }
    }

    private static SharedPreferences prefs(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
