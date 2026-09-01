package org.schabi.newpipe.speedcam;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Warns about the speed camera ahead while driving.
 *
 * <p>Runs as a foreground service because the warning has to keep working with the
 * screen off, which is how the phone sits in the car. Positions come from the
 * platform location service rather than a Google library, so the app stays free of
 * Google Play Services like the rest of NewPipe.
 */
public final class SpeedCameraService extends Service implements LocationListener {
    private static final String TAG = "SpeedCameraService";
    private static final String CHANNEL_ID = "speed_camera_alerts";
    private static final int NOTIFICATION_ID = 4231;

    /** Below this speed the driver is parked or crawling, and a warning helps nobody. */
    private static final float MIN_SPEED_MPS = 5.5f;
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final float UPDATE_DISTANCE_M = 10;

    /** Marks a speed trap at the current position, sent from the notification button. */
    public static final String ACTION_MARK = "org.schabi.newpipe.speedcam.MARK";

    private final SpeedCameraAlerts alerts = new SpeedCameraAlerts();
    /** Cameras already given the full sentence, so later stages can be a bare number. */
    private final Set<SpeedCamera> introduced = new HashSet<>();
    private List<SpeedCamera> cameras;
    private Location lastLocation;
    private LocationManager locationManager;
    private TextToSpeech speech;
    private boolean speechReady;
    private AudioFocusRequest focusRequest;

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cameras = new ArrayList<>(SpeedCameraStore.get(this));
        cameras.addAll(SpeedCameraMarks.all(this));
        speech = new TextToSpeech(this, status -> {
            speechReady = status == TextToSpeech.SUCCESS;
            if (speechReady) {
                speech.setLanguage(Locale.TAIWAN);
                speech.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
            }
        });
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());

        if (intent != null && ACTION_MARK.equals(intent.getAction())) {
            markHere();
            return START_STICKY;
        }

        alerts.reset();
        introduced.clear();
        startListening();
        return START_STICKY;
    }

    /**
     * Stores the current position as a speed trap the published data does not know about,
     * and says so out loud, because the driver cannot look at the screen to check.
     */
    private void markHere() {
        if (lastLocation == null) {
            speak(getString(R.string.speedcam_mark_no_position));
            return;
        }
        cameras.add(SpeedCameraMarks.add(this, lastLocation.getLatitude(),
                lastLocation.getLongitude()));
        speak(getString(R.string.speedcam_mark_saved));
    }

    private void startListening() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no location permission, stopping");
            stopSelf();
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, this);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "the device has no satellite positioning", e);
            stopSelf();
        }
    }

    @Override
    public void onLocationChanged(@NonNull final Location location) {
        lastLocation = location;

        if (!location.hasBearing() || location.getSpeed() < MIN_SPEED_MPS) {
            // without movement there is no direction, and a stationary phone needs no warning
            return;
        }

        // metres per second is what the phone reports; drivers think in kilometres per hour
        final float speedKmh = location.getSpeed() * 3.6f;

        final SpeedCameraAlerts.Alert alert = alerts.nextAlert(cameras, location.getLatitude(),
                location.getLongitude(), location.getBearing(), warningScale(), speedKmh);
        if (alert == null) {
            return;
        }

        if (alert.passed()) {
            speak(getString(R.string.speedcam_passed));
            return;
        }

        // the first call for a camera gets the whole sentence; the later ones are just the
        // number, because at motorway speed only a few seconds separate them
        final boolean firstCall = introduced.add(alert.camera());
        announce(alert, speedKmh, firstCall);
    }

    private float warningScale() {
        return SpeedCameraSettings.warningScale(this);
    }

    private void announce(@NonNull final SpeedCameraAlerts.Alert alert, final float speedKmh,
                          final boolean firstCall) {
        if (!speechReady) {
            return;
        }

        final int stageMeters = alert.stageMeters();
        final int limitKmh = alert.camera().limitKmh();
        final boolean speeding = SpeedCameraAlerts.isOverLimit(speedKmh, limitKmh);

        final String text;
        if (!firstCall) {
            // a bare number, so it still fits between two stages at motorway speed
            text = speeding
                    ? getString(R.string.speedcam_stage_over_limit, stageMeters)
                    : getString(R.string.speedcam_stage, stageMeters);
        } else if (limitKmh <= 0) {
            text = getString(R.string.speedcam_alert, stageMeters);
        } else if (alert.camera().deck() == SpeedCamera.Deck.ELEVATED) {
            // saying which deck it thinks we are on lets the driver catch it being wrong
            text = speeding
                    ? getString(R.string.speedcam_alert_elevated_over_limit,
                            stageMeters, limitKmh)
                    : getString(R.string.speedcam_alert_elevated, stageMeters, limitKmh);
        } else if (speeding) {
            // the same warning every time stops being heard, so the one that matters differs
            text = getString(R.string.speedcam_alert_over_limit, stageMeters, limitKmh);
        } else {
            text = getString(R.string.speedcam_alert_with_limit, stageMeters, limitKmh);
        }
        speak(text);
    }

    private void speak(@NonNull final String text) {
        if (!speechReady) {
            return;
        }
        if (MainActivity.DEBUG) {
            Log.d(TAG, "announcing: " + text);
        }
        duckMusic();
        speech.speak(text, TextToSpeech.QUEUE_ADD, null, "speedcam");
    }

    /**
     * Asks the system to lower whatever is playing for the length of the warning, so the
     * spoken distance is audible over the music instead of fighting it.
     */
    private void duckMusic() {
        final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        if (focusRequest == null) {
            focusRequest = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .build();
        }
        audio.requestAudioFocus(focusRequest);
        // the system restores the volume once focus is dropped again
        audio.abandonAudioFocusRequest(focusRequest);
    }

    private Notification buildNotification() {
        final NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.speedcam_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.speedcam_channel_description));
            manager.createNotificationChannel(channel);
        }

        final PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        final PendingIntent mark = PendingIntent.getService(this, 1,
                new Intent(this, SpeedCameraService.class).setAction(ACTION_MARK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_newpipe_triangle_white)
                .setContentTitle(getString(R.string.speedcam_running_title))
                .setContentText(getString(R.string.speedcam_running_text, cameras.size()))
                .setContentIntent(open)
                // a wide target on the lock screen, so a speed trap can be marked without
                // looking at the phone
                .addAction(R.drawable.ic_add, getString(R.string.speedcam_mark_action), mark)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (speech != null) {
            speech.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Starts warning about cameras, or stops it.
     *
     * @param context used to reach the service
     * @param running true to start driving mode, false to end it
     */
    public static void setRunning(@NonNull final Context context, final boolean running) {
        final Intent intent = new Intent(context, SpeedCameraService.class);
        if (running) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } else {
            context.stopService(intent);
        }
    }
}
