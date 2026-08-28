package org.schabi.newpipe.player.helper

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.Player.REPEAT_MODE_OFF
import com.google.android.exoplayer2.Player.REPEAT_MODE_ONE
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers test case A3-1 and A3-3 of docs/test-plan.html: the repeat mode chosen once must still
 * be in effect on the next trip, and a fresh install must start with repeating off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RepeatModePersistenceTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val prefs get() = PreferenceManager.getDefaultSharedPreferences(context)

    @Before fun clearStoredMode() {
        prefs.edit().clear().commit()
    }

    @Test fun `a fresh install starts with repeating off`() {
        assertEquals(REPEAT_MODE_OFF, PlayerHelper.retrieveRepeatModeFromPrefs(context, prefs))
    }

    @Test fun `the chosen mode survives into the next session`() {
        PlayerHelper.saveRepeatModeToPrefs(context, prefs, REPEAT_MODE_ONE)
        assertEquals(REPEAT_MODE_ONE, PlayerHelper.retrieveRepeatModeFromPrefs(context, prefs))

        PlayerHelper.saveRepeatModeToPrefs(context, prefs, REPEAT_MODE_ALL)
        assertEquals(REPEAT_MODE_ALL, PlayerHelper.retrieveRepeatModeFromPrefs(context, prefs))
    }

    @Test fun `pressing the button three times leaves the stored mode where it started`() {
        var mode = REPEAT_MODE_OFF
        repeat(3) {
            mode = PlayerHelper.nextRepeatMode(mode)
            PlayerHelper.saveRepeatModeToPrefs(context, prefs, mode)
        }
        assertEquals(REPEAT_MODE_OFF, PlayerHelper.retrieveRepeatModeFromPrefs(context, prefs))
    }

    @Test fun `a corrupted stored value falls back to repeating off`() {
        prefs.edit().putInt(context.getString(org.schabi.newpipe.R.string.last_repeat_mode), 42)
            .commit()
        assertEquals(REPEAT_MODE_OFF, PlayerHelper.retrieveRepeatModeFromPrefs(context, prefs))
    }
}
