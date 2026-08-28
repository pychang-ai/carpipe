package org.schabi.newpipe.speedcam;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.schabi.newpipe.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
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
            cameras = Collections.unmodifiableList(read(context));
        }
        return cameras;
    }

    @NonNull
    private static List<SpeedCamera> read(@NonNull final Context context) {
        final List<SpeedCamera> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(ASSET), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line = reader.readLine();
            while (line != null) {
                final SpeedCamera camera = parseLine(line);
                if (camera != null) {
                    loaded.add(camera);
                }
                line = reader.readLine();
            }
        } catch (final IOException e) {
            // an alert that cannot load must not take the music down with it
            Log.e(TAG, "could not read the speed camera list", e);
        }

        if (MainActivity.DEBUG) {
            Log.d(TAG, "loaded " + loaded.size() + " speed cameras");
        }
        return loaded;
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
