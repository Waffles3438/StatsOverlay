package me.redth.statsoverlay.data

import com.mojang.authlib.GameProfile
import me.redth.statsoverlay.config.ModConfig
import me.redth.statsoverlay.util.HypixelAPI
import me.redth.statsoverlay.util.MojangAPI
import me.redth.statsoverlay.util.Achievements
import me.redth.statsoverlay.util.Bedwars
import me.redth.statsoverlay.util.PlayerObject
import me.redth.statsoverlay.util.Stats
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.max

object FetchQueue {
    private val executor = Executors.newFixedThreadPool(3)
    private val map = ConcurrentHashMap<UUID, Row>()

    fun add(profile: GameProfile) {
        if (map[profile.id] != null) return
        // The tab entry wins over a chat-added row for the same name under another key
        // (a nick gets a fake key), so the player isn't listed twice.
        for (key in chatAdded) {
            if (key != profile.id && map[key]?.name.equals(profile.name, ignoreCase = true)) removeKey(key)
        }
        put(profile)
    }

    private fun put(profile: GameProfile) {
        map[profile.id] = Loading(profile.name, executor.submit { fetch(profile.id) })
    }

    private fun removeKey(key: UUID) {
        chatAdded.remove(key)
        val removed = map.remove(key)
        if (removed is Loading) removed.cancel()
    }

    // Bumped on clear()/pruneChatAdded() so name lookups that finish late are dropped.
    @Volatile
    private var generation = 0

    // Keys of rows that came from chat rather than the tab list.
    private val chatAdded = ConcurrentHashMap.newKeySet<UUID>()

    /** Adds a player known only by name (e.g. from chat while tab names are hidden). */
    fun addByName(name: String) {
        if (map.values.any { it.name.equals(name, ignoreCase = true) }) return
        val gen = generation
        executor.submit {
            val uuid = MojangAPI.getUuid(name)
            if (gen != generation) return@submit
            // The tab list may have picked them up while the lookup was running.
            if (map.values.any { it.name.equals(name, ignoreCase = true) }) return@submit
            if (uuid == null) {
                // Not a real account, so it's a nick. Use a stable fake key.
                val key = UUID.nameUUIDFromBytes("nick:$name".toByteArray())
                if (map.putIfAbsent(key, Nicked(name)) == null) chatAdded += key
                return@submit
            }
            if (map[uuid] != null) return@submit
            chatAdded += uuid
            put(GameProfile(uuid, name))
        }
    }

    /**
     * Called once the game has started and the tab list shows real names. Chat-added rows
     * not backed by a tab entry belong to players who left (or are duplicates), so drop them.
     * Rows that match a tab entry are handed over to normal tab tracking, which also covers
     * removal on final kill.
     */
    fun pruneChatAdded(tabIds: Set<UUID>) {
        generation++ // drop lookups still running from the pre-game lobby
        for (key in chatAdded.toList()) {
            if (key in tabIds) chatAdded.remove(key) else removeKey(key)
        }
    }

    fun remove(profile: GameProfile) = removeKey(profile.id)

    fun clear() {
        generation++
        chatAdded.clear()
        for ((_, row) in map) {
            if (row is Loading) row.cancel()
        }
        map.clear()
    }

    private fun fetch(uuid: UUID) {
        val response = HypixelAPI.getStatsOrCached(uuid) ?: return
        val row = map[uuid]
        if (row !is Loading) return // removed or already loaded
        map[uuid] = when (val stats = response.player) {
            null -> Nicked(row.name)
            else -> Loaded(row.name, stats)
        }
    }

    fun makeGrid(preview: Boolean = false): CompiledGrid {
        val shown = Column.entries.filter { ModConfig.shouldShow(it) }
        val widthArray = IntArray(shown.size)
        val result = buildList {
            add(shown.map { column -> ColoredText(column.label) })
            val rows = map.values.toList().ifEmpty { if (preview) previewRows else emptyList() }
            rows
                .sortedWith(getComparator())
                .mapTo(this) { row ->
                    shown.map { column -> row.getText(column) }
                }
        }
        for (row in result) {
            for ((index, text) in row.withIndex()) {
                widthArray[index] = max(widthArray[index], text.width + 4)
            }
        }
        return CompiledGrid(
            result,
            widthArray.toList(),
            widthArray.sum(),
            result.size * 10
        )
    }

    // Local examples let the editor show the HUD without a server, key press or API request.
    private val previewRows by lazy {
        listOf(
            Loaded("Steve", PlayerObject(Achievements(250), Stats(Bedwars(
                finalKills = 12500, finalDeaths = 2500, kills = 18000, deaths = 12000,
                wins = 2500, losses = 1000, bedsBroken = 4000, bedsLost = 1600, winstreak = 12,
            )))),
            Nicked("Alex"),
        )
    }
}

private fun getComparator(): Comparator<Row> = when {
    ModConfig.sortByTeams -> compareBy<Row> { row -> row.getTeamNameForSorting() }.thenComparing(RowComparator)
    else -> RowComparator
}

