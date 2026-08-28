package org.schabi.newpipe.speedcam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides which camera to announce, and refuses to say the same one twice.
 *
 * <p>Kept free of Android classes so the rules that matter on the road can be
 * checked on a computer: only cameras ahead are announced, cameras on the other
 * carriageway are ignored, and standing in traffic beside one does not turn the
 * warning into a loop.
 */
public final class SpeedCameraAlerts {
    /** How far off the driving direction a camera may sit and still count as ahead. */
    private static final int AHEAD_TOLERANCE_DEG = 50;
    /** How far a camera's listed direction may differ from ours and still be our side. */
    private static final int SAME_ROAD_TOLERANCE_DEG = 60;
    /** Once announced, a camera is only allowed to speak again after leaving it behind. */
    private static final double FORGET_DISTANCE_M = 2000;

    private final Set<SpeedCamera> announced = new HashSet<>();

    /**
     * How far above the limit still counts as not speeding. A car's own speedometer reads a
     * few kilometres high by design, so without this margin the warning would scold the
     * driver while the dashboard shows a legal speed, and be ignored soon after.
     */
    public static final int SPEED_MARGIN_KMH = 5;

    /**
     * Tells whether the car is going fast enough for the warning to say so.
     *
     * @param speedKmh the speed the phone measured
     * @param limitKmh the limit the camera enforces, 0 when unknown
     * @return true when the driver should be told to slow down
     */
    public static boolean isOverLimit(final float speedKmh, final int limitKmh) {
        return limitKmh > 0 && speedKmh > limitKmh + SPEED_MARGIN_KMH;
    }

    /**
     * How early to speak, chosen from the limit the camera enforces: a warning that is
     * right for a motorway comes far too early in town, and one right for town comes
     * too late to do anything about at speed.
     *
     * @param limitKmh the speed the camera enforces, 0 when unknown
     * @return metres before the camera at which the warning should be given
     */
    public static int warningDistanceMeters(final int limitKmh) {
        if (limitKmh >= 90) {
            return 600;
        }
        if (limitKmh >= 70) {
            return 450;
        }
        if (limitKmh >= 50) {
            return 300;
        }
        return 200;
    }

    /**
     * Picks the camera to announce for the current position, if any.
     *
     * @param cameras   every known camera
     * @param latitude  where the car is
     * @param longitude where the car is
     * @param heading   the direction the car is travelling, degrees clockwise from north
     * @param scale     multiplies the warning distance, from the user's setting
     * @return the camera to announce, or null when there is nothing to say
     */
    @Nullable
    public SpeedCamera nextAlert(@NonNull final List<SpeedCamera> cameras,
                                 final double latitude, final double longitude,
                                 final float heading, final float scale) {
        SpeedCamera closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (final SpeedCamera camera : cameras) {
            final double distance = Geo.distanceMeters(latitude, longitude,
                    camera.latitude(), camera.longitude());

            if (announced.contains(camera)) {
                if (distance > FORGET_DISTANCE_M) {
                    announced.remove(camera);
                }
                continue;
            }

            if (distance > warningDistanceMeters(camera.limitKmh()) * scale) {
                continue;
            }
            if (!camera.watches(heading, SAME_ROAD_TOLERANCE_DEG)) {
                continue;
            }

            final double toCamera = Geo.bearingDegrees(latitude, longitude,
                    camera.latitude(), camera.longitude());
            if (Geo.angleBetween(heading, toCamera) > AHEAD_TOLERANCE_DEG) {
                // behind us, or off to the side on another road
                continue;
            }

            if (distance < closestDistance) {
                closest = camera;
                closestDistance = distance;
            }
        }

        if (closest != null) {
            announced.add(closest);
        }
        return closest;
    }

    /**
     * Forgets what has been announced, for the start of a new trip.
     */
    public void reset() {
        announced.clear();
    }
}
