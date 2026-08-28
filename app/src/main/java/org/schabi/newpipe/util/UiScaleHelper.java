package org.schabi.newpipe.util;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;

/**
 * Scales the whole user interface by the factor chosen in the appearance settings.
 * Text and controls grow together, so the app stays both readable and tappable in a car.
 */
public final class UiScaleHelper {
    private static final float NO_SCALING = 1.0f;
    private static final float MAX_SCALING = 2.0f;

    private UiScaleHelper() {
    }

    /**
     * Wraps a context so that everything inflated from it uses the configured interface scale.
     * Call it from {@code attachBaseContext} before the context is used.
     *
     * @param base the context to wrap
     * @return the scaled context, or {@code base} itself when no scaling is configured
     */
    @NonNull
    public static Context wrapContext(@NonNull final Context base) {
        final float scale = getScale(base);
        if (scale == NO_SCALING) {
            return base;
        }

        final Configuration configuration =
                new Configuration(base.getResources().getConfiguration());
        configuration.densityDpi = Math.round(configuration.densityDpi * scale);
        // the screen holds fewer density independent pixels once they got bigger, so keep the
        // reported sizes in sync or resource qualifiers would pick layouts for a larger screen
        configuration.screenWidthDp = Math.round(configuration.screenWidthDp / scale);
        configuration.screenHeightDp = Math.round(configuration.screenHeightDp / scale);
        configuration.smallestScreenWidthDp =
                Math.round(configuration.smallestScreenWidthDp / scale);

        return base.createConfigurationContext(configuration);
    }

    private static float getScale(@NonNull final Context context) {
        return parseScale(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.ui_scale_key), null));
    }

    /**
     * Turns a stored setting into a usable scale factor. Anything missing, malformed or outside
     * the offered range falls back to no scaling, because a zero or negative factor would leave
     * the interface unusable. Kept free of Android dependencies so it can be unit tested.
     *
     * @param value the stored setting, may be null
     * @return the scale factor to apply, 1.0 when the value cannot be used
     */
    static float parseScale(@Nullable final String value) {
        if (value == null) {
            return NO_SCALING;
        }
        final float scale;
        try {
            scale = Float.parseFloat(value);
        } catch (final NumberFormatException e) {
            return NO_SCALING;
        }
        if (scale < NO_SCALING || scale > MAX_SCALING) {
            return NO_SCALING;
        }
        return scale;
    }
}
