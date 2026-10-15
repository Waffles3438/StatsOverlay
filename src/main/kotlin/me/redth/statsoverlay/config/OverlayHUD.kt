package me.redth.statsoverlay.config

import me.redth.statsoverlay.EventListener
import me.redth.statsoverlay.data.Alignment
import me.redth.statsoverlay.data.CompiledGrid
import me.redth.statsoverlay.data.ColoredText
import me.redth.statsoverlay.data.FetchQueue
import me.redth.statsoverlay.util.mc
import net.minecraft.client.gui.GuiElement
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

private const val ROW_HEIGHT = 10

// OneConfig's Minecraft-rendered HUD API preserves vanilla formatting and fonts.
class OverlayHUD : LegacyHud("statsoverlay-overlay", "StatsOverlay", Category.INFO) {
    @Transient
    private var compiledGrid: CompiledGrid? = null

    private val borderThickness get() = if (ModConfig.overlayBackground && ModConfig.overlayBorder)
        ModConfig.overlayBorderThickness.coerceIn(1, 10) else 0
    private val paddingX get() = ModConfig.overlayPaddingX.coerceIn(0, 50) + borderThickness
    private val paddingY get() = ModConfig.overlayPaddingY.coerceIn(0, 50) + borderThickness

    override fun update(): Boolean {
        val next = FetchQueue.makeGrid(preview = HudManager.isEditing)
        if (compiledGrid == next) return false
        compiledGrid = next
        return true
    }

    override val width get() = (compiledGrid?.width?.toFloat() ?: 1f) + paddingX * 2
    override val height get() = (compiledGrid?.height?.toFloat() ?: ROW_HEIGHT.toFloat()) + paddingY * 2
    override fun showByDefault() = true
    override fun updateFrequency() = 50_000_000L
    override fun multipleInstancesAllowed() = false
    override fun shouldShow() = ModConfig.overlayEnabled && EventListener.shouldShow()

    // OneConfig applies position and scale before calling render.
    override fun render() {
        val grid = compiledGrid ?: return
        if (ModConfig.overlayBackground) drawBackground()
        var yNow = paddingY
        for (row in grid.grid) {
            var xNow = paddingX
            for ((index, text) in row.withIndex()) {
                val width = grid.widthMap[index]
                drawText(text, xNow, yNow, width)
                xNow += width
            }
            yNow += ROW_HEIGHT
        }
    }

    private fun drawBackground() {
        val border = borderThickness
        val radius = if (ModConfig.overlayRounded) ModConfig.overlayCornerRadius.coerceIn(0, 32) else 0
        val backgroundColor = ModConfig.overlayBackgroundColor.argb
        val borderColor = ModConfig.overlayBorderColor.argb
        // Separate fill and border spans keep translucent colors from layering over each other.
        HudBackground.draw(width.toInt(), height.toInt(), radius, border) { left, top, right, bottom, outline ->
            GuiElement.fill(left, top, right, bottom, if (outline) borderColor else backgroundColor)
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
