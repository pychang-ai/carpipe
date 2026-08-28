package org.schabi.newpipe.settings;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import org.schabi.newpipe.R;
import org.schabi.newpipe.speedcam.SpeedCameraDataWorker;
import org.schabi.newpipe.speedcam.SpeedCameraService;
import org.schabi.newpipe.speedcam.SpeedCameraUpdater;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final ExecutorService UPDATER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

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
                    SpeedCameraDataWorker.setEnabled(requireContext(), wanted);
                    return true;
                });

        showDataAge();
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
        if (getString(R.string.speedcam_update_data_key).equals(preference.getKey())) {
            updateCameraList();
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

        final List<String> wanted = new ArrayList<>();
        wanted.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // without this the car stereo connection never reaches us, so driving mode
            // would have to be switched on by hand every trip
            wanted.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        ActivityCompat.requestPermissions(requireActivity(),
                wanted.toArray(new String[0]), LOCATION_REQUEST);
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

    /**
     * Fetches a newer camera list now, rather than waiting for the monthly check.
     */
    private void updateCameraList() {
        final Context context = requireContext().getApplicationContext();
        Toast.makeText(context, R.string.speedcam_update_working, Toast.LENGTH_SHORT).show();

        UPDATER.execute(() -> {
            try {
                final int count = SpeedCameraUpdater.update(context);
                MAIN.post(() -> {
                    Toast.makeText(context, getString(R.string.speedcam_update_done, count),
                            Toast.LENGTH_LONG).show();
                    showDataAge();
                });
            } catch (final Exception e) {
                final String reason = e.getMessage() == null
                        ? e.getClass().getSimpleName() : e.getMessage();
                MAIN.post(() -> Toast.makeText(context,
                        getString(R.string.speedcam_update_failed, reason),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Shows whether the list came from a download or is the one the app was built with, so a
     * check that has quietly stopped working is visible rather than assumed to be fine.
     */
    private void showDataAge() {
        final long updatedAt = SpeedCameraUpdater.updatedAt(requireContext());
        final String when = updatedAt == 0
                ? getString(R.string.speedcam_update_data_shipped)
                : DateFormat.getDateInstance().format(new Date(updatedAt));
        requirePreference(R.string.speedcam_update_data_key)
                .setSummary(getString(R.string.speedcam_update_data_summary, when));
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
