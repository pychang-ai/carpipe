package org.schabi.newpipe.speedcam;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The places the driver marked, for the cameras the published data cannot cover:
 * mobile speed traps and roadside checks, which move and are never in an open data set.
 *
 * <p>Marks are kept in a plain text file so they survive an app update and can be read
 * or edited outside the app, and so they can be carried over in a backup.
 */
public final class SpeedCameraMarks {
    private static final String TAG = "SpeedCameraMarks";
    private static final String FILE = "marked_cameras.csv";
    /** A marked spot is watched in every direction, since nobody notes a bearing while driving. */
    private static final int[] EVERY_DIRECTION = new int[0];

    private SpeedCameraMarks() {
    }

    /**
     * Records a place the driver marked.
     *
     * @param context   used to reach the app's own storage
     * @param latitude  where the car was
     * @param longitude where the car was
     * @return the mark that was stored
     */
    @NonNull
    public static SpeedCamera add(@NonNull final Context context,
                                  final double latitude, final double longitude) {
        try (FileWriter writer = new FileWriter(file(context), true)) {
            writer.write(String.format(Locale.US, "%.6f,%.6f%n", latitude, longitude));
        } catch (final IOException e) {
            Log.e(TAG, "could not store the mark", e);
        }
        return new SpeedCamera(latitude, longitude, 0,
                SpeedCamera.Deck.UNKNOWN, EVERY_DIRECTION);
    }

    /**
     * Reads back everything the driver has marked.
     *
     * @param context used to reach the app's own storage
     * @return the marks, empty when nothing was marked yet
     */
    @NonNull
    public static List<SpeedCamera> all(@NonNull final Context context) {
        final List<SpeedCamera> marks = new ArrayList<>();
        final File file = file(context);
        if (!file.exists()) {
            return marks;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                final String[] parts = line.split(",");
                if (parts.length == 2) {
                    try {
                        marks.add(new SpeedCamera(Double.parseDouble(parts[0]),
                                Double.parseDouble(parts[1]), 0,
                                SpeedCamera.Deck.UNKNOWN, EVERY_DIRECTION));
                    } catch (final NumberFormatException ignored) {
                        // a damaged line must not cost the driver the rest of their marks
                    }
                }
                line = reader.readLine();
            }
        } catch (final IOException e) {
            Log.e(TAG, "could not read the marks", e);
        }
        return marks;
    }

    /**
     * Gives the file the marks live in, so a backup can pick it up.
     *
     * @param context used to reach the app's own storage
     * @return the file, which may not exist yet
     */
    @NonNull
    public static File file(@NonNull final Context context) {
        return new File(context.getFilesDir(), FILE);
    }
}
