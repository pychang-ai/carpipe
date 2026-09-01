package org.schabi.newpipe.speedcam;

import androidx.annotation.NonNull;

/**
 * One speed camera from the police data set.
 *
 * @param latitude  where it stands
 * @param longitude where it stands
 * @param limitKmh  the speed it enforces, 0 when the source did not say
 * @param deck      which deck of the road it watches, when the address said so
 * @param headings  the travel directions it watches, in degrees clockwise from north;
 *                  an empty array means it watches every direction, which is also what
 *                  an unreadable direction falls back to
 */
public record SpeedCamera(double latitude, double longitude, int limitKmh,
                          @NonNull Deck deck, @NonNull int[] headings) {

    /**
     * Which deck of the road a camera watches. A phone knows where the car is but not which
     * deck it is on, so this only narrows the choice; it never decides it alone.
     */
    public enum Deck {
        /** The address said nothing, which is true of most of them. */
        UNKNOWN,
        /** On an elevated road. */
        ELEVATED,
        /** On the surface street running beneath an elevated road. */
        UNDER,
        /** Inside a tunnel. */
        TUNNEL,
        /** On a ramp between the two. */
        RAMP;

        /**
         * Reads the tag written into the shipped file.
         *
         * @param text the tag, which may be empty
         * @return the deck it names, UNKNOWN when it names none
         */
        @NonNull
        public static Deck of(final String text) {
            if (text == null) {
                return UNKNOWN;
            }
            return switch (text.trim()) {
                case "elevated" -> ELEVATED;
                case "under" -> UNDER;
                case "tunnel" -> TUNNEL;
                case "ramp" -> RAMP;
                default -> UNKNOWN;
            };
        }
    }

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
