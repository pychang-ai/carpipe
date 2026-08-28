package org.schabi.newpipe.settings;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.schabi.newpipe.R;
import org.schabi.newpipe.backup.BackupManager;
import org.schabi.newpipe.backup.BackupNames;
import org.schabi.newpipe.backup.DailyBackupWorker;
import org.schabi.newpipe.backup.DropboxAccount;
import org.schabi.newpipe.backup.DropboxApi;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wires the Dropbox backup entries on the backup and restore screen.
 *
 * <p>Sign-in goes through the browser and comes back as a code the driver pastes in. That
 * avoids registering a return address with Dropbox, which is one more thing to get wrong for
 * a one-off setup, and it works the same on every phone.
 */
final class DropboxPreferences {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DropboxPreferences() {
    }

    /**
     * Attaches the behaviour to the preferences already inflated by the fragment.
     *
     * @param fragment the backup and restore screen
     */
    static void attach(@NonNull final PreferenceFragmentCompat fragment) {
        final Context context = fragment.requireContext();

        final Preference link = fragment.findPreference(context.getString(
                R.string.dropbox_link_key));
        final Preference backupNow = fragment.findPreference(context.getString(
                R.string.dropbox_backup_now_key));
        final Preference restore = fragment.findPreference(context.getString(
                R.string.dropbox_restore_key));
        final SwitchPreferenceCompat auto = fragment.findPreference(context.getString(
                R.string.dropbox_auto_key));
        if (link == null || backupNow == null || restore == null || auto == null) {
            return;
        }

        refresh(fragment, link, backupNow, auto, restore);

        // the approval comes back through the Dropbox app, which brings this screen forward
        // again rather than calling us, so the result is picked up on the way back in
        fragment.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_RESUME
                    && !DropboxAccount.isLinked(context)
                    && DropboxAccount.collectSignIn(context)) {
                DailyBackupWorker.setEnabled(context, auto.isChecked());
                WORKER.execute(() -> {
                    new DropboxApi(context).fetchAccountName();
                    MAIN.post(() -> refresh(fragment, link, backupNow, auto, restore));
                });
                refresh(fragment, link, backupNow, auto, restore);
            }
        });

        link.setOnPreferenceClickListener(p -> {
            if (!DropboxAccount.isUsable()) {
                toast(fragment, context.getString(R.string.dropbox_not_configured));
            } else if (DropboxAccount.isLinked(context)) {
                DropboxAccount.unlink(context);
                DailyBackupWorker.setEnabled(context, false);
                refresh(fragment, link, backupNow, auto, restore);
            } else {
                DropboxAccount.startSignIn(context);
            }
            return true;
        });

        auto.setOnPreferenceChangeListener((p, value) -> {
            DailyBackupWorker.setEnabled(context, Boolean.TRUE.equals(value));
            return true;
        });

        backupNow.setOnPreferenceClickListener(p -> {
            toast(fragment, context.getString(R.string.dropbox_working));
            WORKER.execute(() -> {
                try {
                    BackupManager.backUpNow(context, LocalDateTime.now());
                    MAIN.post(() -> {
                        toast(fragment, context.getString(R.string.dropbox_backup_done));
                        refresh(fragment, link, backupNow, auto, restore);
                    });
                } catch (final Exception e) {
                    reportFailure(fragment, e);
                }
            });
            return true;
        });

        restore.setOnPreferenceClickListener(p -> {
            toast(fragment, context.getString(R.string.dropbox_working));
            WORKER.execute(() -> {
                try {
                    final List<String> backups = BackupManager.listBackups(context);
                    MAIN.post(() -> offerRestore(fragment, backups));
                } catch (final Exception e) {
                    reportFailure(fragment, e);
                }
            });
            return true;
        });
    }

    private static void offerRestore(@NonNull final PreferenceFragmentCompat fragment,
                                     @NonNull final List<String> backups) {
        final Context context = fragment.requireContext();
        if (backups.isEmpty()) {
            toast(fragment, context.getString(R.string.dropbox_no_backups));
            return;
        }

        final String[] labels = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++) {
            final LocalDateTime taken = BackupNames.takenAt(backups.get(i));
            labels[i] = taken == null ? backups.get(i) : taken.toString().replace('T', ' ');
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.dropbox_restore_title)
                .setItems(labels, (d, which) -> confirmRestore(fragment, backups.get(which)))
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private static void confirmRestore(@NonNull final PreferenceFragmentCompat fragment,
                                       @NonNull final String name) {
        final Context context = fragment.requireContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.dropbox_restore_title)
                .setMessage(R.string.dropbox_restore_confirm)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.ok, (d, w) -> {
                    toast(fragment, context.getString(R.string.dropbox_working));
                    WORKER.execute(() -> {
                        try {
                            BackupManager.restore(context, name);
                            MAIN.post(() -> toast(fragment,
                                    context.getString(R.string.dropbox_restore_done)));
                        } catch (final Exception e) {
                            reportFailure(fragment, e);
                        }
                    });
                })
                .show();
    }

    private static void refresh(@NonNull final PreferenceFragmentCompat fragment,
                                final Preference link, final Preference backupNow,
                                final SwitchPreferenceCompat auto, final Preference restore) {
        final Context context = fragment.requireContext();
        final boolean linked = DropboxAccount.isLinked(context);

        if (linked) {
            final String who = DropboxAccount.accountName(context);
            link.setTitle(R.string.dropbox_unlink_title);
            link.setSummary(who == null
                    ? context.getString(R.string.dropbox_unlink_summary)
                    : context.getString(R.string.dropbox_linked_summary, who));
        } else {
            link.setTitle(R.string.dropbox_link_title);
            link.setSummary(R.string.dropbox_link_summary);
        }

        auto.setEnabled(linked);
        backupNow.setEnabled(linked);
        restore.setEnabled(linked);

        final long last = DropboxAccount.lastBackupAt(context);
        backupNow.setSummary(context.getString(R.string.dropbox_backup_now_summary,
                last == 0 ? context.getString(R.string.dropbox_never_backed_up)
                        : DateFormat.getDateTimeInstance().format(new Date(last))));
    }

    private static void reportFailure(@NonNull final PreferenceFragmentCompat fragment,
                                      @NonNull final Exception failure) {
        // never fail quietly: a backup nobody knows is broken is worse than no backup at all
        final String reason = failure.getMessage() == null
                ? failure.getClass().getSimpleName() : failure.getMessage();
        MAIN.post(() -> toast(fragment, fragment.requireContext()
                .getString(R.string.dropbox_failed, reason)));
    }

    private static void toast(@NonNull final PreferenceFragmentCompat fragment,
                              @Nullable final String message) {
        if (fragment.getContext() != null) {
            Toast.makeText(fragment.getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
