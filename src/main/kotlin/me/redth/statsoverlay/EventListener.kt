package me.redth.statsoverlay

import com.mojang.authlib.GameProfile
import me.redth.statsoverlay.config.ModConfig
import me.redth.statsoverlay.data.FetchQueue
import me.redth.statsoverlay.util.mc
import net.hypixel.data.type.GameType
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent
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
        // Players who chatted in the pre-game lobby but left aren't in the real tab list,
        // and their leave message used a scrambled name, so drop them once the game starts.
        if (pruneCountdown >= 0 && pruneCountdown-- == 0) FetchQueue.pruneChatAdded(players.keys)
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
        if (players.isNotEmpty() || addedFromChat) FetchQueue.clear()
        players.clear()
        addedFromChat = false
        gameStarted = false
        pruneCountdown = -1
    }

    private var addedFromChat = false

    // Set once the game begins. Chat is only used to find players before that, otherwise
    // a final-killed player (gone from tab) would be re-added as soon as they chat.
    private var gameStarted = false

    // Ticks left before pruning chat-added players once the game has started (-1 = idle).
    private var pruneCountdown = -1

    // Hypixel sends this when the Bedwars game begins. Tab names are real from then on.
    private const val GAME_START_MESSAGE = "Protect your bed and destroy the enemy beds."

    // "[MVP+] Name: msg" or "Name: msg" (colors already stripped). Requiring the name to sit
    // right before ": " skips "Party > ...", "Guild > ...", "From ..." and join/leave lines.
    private val chatRegex = Regex("^(?:\\[[^\\]]+\\] )*([A-Za-z0-9_]{1,16}): ")

    // In the pre-game lobby, Hypixel hides tab names, but chat still shows the real name.
    @Subscribe
    fun onChat(event: ChatEvent.Receive) {
        if (!wasInBedwars || gameStarted) return
        val message = event.fullyUnformattedMessage
        if (message.trim() == GAME_START_MESSAGE) {
            gameStarted = true
            pruneCountdown = 40 // ~2s for the tab list to update
            return
        }
        val name = chatRegex.find(message)?.groupValues?.get(1) ?: return
        if (players.values.any { it.name.equals(name, ignoreCase = true) }) return
        addedFromChat = true
        FetchQueue.addByName(name)
    }

    @Subscribe
    fun onLocation(event: HypixelLocationEvent) {
        val location = event.location
        wasInBedwars = location.gameType.orElse(null) == GameType.BEDWARS &&
            location.mode.orElse("lobby") != "lobby" && !location.inLobby()
    }
}
