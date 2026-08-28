package org.schabi.newpipe.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.settings.export.BackupFileLocator;
import org.schabi.newpipe.settings.export.ImportExportManager;
import org.schabi.newpipe.speedcam.SpeedCameraMarks;
import org.schabi.newpipe.streams.io.StoredFileHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Puts a backup together, sends it to Dropbox, and brings one back.
 *
 * <p>The backup is the app's own export — subscriptions, playlists, history and settings —
 * with the driver's marked speed traps added, since those are the one thing the published
 * data cannot replace.
 *
 * <p>Every method here talks to the network or the database, so none of them may be called
 * on the main thread.
 */
public final class BackupManager {
    private static final String ZIP_MIME = "application/zip";
    private static final String MARKS_ENTRY = "marked_cameras.csv";

    private BackupManager() {
    }

    /**
     * Takes a backup and sends it to Dropbox, then removes the copies that have aged out.
     *
     * @param context used to reach the database, the settings and the account
     * @param taken   the moment the backup is being taken, which becomes its name
     * @throws IOException when the backup cannot be written or sent
     */
    public static void backUpNow(@NonNull final Context context,
                                 @NonNull final LocalDateTime taken) throws IOException {
        final File file = writeBackupFile(context);
        try {
            final DropboxApi dropbox = new DropboxApi(context);
            dropbox.upload(file, BackupNames.of(taken));
            DropboxAccount.recordBackup(context, System.currentTimeMillis());

            for (final String expired : BackupRetention.expired(dropbox.list())) {
                dropbox.delete(expired);
            }
        } finally {
            // the copy in the cache is of no use once it is up, and it is not small
            Files.deleteIfExists(file.toPath());
        }
    }

    /**
     * Lists the backups held in Dropbox, newest first.
     *
     * @param context used to reach the account
     * @return the file names
     * @throws IOException when the network fails
     */
    @NonNull
    public static List<String> listBackups(@NonNull final Context context) throws IOException {
        final List<String> ours = new ArrayList<>();
        for (final String name : new DropboxApi(context).list()) {
            if (BackupNames.takenAt(name) != null) {
                ours.add(name);
            }
        }
        ours.sort(Comparator.comparing(
                (String name) -> (LocalDateTime) BackupNames.takenAt(name)).reversed());
        return ours;
    }

    /**
     * Fetches one backup and puts its contents back in place, replacing what is there now.
     *
     * @param context used to reach the database and the settings
     * @param name    which backup to bring back
     * @throws IOException when the file cannot be fetched or read
     */
    public static void restore(@NonNull final Context context, @NonNull final String name)
            throws IOException {
        final File downloaded = new File(context.getCacheDir(), "restore.zip");
        new DropboxApi(context).download(name, downloaded);

        try {
            final StoredFileHelper file = new StoredFileHelper(
                    context, null, Uri.fromFile(downloaded), ZIP_MIME);
            final ImportExportManager manager =
                    new ImportExportManager(new BackupFileLocator(context));
            final SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(context);

            manager.ensureDbDirectoryExists();
            if (!manager.extractDb(file)) {
                throw new IOException("the backup held no database");
            }
            try {
                if (manager.exportHasJsonPrefs(file)) {
                    manager.loadJsonPrefs(file, preferences);
                } else if (manager.exportHasSerializedPrefs(file)) {
                    manager.loadSerializedPrefs(file, preferences);
                }
            } catch (final Exception e) {
                // the database is already back; losing only the settings is worth reporting
                // but not worth undoing the rest of the restore
                throw new IOException("the settings in the backup could not be read", e);
            }

            restoreMarks(context, downloaded);
        } finally {
            Files.deleteIfExists(downloaded.toPath());
        }
    }

    /**
     * Writes the backup into the cache folder, ready to be sent.
     *
     * @param context used to reach the database and the settings
     * @return the file written
     * @throws IOException when it cannot be written
     */
    @NonNull
    static File writeBackupFile(@NonNull final Context context) throws IOException {
        NewPipeDatabase.checkpoint();

        final File exported = new File(context.getCacheDir(), "backup-export.zip");
        Files.deleteIfExists(exported.toPath());

        final StoredFileHelper target = new StoredFileHelper(
                context, null, Uri.fromFile(exported), ZIP_MIME);
        try {
            new ImportExportManager(new BackupFileLocator(context))
                    .exportDatabase(PreferenceManager.getDefaultSharedPreferences(context),
                            target);
        } catch (final Exception e) {
            throw new IOException("the backup could not be written", e);
        }

        return withMarks(context, exported);
    }

    /**
     * Copies the export into a new archive with the marked speed traps added. A zip cannot be
     * appended to in place, so it is rewritten once.
     *
     * @param context used to find the marks file
     * @param source  the export as the app wrote it
     * @return the archive to send
     * @throws IOException when either file cannot be read or written
     */
    private static File withMarks(@NonNull final Context context, @NonNull final File source)
            throws IOException {
        final File marks = SpeedCameraMarks.file(context);
        if (!marks.exists()) {
            return source;
        }

        final File combined = new File(context.getCacheDir(), "backup.zip");
        Files.deleteIfExists(combined.toPath());

        try (ZipFile in = new ZipFile(source);
             ZipOutputStream out = new ZipOutputStream(
                     Files.newOutputStream(combined.toPath()))) {
            final var entries = in.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                out.putNextEntry(new ZipEntry(entry.getName()));
                try (InputStream stream = in.getInputStream(entry)) {
                    stream.transferTo(out);
                }
                out.closeEntry();
            }

            out.putNextEntry(new ZipEntry(MARKS_ENTRY));
            Files.copy(marks.toPath(), out);
            out.closeEntry();
        }

        Files.deleteIfExists(source.toPath());
        return combined;
    }

    private static void restoreMarks(@NonNull final Context context, @NonNull final File archive)
            throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            final ZipEntry entry = zip.getEntry(MARKS_ENTRY);
            if (entry == null) {
                return;
            }
            try (InputStream stream = zip.getInputStream(entry);
                 OutputStream out = Files.newOutputStream(
                         SpeedCameraMarks.file(context).toPath())) {
                stream.transferTo(out);
            }
        }
    }
}
