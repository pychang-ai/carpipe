package org.schabi.newpipe.settings;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import org.schabi.newpipe.R;
import org.schabi.newpipe.speedcam.SpeedCameraService;

import java.util.Locale;

/**
 * Settings for the spoken speed camera warning.
 *
 * <p>Switching it on has to ask for the location permission, because without it the
 * warning would sit there looking enabled and never say anything.
 */
public class SpeedCamSettingsFragment extends BasePreferenceFragment {
    private static final int LOCATION_REQUEST = 6231;
    /** The distance and limit spoken by the try-it-now button, chosen to sound typical. */
    private static final int DEMO_DISTANCE_M = 500;
    private static final int DEMO_LIMIT_KMH = 60;

    private TextToSpeech demoSpeech;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();

        final String enabledKey = getString(R.string.speedcam_enabled_key);
        requirePreference(R.string.speedcam_enabled_key)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    final boolean wanted = Boolean.TRUE.equals(newValue);
                    defaultPreferences.edit().putBoolean(enabledKey, wanted).apply();

                    if (wanted && !hasLocationPermission()) {
                        askForLocation();
                        return true;
                    }
                    SpeedCameraService.setRunning(requireContext(), wanted);
                    return true;
                });
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        if (getString(R.string.speedcam_test_key).equals(preference.getKey())) {
            playDemoWarning();
            return true;
        }
        if (getString(R.string.speedcam_battery_key).equals(preference.getKey())) {
            openBatterySettings();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void askForLocation() {
        Toast.makeText(requireContext(), R.string.speedcam_needs_location, Toast.LENGTH_LONG)
                .show();
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_REQUEST) {
            return;
        }
        final boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            SpeedCameraService.setRunning(requireContext(), true);
        } else {
            // leaving the switch on would promise a warning the app cannot give
            defaultPreferences.edit()
                    .putBoolean(getString(R.string.speedcam_enabled_key), false).apply();
            setPreferenceScreen(null);
            addPreferencesFromResourceRegistry();
        }
    }

    /**
     * Opens the system list where an app can be exempted from power saving. Phone makers put
     * this in different places, so the general battery screen is the fallback that always works.
     */
    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (final ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().getPackageName(), null)));
            } catch (final ActivityNotFoundException ignored) {
                Toast.makeText(requireContext(), R.string.general_error, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void playDemoWarning() {
        final String text = getString(R.string.speedcam_alert_with_limit,
                DEMO_DISTANCE_M, DEMO_LIMIT_KMH);
        demoSpeech = new TextToSpeech(requireContext().getApplicationContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                return;
            }
            demoSpeech.setLanguage(Locale.TAIWAN);
            demoSpeech.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            demoSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speedcam-demo");
        });
    }

    @Override
    public void onDestroy() {
        if (demoSpeech != null) {
            demoSpeech.shutdown();
            demoSpeech = null;
        }
        super.onDestroy();
    }
}
