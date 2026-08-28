package org.schabi.newpipe.backup;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * The proof-key part of the sign-in.
 *
 * <p>It lets the app prove that the browser round trip and the token request came from the
 * same place, without holding a secret. That matters here because the source code is public:
 * there is no secret to leak, since there is none.
 */
public final class Pkce {
    private static final int VERIFIER_BYTES = 64;
    private static final int BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    private Pkce() {
    }

    /**
     * Makes a fresh random secret for one sign-in attempt.
     *
     * @return the verifier, to be kept until the tokens come back
     */
    @NonNull
    public static String newVerifier() {
        final byte[] random = new byte[VERIFIER_BYTES];
        new SecureRandom().nextBytes(random);
        return Base64.encodeToString(random, BASE64_FLAGS);
    }

    /**
     * Derives the value sent to the browser, which cannot be turned back into the verifier.
     *
     * @param verifier the secret from {@link #newVerifier()}
     * @return the challenge to put in the sign-in address
     */
    @NonNull
    public static String challengeOf(@NonNull final String verifier) {
        try {
            final byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.encodeToString(hashed, BASE64_FLAGS);
        } catch (final NoSuchAlgorithmException e) {
            // every Android device ships SHA-256; there is no sensible fallback
            throw new IllegalStateException(e);
        }
    }
}
