package org.schabi.newpipe.speedcam

import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Covers test case C1-3 of docs/test-plan.html at its root: a camera may only be tied to a
 * direction when the data really names one. Every string here is taken from the published
 * police data set.
 */
class CameraDirectionsTest {
    private fun assertWatches(expected: IntArray, published: String?) {
        assertArrayEquals(
            "wrong direction read from \"$published\"",
            expected,
            CameraDirections.parse(published)
        )
    }

    private val everyDirection = intArrayOf()

    @Test fun `both ways along a road gives both directions`() {
        assertWatches(intArrayOf(0, 180), "南北雙向")
        assertWatches(intArrayOf(0, 180), "北南雙向")
        assertWatches(intArrayOf(0, 180), "南北向")
        assertWatches(intArrayOf(0, 180), "南北雙向(區間測速)")
        assertWatches(intArrayOf(90, 270), "東西雙向")
        assertWatches(intArrayOf(90, 270), "東西向")
        assertWatches(intArrayOf(90, 270), "東西雙向（區間測速）")
    }

    @Test fun `from one place to another gives the direction of travel`() {
        assertWatches(intArrayOf(180), "北向南")
        assertWatches(intArrayOf(0), "南向北")
        assertWatches(intArrayOf(0), "南往北")
        assertWatches(intArrayOf(270), "東往西")
        assertWatches(intArrayOf(90), "西向東")
        assertWatches(intArrayOf(45), "西南向東北")
        assertWatches(intArrayOf(225), "東北向西南")
        assertWatches(intArrayOf(135), "西北向東南")
        assertWatches(intArrayOf(315), "東南向西北")
        assertWatches(intArrayOf(270), "東往西向")
    }

    @Test fun `a single direction word is the direction of travel`() {
        assertWatches(intArrayOf(180), "往南")
        assertWatches(intArrayOf(0), "往北")
        assertWatches(intArrayOf(90), "往東")
        assertWatches(intArrayOf(180), "南向")
        assertWatches(intArrayOf(0), "北向")
        assertWatches(intArrayOf(90), "東向")
    }

    @Test fun `carriageway wording is read as the direction it runs`() {
        assertWatches(intArrayOf(180), "南下")
        assertWatches(intArrayOf(0), "北上")
        assertWatches(intArrayOf(180), "南下車道")
        assertWatches(intArrayOf(0), "北上車道")
        assertWatches(intArrayOf(0), "北上方向")
        assertWatches(intArrayOf(180), "南下方向")
    }

    @Test fun `a compass word inside a place name is not a direction`() {
        // reading these as a direction would silence the camera for drivers going the other
        // way, and a missed camera is the expensive mistake
        assertWatches(everyDirection, "往台北市")
        assertWatches(everyDirection, "往南崁方向")
        assertWatches(everyDirection, "往大溪方向")
        assertWatches(everyDirection, "往南桃園交流道方向")
        assertWatches(everyDirection, "中山北路上往平鎮")
    }

    @Test fun `naming both compass axes still means both ways along the road`() {
        assertWatches(intArrayOf(90, 270), "往東西向")
        assertWatches(intArrayOf(0, 180), "往南北")
    }

    @Test fun `anything unreadable watches every direction`() {
        assertWatches(everyDirection, null)
        assertWatches(everyDirection, "")
        assertWatches(everyDirection, "雙向")
        assertWatches(everyDirection, "多向")
        assertWatches(everyDirection, "雙向測速科技執法")
        assertWatches(everyDirection, "南向50北向60")
    }
}
