package org.schabi.newpipe.speedcam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers test cases C1-2, C1-3 and C1-4 of docs/test-plan.html: only cameras ahead are
 * announced, the other carriageway is left alone, each stage is called once as the car closes
 * in, and sitting in traffic beside a camera does not turn the warning into a loop.
 *
 * Positions are built along a north-south road in Kaohsiung. One degree of latitude is about
 * 111 kilometres, which is where the offsets below come from.
 */
class SpeedCameraAlertsTest {
    private val baseLat = 22.6100
    private val baseLon = 120.3000
    private val northbound = 0f
    private val southbound = 180f

    private fun metersNorth(meters: Double) = baseLat + meters / 111_320.0

    private fun camera(
        atMetersNorth: Double,
        limit: Int = 60,
        watches: String? = null,
        deck: SpeedCamera.Deck = SpeedCamera.Deck.UNKNOWN
    ) = SpeedCamera(
        metersNorth(atMetersNorth),
        baseLon,
        limit,
        deck,
        CameraDirections.parse(watches)
    )

    /** Drives north to a point the given distance short of the camera. */
    private fun SpeedCameraAlerts.callAt(
        camera: SpeedCamera,
        metersShort: Double,
        heading: Float = northbound,
        scale: Float = 1f
    ) = nextAlert(
        listOf(camera),
        camera.latitude() - metersShort / 111_320.0,
        baseLon,
        heading,
        scale
    )

    @Test fun `each stage is called once as the car closes in`() {
        val camera = camera(atMetersNorth = 1000.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertEquals(500, alerts.callAt(camera, 480.0)?.stageMeters())
        assertNull("the same stage was called twice", alerts.callAt(camera, 450.0))
        assertEquals(300, alerts.callAt(camera, 290.0)?.stageMeters())
        assertEquals(200, alerts.callAt(camera, 190.0)?.stageMeters())
        assertEquals(100, alerts.callAt(camera, 90.0)?.stageMeters())
        assertNull("something was called past the last stage", alerts.callAt(camera, 60.0))
    }

    @Test fun `joining a road close to a camera still gets one warning`() {
        val camera = camera(atMetersNorth = 1000.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        // first sight at 250 metres: the stage the car is already inside, not silence
        assertEquals(300, alerts.callAt(camera, 250.0)?.stageMeters())
        assertEquals(200, alerts.callAt(camera, 180.0)?.stageMeters())
    }

    @Test fun `nothing is said while the camera is still far off`() {
        val camera = camera(atMetersNorth = 1000.0, watches = "南向北")

        assertNull(SpeedCameraAlerts().callAt(camera, 700.0))
    }

    @Test fun `passing the camera is confirmed once`() {
        val camera = camera(atMetersNorth = 500.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNotNull(alerts.callAt(camera, 250.0))

        // now 150 metres beyond it, still heading north
        val beyond = alerts.nextAlert(
            listOf(camera),
            metersNorth(650.0),
            baseLon,
            northbound,
            1f
        )
        assertTrue("passing was not announced", beyond!!.passed())

        assertNull(
            "passing was announced twice",
            alerts.nextAlert(listOf(camera), metersNorth(700.0), baseLon, northbound, 1f)
        )
    }

    @Test fun `a camera never warned about is not announced as passed`() {
        // driving away from a camera on another road should stay silent, not say "passed"
        val elsewhere = camera(atMetersNorth = -300.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNull(alerts.nextAlert(listOf(elsewhere), baseLat, baseLon, northbound, 1f))
    }

    @Test fun `a camera watching the opposite carriageway stays quiet`() {
        val oncoming = camera(atMetersNorth = 250.0, watches = "北向南")

        assertNull(SpeedCameraAlerts().callAt(oncoming, 250.0))
    }

    @Test fun `a camera with no readable direction is announced either way`() {
        val bothWays = camera(atMetersNorth = 250.0, watches = "往大溪方向")

        // approaching from the south, driving north
        assertNotNull(SpeedCameraAlerts().callAt(bothWays, 250.0))

        // and approaching the same camera from the north, driving south
        assertNotNull(
            SpeedCameraAlerts().nextAlert(
                listOf(bothWays),
                bothWays.latitude() + 250.0 / 111_320.0,
                baseLon,
                southbound,
                1f
            )
        )
    }

    @Test fun `stopped in traffic between stages nothing is repeated`() {
        val camera = camera(atMetersNorth = 400.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertEquals(200, alerts.callAt(camera, 180.0)?.stageMeters())
        repeat(20) {
            assertNull(alerts.callAt(camera, 180.0))
        }
    }

    @Test fun `a later trip past the same camera speaks again`() {
        val camera = camera(atMetersNorth = 400.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNotNull(alerts.callAt(camera, 250.0))

        // three kilometres away puts it behind us for good
        assertNull(alerts.callAt(camera, 3000.0))

        assertEquals(300, alerts.callAt(camera, 250.0)?.stageMeters())
    }

    @Test fun `turning the distance setting up warns from further out`() {
        val camera = camera(atMetersNorth = 1000.0, watches = "南向北")

        assertNull(SpeedCameraAlerts().callAt(camera, 700.0, scale = 1f))
        assertEquals(500, SpeedCameraAlerts().callAt(camera, 700.0, scale = 1.5f)?.stageMeters())
    }

    @Test fun `at elevated speed the elevated camera is the one announced`() {
        // an elevated road and the street beneath share a position; only the speed tells them
        // apart, and at 75 the car cannot be on a street limited to 50
        val elevated = camera(
            atMetersNorth = 300.0,
            limit = 80,
            watches = "南向北",
            deck = SpeedCamera.Deck.ELEVATED
        )
        val beneath = camera(
            atMetersNorth = 295.0,
            limit = 50,
            watches = "南向北",
            deck = SpeedCamera.Deck.UNDER
        )

        val alert = SpeedCameraAlerts().nextAlert(
            listOf(beneath, elevated),
            baseLat,
            baseLon,
            northbound,
            1f,
            75f
        )

        assertEquals(SpeedCamera.Deck.ELEVATED, alert?.camera()?.deck())
    }

    @Test fun `at street speed the camera beneath is the one announced`() {
        val elevated = camera(
            atMetersNorth = 300.0,
            limit = 80,
            watches = "南向北",
            deck = SpeedCamera.Deck.ELEVATED
        )
        val beneath = camera(
            atMetersNorth = 295.0,
            limit = 50,
            watches = "南向北",
            deck = SpeedCamera.Deck.UNDER
        )

        val alert = SpeedCameraAlerts().nextAlert(
            listOf(elevated, beneath),
            baseLat,
            baseLon,
            northbound,
            1f,
            45f
        )

        assertEquals(SpeedCamera.Deck.UNDER, alert?.camera()?.deck())
    }

    @Test fun `without a speed reading nothing is guessed about the deck`() {
        val elevated = camera(
            atMetersNorth = 300.0,
            limit = 80,
            watches = "南向北",
            deck = SpeedCamera.Deck.ELEVATED
        )
        val beneath = camera(
            atMetersNorth = 295.0,
            limit = 50,
            watches = "南向北",
            deck = SpeedCamera.Deck.UNDER
        )

        // the nearer camera is kept rather than a guess being made
        val alert = SpeedCameraAlerts().nextAlert(
            listOf(elevated, beneath),
            baseLat,
            baseLon,
            northbound,
            1f,
            0f
        )

        assertEquals(SpeedCamera.Deck.UNDER, alert?.camera()?.deck())
    }

    @Test fun `two cameras with similar limits are not second guessed`() {
        // 60 and 50 are too close for a speed reading to tell the roads apart
        val far = camera(atMetersNorth = 300.0, limit = 60, watches = "南向北")
        val near = camera(atMetersNorth = 295.0, limit = 50, watches = "南向北")

        val alert = SpeedCameraAlerts().nextAlert(
            listOf(far, near),
            baseLat,
            baseLon,
            northbound,
            1f,
            62f
        )

        assertEquals(50, alert?.camera()?.limitKmh())
    }

    @Test fun `being told to slow down waits until the car really is over the limit`() {
        // a car's own speedometer reads a few kilometres high, so a driver seeing 62 on the
        // dashboard at a 60 limit is not speeding and must not be scolded
        assertEquals(false, SpeedCameraAlerts.isOverLimit(58f, 60))
        assertEquals(false, SpeedCameraAlerts.isOverLimit(65f, 60))

        assertEquals(true, SpeedCameraAlerts.isOverLimit(66f, 60))
        assertEquals(true, SpeedCameraAlerts.isOverLimit(95f, 60))
    }

    @Test fun `a camera with no published limit never says slow down`() {
        // saying "slow down" without a number to slow down to is noise, not help
        assertEquals(false, SpeedCameraAlerts.isOverLimit(120f, 0))
    }

    @Test fun `standing still is never over the limit`() {
        assertEquals(false, SpeedCameraAlerts.isOverLimit(0f, 40))
    }
}
