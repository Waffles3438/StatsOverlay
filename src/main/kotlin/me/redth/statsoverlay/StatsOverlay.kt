package me.redth.statsoverlay

import me.redth.statsoverlay.config.ModConfig
import me.redth.statsoverlay.config.OverlayHUD
import net.fabricmc.api.ClientModInitializer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hypixel.v1.HypixelUtils

object StatsOverlay : ClientModInitializer {
    const val MODID = "statsoverlay"
    const val NAME = "StatsOverlay"

    override fun onInitializeClient() {
        ModConfig.preload()
        HypixelUtils.getLocation()
        EventManager.INSTANCE.register(EventListener)
        HudManager.register(OverlayHUD(), MODID)
    }
}
