package org.schabi.newpipe.backup

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the part of test case C2-1 that can be checked without a real account: the address
 * the browser is sent to has to carry everything Dropbox needs, or the driver reaches a page
 * that simply refuses, with nothing on screen to explain why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DropboxSignInTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun `the sign-in address carries the app, the proof and the lasting token request`() {
        val url = Uri.parse(DropboxAccount.startSignIn(context))

        assertEquals("www.dropbox.com", url.host)
        assertEquals("code", url.getQueryParameter("response_type"))
        assertEquals("offline", url.getQueryParameter("token_access_type"))
        assertEquals("S256", url.getQueryParameter("code_challenge_method"))
        assertNotNull("no proof was sent", url.getQueryParameter("code_challenge"))

        val appKey = url.getQueryParameter("client_id")
        assertNotNull("this build has no Dropbox application key", appKey)
        assertTrue("the application key is empty", appKey!!.isNotEmpty())
    }

    @Test fun `each attempt uses a fresh proof`() {
        val first = Uri.parse(DropboxAccount.startSignIn(context))
            .getQueryParameter("code_challenge")
        val second = Uri.parse(DropboxAccount.startSignIn(context))
            .getQueryParameter("code_challenge")

        assertNotEquals("the same proof was reused", first, second)
    }

    @Test fun `the proof in the address matches the secret kept to redeem the code`() {
        val url = Uri.parse(DropboxAccount.startSignIn(context))

        val verifier = DropboxAccount.pendingVerifier(context)

        assertNotNull("nothing was kept to redeem the code with", verifier)
        assertEquals(url.getQueryParameter("code_challenge"), Pkce.challengeOf(verifier!!))
    }
}
