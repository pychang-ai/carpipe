package org.schabi.newpipe.speedcam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the police data set's direction column, which is free text with more than a
 * hundred spellings. Around one in seven entries names a landmark rather than a
 * compass direction, for example "往大溪方向"; those cannot be turned into an angle,
 * so they are treated as watching every direction.
 *
 * <p>That fallback is deliberate: missing a camera costs a ticket, while an extra
 * spoken warning costs a second of attention. The two are not worth trading evenly.
 * For the same reason a compass character inside a place name, as in "往台北市" or
 * "往南崁方向", must not be read as a direction, so only whole words count.
 */
public final class CameraDirections {
    private static final int[] EVERY_DIRECTION = new int[0];

    private static final int NORTH = 0;
    private static final int NORTHEAST = 45;
    private static final int EAST = 90;
    private static final int SOUTHEAST = 135;
    private static final int SOUTH = 180;
    private static final int SOUTHWEST = 225;
    private static final int WEST = 270;
    private static final int NORTHWEST = 315;

    private static final String COMPASS = "(東北|東南|西北|西南|北|南|東|西)";
    /** A direction word on its own, once the trailing filler has been taken off. */
    private static final Pattern ALONE = Pattern.compile("^" + COMPASS + "(上|下)?$");
    /** "A向B" and "A往B" mean traffic running from A to B, so B is the travel direction. */
    private static final Pattern FROM_TO =
            Pattern.compile("^" + COMPASS + "(向|往)" + COMPASS + "$");
    /** Words the data set appends that say nothing about which way traffic runs. */
    private static final String[] FILLER = {"方向", "車道", "向", "側", "邊"};

    private CameraDirections() {
    }

    /**
     * Turns one direction cell into the travel directions the camera watches.
     *
     * @param text the cell as published, may be null
     * @return the watched directions in degrees clockwise from north, empty when the
     *         camera watches everything or the text could not be read
     */
    @NonNull
    public static int[] parse(@Nullable final String text) {
        if (text == null) {
            return EVERY_DIRECTION;
        }
        // notes in brackets say what is enforced, not which way, so they only get in the way
        final String cleaned = text.replaceAll("[（(].*?[)）]", "").replaceAll("\\s+", "").trim();
        if (cleaned.isEmpty()) {
            return EVERY_DIRECTION;
        }

        if (cleaned.contains("南北") || cleaned.contains("北南")) {
            return new int[]{NORTH, SOUTH};
        }
        if (cleaned.contains("東西") || cleaned.contains("西東")) {
            return new int[]{EAST, WEST};
        }

        final Matcher fromTo = FROM_TO.matcher(withoutFiller(cleaned));
        if (fromTo.matches()) {
            return new int[]{degreesOf(fromTo.group(3))};
        }

        final String lead = cleaned.startsWith("往") ? cleaned.substring(1) : cleaned;
        final Matcher alone = ALONE.matcher(withoutFiller(lead));
        if (alone.matches()) {
            return new int[]{travelDirection(alone.group(1), alone.group(2))};
        }

        return EVERY_DIRECTION;
    }

    /**
     * Takes off the trailing words that carry no direction, so that "南下車道" and
     * "北上方向" reduce to the part that does.
     *
     * @param text a cleaned direction cell
     * @return the same text without its trailing filler
     */
    private static String withoutFiller(final String text) {
        String result = text;
        boolean trimmed = true;
        while (trimmed) {
            trimmed = false;
            for (final String filler : FILLER) {
                if (result.length() > filler.length() && result.endsWith(filler)) {
                    result = result.substring(0, result.length() - filler.length());
                    trimmed = true;
                }
            }
        }
        return result;
    }

    /**
     * Works out which way traffic runs for a lone direction word. "南下" is the
     * southbound carriageway and "北上" the northbound one, while "南向" or a bare
     * "南" already names the direction of travel.
     *
     * @param word   the compass word
     * @param ending the word that followed it, may be null
     * @return the travel direction in degrees clockwise from north
     */
    private static int travelDirection(final String word, @Nullable final String ending) {
        if ("上".equals(ending) || "下".equals(ending)) {
            // only the north-south carriageway wording uses these
            return "南".equals(word) ? SOUTH : NORTH;
        }
        return degreesOf(word);
    }

    private static int degreesOf(final String word) {
        return switch (word) {
            case "北" -> NORTH;
            case "東北" -> NORTHEAST;
            case "東" -> EAST;
            case "東南" -> SOUTHEAST;
            case "南" -> SOUTH;
            case "西南" -> SOUTHWEST;
            case "西" -> WEST;
            default -> NORTHWEST;
        };
    }
}
