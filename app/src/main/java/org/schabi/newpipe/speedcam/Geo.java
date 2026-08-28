package org.schabi.newpipe.speedcam;

/**
 * The small amount of geometry the alert needs. Kept free of Android classes so the
 * behaviour that matters on the road can be unit tested on a computer.
 */
public final class Geo {
    private static final double EARTH_RADIUS_M = 6371000.0;

    private Geo() {
    }

    /**
     * Distance over the ground between two positions.
     *
     * @param lat1 latitude of the first position
     * @param lon1 longitude of the first position
     * @param lat2 latitude of the second position
     * @param lon2 longitude of the second position
     * @return metres
     */
    public static double distanceMeters(final double lat1, final double lon1,
                                        final double lat2, final double lon2) {
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLon = Math.toRadians(lon2 - lon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /**
     * The direction one has to travel to get from the first position to the second.
     *
     * @param lat1 latitude of the starting position
     * @param lon1 longitude of the starting position
     * @param lat2 latitude of the position being headed for
     * @param lon2 longitude of the position being headed for
     * @return degrees clockwise from north, 0 to 360
     */
    public static double bearingDegrees(final double lat1, final double lon1,
                                        final double lat2, final double lon2) {
        final double phi1 = Math.toRadians(lat1);
        final double phi2 = Math.toRadians(lat2);
        final double dLon = Math.toRadians(lon2 - lon1);
        final double y = Math.sin(dLon) * Math.cos(phi2);
        final double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);
        return normalize(Math.toDegrees(Math.atan2(y, x)));
    }

    /**
     * The smaller of the two ways round between two directions, so that north and
     * almost-north come out close together rather than a full turn apart.
     *
     * @param bearingA one direction, in degrees clockwise from north
     * @param bearingB the other direction, in degrees clockwise from north
     * @return degrees, 0 to 180
     */
    public static double angleBetween(final double bearingA, final double bearingB) {
        final double diff = Math.abs(normalize(bearingA) - normalize(bearingB));
        return diff > 180 ? 360 - diff : diff;
    }

    /**
     * Tells whether a position is anywhere Taiwan's cameras could be, counting the outlying
     * counties. A list holding anything outside this is not the list we asked for.
     *
     * @param latitude  the position to check
     * @param longitude the position to check
     * @return true when the position is within the country
     */
    public static boolean isInTaiwan(final double latitude, final double longitude) {
        return latitude >= 21.7 && latitude <= 26.5
                && longitude >= 118.0 && longitude <= 122.2;
    }

    /**
     * Brings any angle into the 0 to 360 range.
     *
     * @param degrees the angle to fold
     * @return the same direction expressed between 0 and 360
     */
    public static double normalize(final double degrees) {
        final double folded = degrees % 360;
        return folded < 0 ? folded + 360 : folded;
    }
}
