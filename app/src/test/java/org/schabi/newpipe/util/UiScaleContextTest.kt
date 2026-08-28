package org.schabi.newpipe.util

import android.app.Application
import androidx.preference.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.schabi.newpipe.R

/**
 * Covers test cases A5-2 and A5-6 of docs/test-plan.html: at the normal setting nothing about the
 * interface changes, at a larger setting text and controls grow by that factor, and an unusable
 * stored value leaves the interface at its normal size instead of collapsing it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UiScaleContextTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()

    private fun storeScale(value: String?) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        if (value == null) {
            editor.remove(context.getString(R.string.ui_scale_key))
        } else {
            editor.putString(context.getString(R.string.ui_scale_key), value)
        }
        editor.commit()
    }

    private fun baseDensityDpi() = context.resources.configuration.densityDpi

    @Before fun clearStoredScale() {
        storeScale(null)
    }

    @Test fun `without a setting the context is handed back untouched`() {
        assertSame(context, UiScaleHelper.wrapContext(context))
    }

    @Test fun `the normal setting changes nothing`() {
        storeScale("1.0")
        assertSame(context, UiScaleHelper.wrapContext(context))
    }

    @Test fun `a larger setting scales the interface by that factor`() {
        val base = baseDensityDpi()
        storeScale("1.5")

        val scaled = UiScaleHelper.wrapContext(context)

        assertEquals(
            Math.round(base * 1.5f),
            scaled.resources.configuration.densityDpi
        )
    }

    @Test fun `a larger setting leaves room for less content across the screen`() {
        val baseWidthDp = context.resources.configuration.screenWidthDp
        storeScale("1.5")

        val scaled = UiScaleHelper.wrapContext(context)

        // bigger pixels mean fewer of them fit, otherwise layouts meant for a wider screen
        // would be picked and the result would look wrong rather than merely large
        assertTrue(scaled.resources.configuration.screenWidthDp < baseWidthDp)
    }

    @Test fun `an unusable stored value leaves the interface at its normal size`() {
        val base = baseDensityDpi()
        for (broken in listOf("0", "-1.5", "12", "large")) {
            storeScale(broken)
            assertEquals(base, UiScaleHelper.wrapContext(context).resources.configuration.densityDpi)
        }
    }
}
