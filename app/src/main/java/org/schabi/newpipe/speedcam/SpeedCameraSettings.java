package org.schabi.newpipe.speedcam;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;

/**
 * Reads the driving mode settings. The parsing is kept free of Android classes so the
 * fallbacks can be unit tested: a broken setting must leave the warning working at its
 * normal distance rather than silently switching it off.
 */
public final class SpeedCameraSettings {
    private static final float NORMAL_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;

    private SpeedCameraSettings() {
    }

    /**
     * Tells whether the driver asked for camera warnings at all.
     *
     * @param context used to read the settings
     * @return true when warnings are switched on
     */
    public static boolean isEnabled(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.speedcam_enabled_key), false);
    }

    /**
     * How much earlier or later than normal to warn, as the driver chose.
     *
     * @param context used to read the settings
     * @return the multiplier to apply to the warning distance
     */
    public static float warningScale(@NonNull final Context context) {
        return parseScale(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.speedcam_distance_key), null));
    }

    /**
     * Turns a stored distance setting into a usable multiplier.
     *
     * @param value the stored setting, may be null
     * @return the multiplier, 1.0 when the value cannot be used
     */
    static float parseScale(@Nullable final String value) {
        if (value == null) {
            return NORMAL_SCALE;
        }
        final float scale;
        try {
            scale = Float.parseFloat(value);
        } catch (final NumberFormatException e) {
            return NORMAL_SCALE;
        }
        if (scale < MIN_SCALE || scale > MAX_SCALE) {
            return NORMAL_SCALE;
        }
        return scale;
    }
}
