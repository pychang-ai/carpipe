package org.schabi.newpipe.speedcam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers test cases C1-2, C1-3 and C1-4 of docs/test-plan.html: only cameras ahead are
 * announced, the other carriageway is left alone, and sitting in traffic beside a camera
 * does not turn the warning into a loop.
 *
 * Positions are built from a point on a north-south road in Kaohsiung. One degree of
 * latitude is about 111 kilometres, which is where the offsets below come from.
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
        watches: String? = null
    ) = SpeedCamera(metersNorth(atMetersNorth), baseLon, limit, CameraDirections.parse(watches))

    @Test fun `a camera ahead on our side is announced`() {
        val ahead = camera(atMetersNorth = 250.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        val hit = alerts.nextAlert(listOf(ahead), baseLat, baseLon, northbound, 1f)

        assertEquals(ahead, hit)
    }

    @Test fun `a camera we have already passed stays quiet`() {
        val behind = camera(atMetersNorth = -250.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNull(alerts.nextAlert(listOf(behind), baseLat, baseLon, northbound, 1f))
    }

    @Test fun `a camera watching the opposite carriageway stays quiet`() {
        val oncoming = camera(atMetersNorth = 250.0, watches = "北向南")
        val alerts = SpeedCameraAlerts()

        assertNull(alerts.nextAlert(listOf(oncoming), baseLat, baseLon, northbound, 1f))
    }

    @Test fun `a camera with no readable direction is announced either way`() {
        val bothWays = camera(atMetersNorth = 250.0, watches = "往大溪方向")
        val alerts = SpeedCameraAlerts()

        assertNotNull(alerts.nextAlert(listOf(bothWays), baseLat, baseLon, northbound, 1f))

        val other = SpeedCameraAlerts()
        val behindUsGoingSouth = camera(atMetersNorth = -250.0, watches = "往大溪方向")
        assertNotNull(
            other.nextAlert(listOf(behindUsGoingSouth), baseLat, baseLon, southbound, 1f)
        )
    }

    @Test fun `a camera still far away stays quiet until we get close`() {
        val faraway = camera(atMetersNorth = 900.0, limit = 60, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNull(alerts.nextAlert(listOf(faraway), baseLat, baseLon, northbound, 1f))
    }

    @Test fun `stopped in traffic beside a camera it is announced only once`() {
        val ahead = camera(atMetersNorth = 200.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNotNull(alerts.nextAlert(listOf(ahead), baseLat, baseLon, northbound, 1f))
        repeat(20) {
            assertNull(alerts.nextAlert(listOf(ahead), baseLat, baseLon, northbound, 1f))
        }
    }

    @Test fun `the same camera speaks again on a later trip past it`() {
        val camera = camera(atMetersNorth = 200.0, watches = "南向北")
        val alerts = SpeedCameraAlerts()

        assertNotNull(alerts.nextAlert(listOf(camera), baseLat, baseLon, northbound, 1f))

        // drive three kilometres away, which puts it behind us for good
        val farAwayLat = metersNorth(-3000.0)
        assertNull(alerts.nextAlert(listOf(camera), farAwayLat, baseLon, northbound, 1f))

        // and come back
        assertNotNull(alerts.nextAlert(listOf(camera), baseLat, baseLon, northbound, 1f))
    }

    @Test fun `the warning comes earlier where the road is faster`() {
        assertEquals(600, SpeedCameraAlerts.warningDistanceMeters(100))
        assertEquals(450, SpeedCameraAlerts.warningDistanceMeters(70))
        assertEquals(300, SpeedCameraAlerts.warningDistanceMeters(50))
        assertEquals(200, SpeedCameraAlerts.warningDistanceMeters(40))
        assertEquals(200, SpeedCameraAlerts.warningDistanceMeters(0))
    }

    @Test fun `being told to slow down waits until the car really is over the limit`() {
        // a car's own speedometer reads a few kilometres high, so a driver seeing 62 on the
        // dashboard at a 60 limit is not speeding and must not be scolded
        assertEquals(false, SpeedCameraAlerts.isOverLimit(58f, 60))
        assertEquals(false, SpeedCameraAlerts.isOverLimit(60f, 60))
        assertEquals(false, SpeedCameraAlerts.isOverLimit(64f, 60))
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

    @Test fun `turning the distance setting up warns from further out`() {
        val camera = camera(atMetersNorth = 500.0, limit = 60, watches = "南向北")

        assertNull(SpeedCameraAlerts().nextAlert(listOf(camera), baseLat, baseLon, northbound, 1f))
        assertNotNull(
            SpeedCameraAlerts().nextAlert(listOf(camera), baseLat, baseLon, northbound, 2f)
        )
    }
}
