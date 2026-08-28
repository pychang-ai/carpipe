package org.schabi.newpipe.backup;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Decides which old backups to delete.
 *
 * <p>Keeping only one is the trap: if the data is already damaged when that backup runs, the
 * good copy is gone. A week of copies gives room to notice and go back. The newest is never
 * deleted, whatever else the list contains.
 */
public final class BackupRetention {
    static final int KEEP = 7;

    private BackupRetention() {
    }

    /**
     * Picks the backups that may be deleted once a new one has been stored.
     *
     * @param names every file name currently in the cloud folder
     * @return the names to delete, oldest first; empty when nothing should go
     */
    @NonNull
    public static List<String> expired(@NonNull final List<String> names) {
        final List<String> ours = new ArrayList<>();
        for (final String name : names) {
            if (BackupNames.takenAt(name) != null) {
                ours.add(name);
            }
        }

        // newest first, so everything past the keep count is the older end of the list
        ours.sort(Comparator.comparing(
                (String name) -> (LocalDateTime) BackupNames.takenAt(name)).reversed());

        final List<String> expired = new ArrayList<>();
        for (int i = KEEP; i < ours.size(); i++) {
            expired.add(ours.get(i));
        }
        return expired;
    }
}
