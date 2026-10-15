package me.redth.statsoverlay.config

import kotlin.math.ceil
import kotlin.math.sqrt

/** Rounded background and border geometry in unscaled HUD pixels. */
internal object HudBackground {
    fun draw(
        width: Int,
        height: Int,
        radius: Int,
        border: Int,
        fill: (left: Int, top: Int, right: Int, bottom: Int, outline: Boolean) -> Unit,
    ) {
        if (width <= 0 || height <= 0) return
        val thickness = border.coerceIn(0, minOf(width, height) / 2)
        val outerRadius = if (radius > 0) radius + thickness else 0
        val innerWidth = width - thickness * 2
        val innerHeight = height - thickness * 2
        var bandTop = 0
        var lastOuterLeft = -1
        var lastInnerLeft = -1
        var lastInnerRight = -1
        var lastOuterRight = -1

        fun flush(bottom: Int) {
            if (lastOuterLeft < 0) return
            if (lastOuterLeft < lastInnerLeft) fill(lastOuterLeft, bandTop, lastInnerLeft, bottom, true)
            if (lastInnerLeft < lastInnerRight) fill(lastInnerLeft, bandTop, lastInnerRight, bottom, false)
            if (lastInnerRight < lastOuterRight) fill(lastInnerRight, bandTop, lastOuterRight, bottom, true)
        }

        for (y in 0 until height) {
            val outerLeft = inset(width, height, outerRadius, y)
            val outerRight = width - outerLeft
            val borderOnly = y < thickness || y >= height - thickness || innerWidth <= 0
            val innerLeft = if (borderOnly) outerLeft else
                (thickness + inset(innerWidth, innerHeight, radius, y - thickness))
                    .coerceIn(outerLeft, outerRight)
            val innerRight = if (borderOnly) outerLeft else
                (width - innerLeft).coerceIn(innerLeft, outerRight)
            // Batch identical scanlines so a long player list still needs only a few rectangles.
            if (outerLeft != lastOuterLeft || innerLeft != lastInnerLeft ||
                innerRight != lastInnerRight || outerRight != lastOuterRight) {
                flush(y)
                bandTop = y
                lastOuterLeft = outerLeft
                lastInnerLeft = innerLeft
                lastInnerRight = innerRight
                lastOuterRight = outerRight
            }
        }
        flush(height)
    }

    private fun inset(width: Int, height: Int, radius: Int, y: Int): Int {
        val r = radius.coerceIn(0, minOf(width, height) / 2).toFloat()
        val dy = (r - minOf(y, height - y - 1) - 0.5f).coerceAtLeast(0f)
        return ceil(r - sqrt((r * r - dy * dy).coerceAtLeast(0f)) - 0.5f).toInt().coerceAtLeast(0)
    }
}
