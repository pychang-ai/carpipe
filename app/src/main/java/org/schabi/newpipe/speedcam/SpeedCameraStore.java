package org.schabi.newpipe.speedcam;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.schabi.newpipe.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the speed cameras shipped with the app. The file is small enough to keep in
 * memory, so it is read once when the drive starts and then answered from memory,
 * which keeps the check off the storage while driving.
 *
 * <p>Source and licence are recorded in {@code data/speedcam/README.md}.
 */
public final class SpeedCameraStore {
    private static final String TAG = "SpeedCameraStore";
    private static final String ASSET = "speed_cameras.csv";

    private static List<SpeedCamera> cameras;

    private SpeedCameraStore() {
    }

    /**
     * Gives every known camera, reading the file the first time it is asked.
     *
     * @param context used to reach the packaged file
     * @return the cameras, empty when the file could not be read
     */
    @NonNull
    public static synchronized List<SpeedCamera> get(@NonNull final Context context) {
        if (cameras == null) {
            cameras = Collections.unmodifiableList(load(context));
        }
        return cameras;
    }

    /**
     * Drops what is held in memory, so the next request reads the file again. Called after a
     * newer list has been downloaded.
     */
    public static synchronized void forget() {
        cameras = null;
    }

    /**
     * Prefers a downloaded list over the one shipped with the app, and falls back to the
     * shipped one whenever the download turns out to be unreadable.
     *
     * @param context used to reach the packaged file and the app's own storage
     * @return the cameras to warn about
     */
    @NonNull
    private static List<SpeedCamera> load(@NonNull final Context context) {
        final File downloaded = SpeedCameraUpdater.file(context);
        if (downloaded.exists()) {
            final List<SpeedCamera> updated = read(downloaded);
            if (SpeedCameraUpdater.isUsable(updated)) {
                logCount(updated.size(), "downloaded");
                return updated;
            }
            Log.w(TAG, "the downloaded camera list is unusable, keeping the shipped one");
        }

        final List<SpeedCamera> shipped = readAsset(context);
        logCount(shipped.size(), "shipped");
        return shipped;
    }

    /**
     * Reads a camera list from a file.
     *
     * @param file the file to read
     * @return the cameras it holds, empty when it cannot be read
     */
    @NonNull
    static List<SpeedCamera> read(@NonNull final File file) {
        try (InputStream stream = new FileInputStream(file)) {
            return parse(stream);
        } catch (final IOException e) {
            Log.e(TAG, "could not read " + file, e);
            return new ArrayList<>();
        }
    }

    @NonNull
    private static List<SpeedCamera> readAsset(@NonNull final Context context) {
        try (InputStream stream = context.getAssets().open(ASSET)) {
            return parse(stream);
        } catch (final IOException e) {
            // an alert that cannot load must not take the music down with it
            Log.e(TAG, "could not read the shipped speed camera list", e);
            return new ArrayList<>();
        }
    }

    @NonNull
    private static List<SpeedCamera> parse(@NonNull final InputStream stream)
            throws IOException {
        final List<SpeedCamera> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line = reader.readLine();
            while (line != null) {
                final SpeedCamera camera = parseLine(line);
                if (camera != null) {
                    loaded.add(camera);
                }
                line = reader.readLine();
            }
        }
        return loaded;
    }

    private static void logCount(final int count, final String source) {
        if (MainActivity.DEBUG) {
            Log.d(TAG, "loaded " + count + " speed cameras from the " + source + " list");
        }
    }

    private static SpeedCamera parseLine(@NonNull final String line) {
        final String[] parts = line.split(",", 4);
        if (parts.length < 4) {
            return null;
        }
        try {
            return new SpeedCamera(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Integer.parseInt(parts[2]),
                    CameraDirections.parse(parts[3]));
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
