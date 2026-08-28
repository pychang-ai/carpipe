package org.schabi.newpipe.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Names for the backup files kept in the cloud folder.
 *
 * <p>The date lives in the file name rather than only in the cloud's own timestamp, so the
 * list stays readable when opened in Dropbox itself and still sorts oldest to newest.
 */
public final class BackupNames {
    static final String PREFIX = "CAI-PP-backup-";
    static final String SUFFIX = ".zip";
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.US);

    private BackupNames() {
    }

    /**
     * Builds the name a backup taken at the given moment should carry.
     *
     * @param when when the backup was taken
     * @return the file name, without any folder
     */
    @NonNull
    public static String of(@NonNull final LocalDateTime when) {
        return PREFIX + STAMP.format(when) + SUFFIX;
    }

    /**
     * Reads back when a backup was taken, from its name.
     *
     * @param name a file name from the cloud folder
     * @return the moment it was taken, or null when the name is not one of ours
     */
    @Nullable
    public static LocalDateTime takenAt(@Nullable final String name) {
        if (name == null || !name.startsWith(PREFIX) || !name.endsWith(SUFFIX)) {
            return null;
        }
        final String stamp = name.substring(PREFIX.length(), name.length() - SUFFIX.length());
        try {
            return LocalDateTime.parse(stamp, STAMP);
        } catch (final DateTimeParseException e) {
            return null;
        }
    }
}
