package me.redth.statsoverlay

import com.mojang.authlib.GameProfile
import me.redth.statsoverlay.config.ModConfig
import me.redth.statsoverlay.data.FetchQueue
import me.redth.statsoverlay.util.mc
import net.hypixel.data.type.GameType
import org.polyfrost.oneconfig.api.event.v1.events.HypixelLocationEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe
import java.util.UUID

object EventListener {
    private var wasInBedwars = false
    private var keyHeld = false
    private var keyToggled = false
    private val players = mutableMapOf<UUID, GameProfile>()

    fun shouldShow() = mc.world != null && isInAllowedArea() &&
        if (ModConfig.toggleKeyBind) keyToggled else keyHeld

    private fun isInAllowedArea() = ModConfig.showAnywhere || wasInBedwars

    fun onOverlayKey(pressed: Boolean) {
        keyHeld = pressed
        if (pressed && ModConfig.toggleKeyBind) keyToggled = !keyToggled
    }

    // Reconcile the tab list on the client thread after vanilla handles its packets.
    // This also picks up players already present when "show anywhere" is enabled.
    @Subscribe
    fun onTick(event: TickEvent.End) {
        if (!isInAllowedArea()) {
            clearPlayers()
            return
        }
        val current = mc.networkHandler?.onlinePlayers?.map { it.profile }
            ?.filter { it.id != null && it.id.version() != 2 }
            ?.associateBy { it.id } ?: emptyMap()
        for ((id, profile) in players) {
            if (id !in current) FetchQueue.remove(profile)
        }
        for ((id, profile) in current) {
            if (id !in players) FetchQueue.add(profile)
        }
        players.clear()
        players.putAll(current)
    }

    @Subscribe
    fun onWorldLoad(event: WorldEvent.Load) = reset()

    @Subscribe
    fun onWorldUnload(event: WorldEvent.Unload) = reset()

    private fun reset() {
        clearPlayers()
        wasInBedwars = false
    }

    private fun clearPlayers() {
        if (players.isNotEmpty()) FetchQueue.clear()
        players.clear()
    }

    @Subscribe
    fun onLocation(event: HypixelLocationEvent) {
        val location = event.location
        wasInBedwars = location.gameType.orElse(null) == GameType.BEDWARS &&
            location.mode.orElse("lobby") != "lobby" && !location.inLobby()
    }
}
