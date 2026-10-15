package me.redth.statsoverlay.config

import org.junit.Assert.*
import org.junit.Test

class HudBackgroundTest {
    @Test
    fun `square background fills every pixel exactly once without an outline`() {
        val pixels = rasterize(24, 14, radius = 0, border = 0)
        assertTrue(pixels.all { row -> row.all { it == 1 } })
    }

    @Test
    fun `border surrounds the background without painting underneath it`() {
        val pixels = rasterize(24, 14, radius = 0, border = 2)
        for (y in pixels.indices) {
            for (x in pixels[y].indices) {
                val expected = if (x < 2 || x >= 22 || y < 2 || y >= 12) 2 else 1
                assertEquals("pixel $x,$y", expected, pixels[y][x])
            }
        }
    }

    @Test
    fun `rounded backgrounds and borders are symmetric with transparent corners`() {
        for (border in listOf(0, 1, 4)) {
            val pixels = rasterize(40, 24, radius = 8, border = border)
            assertEquals(0, pixels[0][0])
            assertEquals(1, pixels[12][20])
            assertEquals(if (border == 0) 1 else 2, pixels[12][0])
            for (y in pixels.indices) {
                for (x in pixels[y].indices) {
                    assertEquals(pixels[y][x], pixels[23 - y][x])
                    assertEquals(pixels[y][x], pixels[y][39 - x])
                }
            }
        }
    }

    @Test
    fun `large radii and borders remain within small HUD bounds`() {
        for (width in 1..12) {
            for (height in 1..12) {
                rasterize(width, height, radius = 32, border = 10)
            }
        }
    }

    @Test
    fun `empty HUDs draw nothing`() {
        HudBackground.draw(0, 10, 8, 2) { _, _, _, _, _ -> fail("empty width") }
        HudBackground.draw(10, 0, 8, 2) { _, _, _, _, _ -> fail("empty height") }
    }

    @Test
    fun `long player lists do not add background draw calls`() {
        fun count(height: Int): Int {
            var calls = 0
            HudBackground.draw(400, height, 8, 2) { _, _, _, _, _ -> calls++ }
            return calls
        }
        assertEquals(count(60), count(1000))
        var squareCalls = 0
        HudBackground.draw(400, 1000, 0, 0) { _, _, _, _, _ -> squareCalls++ }
        assertEquals(1, squareCalls)
    }

    private fun rasterize(width: Int, height: Int, radius: Int, border: Int): Array<IntArray> {
        val pixels = Array(height) { IntArray(width) }
        HudBackground.draw(width, height, radius, border) { left, top, right, bottom, outline ->
            assertTrue(left in 0 until right && right <= width)
            assertTrue(top in 0 until bottom && bottom <= height)
            for (y in top until bottom) {
                for (x in left until right) {
                    // Overdraw would change the requested opacity of translucent colors.
                    assertEquals("overlapping fills at $x,$y", 0, pixels[y][x])
                    pixels[y][x] = if (outline) 2 else 1
                }
            }
        }
        return pixels
    }
}
