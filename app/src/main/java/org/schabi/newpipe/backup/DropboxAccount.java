package org.schabi.newpipe.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.BuildConfig;

/**
 * Remembers whether Dropbox has been linked, and holds the tokens it gave us.
 *
 * <p>Sign-in happens once. Dropbox returns a long-lived refresh token, which is exchanged for
 * a short-lived access token whenever a backup runs, so the driver is never asked to sign in
 * again on the road.
 */
public final class DropboxAccount {
    private static final String KEY_REFRESH = "dropbox_refresh_token";
    private static final String KEY_ACCOUNT = "dropbox_account_name";
    private static final String KEY_VERIFIER = "dropbox_pending_verifier";
    private static final String KEY_LAST_BACKUP = "dropbox_last_backup";

    private static final String AUTHORIZE = "https://www.dropbox.com/oauth2/authorize";

    private DropboxAccount() {
    }

    private static SharedPreferences prefs(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Tells whether this build was given a Dropbox application key at all.
     *
     * @return false when the key is missing, which means backup cannot be offered
     */
    public static boolean isConfigured() {
        return !BuildConfig.DROPBOX_APP_KEY.isEmpty();
    }

    /**
     * Tells whether the driver has signed in.
     *
     * @param context used to read the stored tokens
     * @return true when a refresh token is held
     */
    public static boolean isLinked(@NonNull final Context context) {
        return prefs(context).getString(KEY_REFRESH, null) != null;
    }

    /**
     * The Dropbox account the backups go to, for showing in the settings.
     *
     * @param context used to read the stored name
     * @return the account name, or null when not signed in
     */
    @Nullable
    public static String accountName(@NonNull final Context context) {
        return prefs(context).getString(KEY_ACCOUNT, null);
    }

    /**
     * Builds the address the browser is sent to, and remembers the matching secret.
     *
     * @param context used to store the secret until the code comes back
     * @return the sign-in address
     */
    @NonNull
    public static String startSignIn(@NonNull final Context context) {
        final String verifier = Pkce.newVerifier();
        prefs(context).edit().putString(KEY_VERIFIER, verifier).apply();

        return Uri.parse(AUTHORIZE).buildUpon()
                .appendQueryParameter("client_id", BuildConfig.DROPBOX_APP_KEY)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("token_access_type", "offline")
                .appendQueryParameter("code_challenge", Pkce.challengeOf(verifier))
                .appendQueryParameter("code_challenge_method", "S256")
                .build().toString();
    }

    /**
     * The secret kept from {@link #startSignIn(Context)}, needed to redeem the code.
     *
     * @param context used to read it back
     * @return the verifier, or null when no sign-in is in progress
     */
    @Nullable
    static String pendingVerifier(@NonNull final Context context) {
        return prefs(context).getString(KEY_VERIFIER, null);
    }

    /**
     * Stores what Dropbox returned once the code has been redeemed.
     *
     * @param context      used to store it
     * @param refreshToken the long-lived token
     * @param accountName  the account the backups will go to
     */
    static void completeSignIn(@NonNull final Context context,
                               @NonNull final String refreshToken,
                               @Nullable final String accountName) {
        prefs(context).edit()
                .putString(KEY_REFRESH, refreshToken)
                .putString(KEY_ACCOUNT, accountName)
                .remove(KEY_VERIFIER)
                .apply();
    }

    /**
     * The long-lived token used to obtain access tokens.
     *
     * @param context used to read it
     * @return the token, or null when not signed in
     */
    @Nullable
    static String refreshToken(@NonNull final Context context) {
        return prefs(context).getString(KEY_REFRESH, null);
    }

    /**
     * Forgets the account. The backups already in Dropbox are left untouched, since they are
     * the driver's own files and deleting them here would be a nasty surprise.
     *
     * @param context used to clear the stored tokens
     */
    public static void unlink(@NonNull final Context context) {
        prefs(context).edit()
                .remove(KEY_REFRESH)
                .remove(KEY_ACCOUNT)
                .remove(KEY_VERIFIER)
                .apply();
    }

    /**
     * When the last backup succeeded, so the settings can show it and warn when it goes stale.
     *
     * @param context used to read it
     * @return milliseconds since the epoch, or 0 when no backup has ever succeeded
     */
    public static long lastBackupAt(@NonNull final Context context) {
        return prefs(context).getLong(KEY_LAST_BACKUP, 0);
    }

    static void recordBackup(@NonNull final Context context, final long whenMillis) {
        prefs(context).edit().putLong(KEY_LAST_BACKUP, whenMillis).apply();
    }
}
