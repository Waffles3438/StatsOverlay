package me.redth.statsoverlay.config

import me.redth.statsoverlay.EventListener
import me.redth.statsoverlay.data.Alignment
import me.redth.statsoverlay.data.CompiledGrid
import me.redth.statsoverlay.data.ColoredText
import me.redth.statsoverlay.data.FetchQueue
import me.redth.statsoverlay.util.mc
import net.minecraft.client.gui.GuiElement
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import kotlin.math.ceil
import kotlin.math.sqrt

private const val ROW_HEIGHT = 10

// OneConfig's Minecraft-rendered HUD API preserves vanilla formatting and fonts.
class OverlayHUD : LegacyHud("statsoverlay-overlay", "StatsOverlay", Category.INFO) {
    @Transient
    private var compiledGrid: CompiledGrid? = null

    init {
        padLeft = 5f
        padRight = 5f
        padTop = 5f
        padBottom = 5f
        bgRadius = 8f
    }

    override fun update(): Boolean {
        val next = FetchQueue.makeGrid()
        if (compiledGrid == next) return false
        compiledGrid = next
        return true
    }

    override val width get() = (compiledGrid?.width?.toFloat() ?: 1f) + padLeft + padRight
    override val height get() = (compiledGrid?.height?.toFloat() ?: ROW_HEIGHT.toFloat()) + padTop + padBottom
    override fun showByDefault() = true
    override fun updateFrequency() = 50_000_000L
    override fun multipleInstancesAllowed() = false
    override fun shouldShow() = EventListener.shouldShow()

    // OneConfig applies position and scale before calling render.
    override fun render() {
        val grid = compiledGrid ?: return
        if (showBackground) drawBackground()
        var yNow = padTop.toInt()
        for (row in grid.grid) {
            var xNow = padLeft.toInt()
            for ((index, text) in row.withIndex()) {
                val width = grid.widthMap[index]
                drawText(text, xNow, yNow, width)
                xNow += width
            }
            yNow += ROW_HEIGHT
        }
    }

    private fun drawBackground() {
        val w = ceil(width).toInt()
        val h = ceil(height).toInt()
        val radius = bgRadius.coerceIn(0f, minOf(w, h) / 2f).toInt()
        GuiElement.fill(0, radius, w, h - radius, bgColor)
        for (row in 0 until radius) {
            val dy = radius - row - 0.5f
            val inset = ceil(radius - sqrt(radius * radius - dy * dy)).toInt()
            GuiElement.fill(inset, row, w - inset, row + 1, bgColor)
            GuiElement.fill(inset, h - row - 1, w - inset, h - row, bgColor)
        }
    }

    private fun drawText(text: ColoredText, x: Int, y: Int, width: Int) {
        val offset = when (text.alignment) {
            Alignment.LEFT -> 0f
            Alignment.CENTER -> (width - text.width) / 2f
            Alignment.RIGHT -> (width - text.width).toFloat()
        }
        mc.textRenderer.draw(text.text, x.toFloat() + offset, y.toFloat(), text.color, true)
    }
}
