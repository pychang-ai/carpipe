package org.schabi.newpipe.speedcam;

import androidx.annotation.NonNull;

/**
 * One speed camera from the police data set.
 *
 * @param latitude  where it stands
 * @param longitude where it stands
 * @param limitKmh  the speed it enforces, 0 when the source did not say
 * @param headings  the travel directions it watches, in degrees clockwise from north;
 *                  an empty array means it watches every direction, which is also what
 *                  an unreadable direction falls back to
 */
public record SpeedCamera(double latitude, double longitude, int limitKmh,
                          @NonNull int[] headings) {

    /**
     * Tells whether a car travelling on the given heading is one this camera watches.
     *
     * @param heading      the direction the car is travelling, degrees clockwise from north
     * @param toleranceDeg how far off the listed direction still counts as the same road
     * @return true when the camera watches this direction, or watches every direction
     */
    public boolean watches(final float heading, final int toleranceDeg) {
        if (headings.length == 0) {
            return true;
        }
        for (final int watched : headings) {
            if (Geo.angleBetween(heading, watched) <= toleranceDeg) {
                return true;
            }
        }
        return false;
    }
}
