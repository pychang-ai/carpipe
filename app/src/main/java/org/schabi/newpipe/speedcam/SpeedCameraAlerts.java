package org.schabi.newpipe.speedcam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides what to say about the camera ahead, and when.
 *
 * <p>A camera is called out as the car closes on it, at five hundred, three hundred, two
 * hundred and one hundred metres, and once more when it has been left behind. Kept free of
 * Android classes so the rules that matter on the road can be checked on a computer: only
 * cameras ahead are announced, the other carriageway is left alone, and standing in traffic
 * beside one does not turn the warning into a loop.
 */
public final class SpeedCameraAlerts {
    /** Where the warnings fall, in metres before the camera, furthest first. */
    public static final int[] STAGES = {500, 300, 200, 100};

    /** How far off the driving direction a camera may sit and still count as ahead. */
    private static final int AHEAD_TOLERANCE_DEG = 50;
    /** How far a camera's listed direction may differ from ours and still be our side. */
    private static final int SAME_ROAD_TOLERANCE_DEG = 60;
    /** Past this angle the camera is behind the car, which is what "passed" means. */
    private static final int BEHIND_DEG = 100;
    /** Once left behind, a camera is only allowed to speak again on a later trip past it. */
    private static final double FORGET_DISTANCE_M = 2000;
    /** Closer than this and two cameras are, for a phone, standing in the same place. */
    private static final double SAME_PLACE_M = 80;
    /** Below this the two limits are too alike for the car's speed to tell them apart. */
    private static final int TELLING_LIMIT_GAP_KMH = 15;
    /**
     * How far above the limit still counts as not speeding. A car's own speedometer reads a
     * few kilometres high by design, so without this margin the warning would scold the
     * driver while the dashboard shows a legal speed, and be ignored soon after.
     */
    public static final int SPEED_MARGIN_KMH = 5;

    /**
     * One thing to say.
     *
     * @param camera      which camera it is about
     * @param stageMeters the stage being called out, or 0 for the camera having been passed
     * @param passed      true when the camera is now behind the car
     */
    public record Alert(@NonNull SpeedCamera camera, int stageMeters, boolean passed) {
    }

    /** The nearest stage already called out for each camera, so none is repeated. */
    private final Map<SpeedCamera, Integer> calledAt = new HashMap<>();
    private final Set<SpeedCamera> reportedPassed = new HashSet<>();

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
     * The stage a given distance falls in: the nearest listed stage still ahead of the car.
     *
     * @param distanceMeters how far the camera is
     * @param scale          multiplies the stages, from the user's setting
     * @return the stage in metres, or 0 when the camera is further away than any stage
     */
    static int stageFor(final double distanceMeters, final float scale) {
        int stage = 0;
        for (final int candidate : STAGES) {
            if (distanceMeters <= candidate * scale) {
                stage = candidate;
            }
        }
        return stage;
    }

    /**
     * Works out what should be said for the current position, if anything.
     *
     * @param cameras   every known camera
     * @param latitude  where the car is
     * @param longitude where the car is
     * @param heading   the direction the car is travelling, degrees clockwise from north
     * @param scale     multiplies the warning distances, from the user's setting
     * @return what to say, or null when there is nothing to say
     */
    @Nullable
    public Alert nextAlert(@NonNull final List<SpeedCamera> cameras,
                           final double latitude, final double longitude,
                           final float heading, final float scale) {
        return nextAlert(cameras, latitude, longitude, heading, scale, 0f);
    }

    /**
     * Works out what should be said for the current position, if anything, using the speed of
     * the car to choose between cameras standing at the same place on different decks.
     *
     * @param cameras   every known camera
     * @param latitude  where the car is
     * @param longitude where the car is
     * @param heading   the direction the car is travelling, degrees clockwise from north
     * @param scale     multiplies the warning distances, from the user's setting
     * @param speedKmh  how fast the car is going, 0 when it is not known
     * @return what to say, or null when there is nothing to say
     */
    @Nullable
    public Alert nextAlert(@NonNull final List<SpeedCamera> cameras,
                           final double latitude, final double longitude,
                           final float heading, final float scale, final float speedKmh) {
        Alert closest = null;
        double closestDistance = Double.MAX_VALUE;
        final List<Alert> candidates = new ArrayList<>();

        for (final SpeedCamera camera : cameras) {
            final double distance = Geo.distanceMeters(latitude, longitude,
                    camera.latitude(), camera.longitude());

            if (distance > FORGET_DISTANCE_M) {
                // far enough behind that a later trip past it should speak again
                calledAt.remove(camera);
                reportedPassed.remove(camera);
                continue;
            }

            final Alert alert = alertFor(camera, distance, latitude, longitude, heading, scale);
            if (alert == null) {
                continue;
            }
            candidates.add(alert);
            if (distance < closestDistance) {
                closest = alert;
                closestDistance = distance;
            }
        }

        if (closest != null && !closest.passed()) {
            closest = onOurDeck(closest, candidates, speedKmh);
        }

        if (closest != null) {
            if (closest.passed()) {
                reportedPassed.add(closest.camera());
            } else {
                calledAt.put(closest.camera(), closest.stageMeters());
            }
        }
        return closest;
    }

    /**
     * Chooses between cameras standing at practically the same place.
     *
     * <p>An elevated road and the street beneath it share a position, and a phone cannot tell
     * which one the car is on. What it can tell is how fast the car is going, and the two
     * roads enforce very different limits: at seventy-five the car is on the elevated road,
     * not on a street limited to fifty. Where the limits are close, or the speed is unknown,
     * the nearest camera is kept and nothing is guessed.
     *
     * @param nearest    the closest camera that would be announced
     * @param candidates every camera that would be announced at this moment
     * @param speedKmh   how fast the car is going, 0 when it is not known
     * @return the camera to announce
     */
    @NonNull
    private Alert onOurDeck(@NonNull final Alert nearest, @NonNull final List<Alert> candidates,
                            final float speedKmh) {
        if (speedKmh <= 0) {
            return nearest;
        }

        Alert best = nearest;
        double bestGap = Math.abs(speedKmh - nearest.camera().limitKmh());

        for (final Alert other : candidates) {
            if (other == nearest || other.passed()) {
                continue;
            }
            final double apart = Geo.distanceMeters(
                    nearest.camera().latitude(), nearest.camera().longitude(),
                    other.camera().latitude(), other.camera().longitude());
            final int limitGap = Math.abs(
                    other.camera().limitKmh() - nearest.camera().limitKmh());
            if (apart > SAME_PLACE_M || limitGap < TELLING_LIMIT_GAP_KMH) {
                continue;
            }

            final double gap = Math.abs(speedKmh - other.camera().limitKmh());
            if (gap < bestGap) {
                best = other;
                bestGap = gap;
            }
        }
        return best;
    }

    @Nullable
    private Alert alertFor(@NonNull final SpeedCamera camera, final double distance,
                           final double latitude, final double longitude,
                           final float heading, final float scale) {
        final double toCamera = Geo.bearingDegrees(latitude, longitude,
                camera.latitude(), camera.longitude());
        final double offCourse = Geo.angleBetween(heading, toCamera);

        if (offCourse >= BEHIND_DEG) {
            // only cameras we actually warned about are worth confirming as passed
            final boolean worthSaying = calledAt.containsKey(camera)
                    && !reportedPassed.contains(camera);
            return worthSaying ? new Alert(camera, 0, true) : null;
        }

        if (offCourse > AHEAD_TOLERANCE_DEG
                || !camera.watches(heading, SAME_ROAD_TOLERANCE_DEG)) {
            return null;
        }

        final int stage = stageFor(distance, scale);
        if (stage == 0) {
            return null;
        }
        // the first call is whichever stage the car is already inside, so joining a road
        // close to a camera still gets one warning rather than silence until the next stage
        final int alreadyCalled = calledAt.getOrDefault(camera, Integer.MAX_VALUE);
        return stage < alreadyCalled ? new Alert(camera, stage, false) : null;
    }

    /**
     * Forgets what has been announced, for the start of a new trip.
     */
    public void reset() {
        calledAt.clear();
        reportedPassed.clear();
    }
}
