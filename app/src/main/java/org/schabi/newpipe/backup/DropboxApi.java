package org.schabi.newpipe.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.schabi.newpipe.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The four things the backup needs from Dropbox: upload, list, download and delete.
 *
 * <p>Called over plain HTTP rather than through the Dropbox library, because the library
 * would add a large dependency for four calls, and this app is deliberately light on them.
 * Access is limited to the app's own folder, so nothing else in the account is reachable.
 */
public final class DropboxApi {
    private static final String TOKEN_URL = "https://api.dropboxapi.com/oauth2/token";
    private static final String ACCOUNT_URL =
            "https://api.dropboxapi.com/2/users/get_current_account";
    private static final String LIST_URL = "https://api.dropboxapi.com/2/files/list_folder";
    private static final String DELETE_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String UPLOAD_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String DOWNLOAD_URL = "https://content.dropboxapi.com/2/files/download";

    private static final MediaType JSON = MediaType.get("application/json");
    private static final MediaType OCTET = MediaType.get("application/octet-stream");

    private final OkHttpClient client = new OkHttpClient();
    private final Context context;
    private String accessToken;

    /**
     * @param context used to read and store the account tokens
     */
    public DropboxApi(@NonNull final Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Redeems the code the driver pasted in, and remembers the account.
     *
     * @param code the authorisation code from the Dropbox page
     * @throws IOException when the network fails or Dropbox rejects the code
     */
    public void signIn(@NonNull final String code) throws IOException {
        final String verifier = DropboxAccount.pendingVerifier(context);
        if (verifier == null) {
            throw new IOException("no sign-in was started on this device");
        }

        final JSONObject tokens = postForm(TOKEN_URL, new FormBody.Builder()
                .add("code", code.trim())
                .add("grant_type", "authorization_code")
                .add("client_id", BuildConfig.DROPBOX_APP_KEY)
                .add("code_verifier", verifier)
                .build());

        final String refresh = tokens.optString("refresh_token", null);
        if (refresh == null) {
            throw new IOException("Dropbox did not return a lasting token");
        }
        accessToken = tokens.optString("access_token", null);
        DropboxAccount.completeSignIn(context, refresh, readAccountName());
    }

    /**
     * Reads the account name from Dropbox and stores it for the settings screen.
     *
     * @return the name, or null when it could not be read
     */
    @Nullable
    public String fetchAccountName() {
        final String name = readAccountName();
        DropboxAccount.setAccountName(context, name);
        return name;
    }

    @Nullable
    private String readAccountName() {
        try {
            final JSONObject account = postJson(ACCOUNT_URL, null);
            final JSONObject name = account.optJSONObject("name");
            return name == null ? account.optString("email", null)
                    : name.optString("display_name", null);
        } catch (final IOException e) {
            // the name is only shown in the settings; failing to read it must not undo sign-in
            return null;
        }
    }

    private String accessToken() throws IOException {
        if (accessToken != null) {
            return accessToken;
        }
        final String refresh = DropboxAccount.refreshToken(context);
        if (refresh == null) {
            throw new IOException("not signed in to Dropbox");
        }
        final JSONObject tokens = postForm(TOKEN_URL, new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .add("client_id", BuildConfig.DROPBOX_APP_KEY)
                .build());
        accessToken = tokens.optString("access_token", null);
        if (accessToken == null) {
            throw new IOException("Dropbox would not renew the sign-in");
        }
        return accessToken;
    }

    /**
     * Sends one backup file up.
     *
     * @param file the file to send
     * @param name the name it should carry in the folder
     * @throws IOException when the network fails or Dropbox refuses the file
     */
    public void upload(@NonNull final File file, @NonNull final String name) throws IOException {
        final JSONObject args = new JSONObject();
        try {
            args.put("path", "/" + name);
            args.put("mode", "overwrite");
            args.put("mute", true);
        } catch (final JSONException e) {
            throw new IOException(e);
        }

        final Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .header("Authorization", "Bearer " + accessToken())
                .header("Dropbox-API-Arg", args.toString())
                .post(RequestBody.create(file, OCTET))
                .build();
        try (Response response = client.newCall(request).execute()) {
            requireSuccess(response);
        }
    }

    /**
     * Lists what is already in the folder.
     *
     * @return the file names, in whatever order Dropbox gives them
     * @throws IOException when the network fails
     */
    @NonNull
    public List<String> list() throws IOException {
        final JSONObject args = new JSONObject();
        try {
            args.put("path", "");
        } catch (final JSONException e) {
            throw new IOException(e);
        }

        final List<String> names = new ArrayList<>();
        final JSONObject listing = postJson(LIST_URL, args);
        final JSONArray entries = listing.optJSONArray("entries");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                final JSONObject entry = entries.optJSONObject(i);
                if (entry != null) {
                    names.add(entry.optString("name"));
                }
            }
        }
        return names;
    }

    /**
     * Fetches one backup file.
     *
     * @param name  the file to fetch
     * @param about where to write it
     * @throws IOException when the network fails or the file is gone
     */
    public void download(@NonNull final String name, @NonNull final File about)
            throws IOException {
        final JSONObject args = new JSONObject();
        try {
            args.put("path", "/" + name);
        } catch (final JSONException e) {
            throw new IOException(e);
        }

        final Request request = new Request.Builder()
                .url(DOWNLOAD_URL)
                .header("Authorization", "Bearer " + accessToken())
                .header("Dropbox-API-Arg", args.toString())
                .post(RequestBody.create(new byte[0], null))
                .build();
        try (Response response = client.newCall(request).execute()) {
            requireSuccess(response);
            final ResponseBody body = response.body();
            try (InputStream in = body.byteStream();
                 OutputStream out = Files.newOutputStream(about.toPath())) {
                final byte[] buffer = new byte[8192];
                int read = in.read(buffer);
                while (read > 0) {
                    out.write(buffer, 0, read);
                    read = in.read(buffer);
                }
            }
        }
    }

    /**
     * Removes one backup file.
     *
     * @param name the file to remove
     * @throws IOException when the network fails
     */
    public void delete(@NonNull final String name) throws IOException {
        final JSONObject args = new JSONObject();
        try {
            args.put("path", "/" + name);
        } catch (final JSONException e) {
            throw new IOException(e);
        }
        postJson(DELETE_URL, args);
    }

    private JSONObject postForm(final String url, final FormBody body) throws IOException {
        final Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = client.newCall(request).execute()) {
            return readJson(response);
        }
    }

    private JSONObject postJson(final String url, @Nullable final JSONObject args)
            throws IOException {
        final RequestBody body = args == null
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(args.toString(), JSON);
        final Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken())
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return readJson(response);
        }
    }

    private JSONObject readJson(final Response response) throws IOException {
        requireSuccess(response);
        try {
            return new JSONObject(response.body().string());
        } catch (final JSONException e) {
            throw new IOException("Dropbox sent something unreadable", e);
        }
    }

    private void requireSuccess(final Response response) throws IOException {
        if (!response.isSuccessful()) {
            final ResponseBody body = response.body();
            final String detail = body == null ? "" : body.string();
            throw new IOException("Dropbox said " + response.code() + " " + detail);
        }
    }
}
