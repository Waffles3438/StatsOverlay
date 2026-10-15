package me.redth.statsoverlay.data

import com.mojang.authlib.GameProfile
import me.redth.statsoverlay.config.ModConfig
import me.redth.statsoverlay.util.HypixelAPI
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
        map[profile.id] = Loading(profile.name, executor.submit { fetch(profile.id) })
    }

    fun remove(profile: GameProfile) {
        val removed = map.remove(profile.id)
        if (removed is Loading) removed.cancel()
    }

    fun clear() {
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

