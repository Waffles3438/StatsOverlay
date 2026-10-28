package me.redth.statsoverlay.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.util.*

/** Resolves usernames to UUIDs, used for players only seen through chat. */
object MojangAPI {
    private val gson = Gson()

    // Optional so that "no such player" (likely a nick) is cached too.
    private val cache: LoadingCache<String, Optional<UUID>> = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(Duration.ofMinutes(30))
        .build { name -> Optional.ofNullable(fetch(name)) }

    /** Returns the UUID, or null if the name doesn't exist (or the lookup failed). */
    fun getUuid(name: String): UUID? = cache[name.lowercase()]?.orElse(null)

    private fun fetch(name: String): UUID? {
        val connection = (URL("https://api.mojang.com/users/profiles/minecraft/$name")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            readTimeout = 5000
            connectTimeout = 5000
            addRequestProperty("User-Agent", "StatsOverlay :>")
        }
        return runCatching {
            if (connection.responseCode != 200) return null // 204/404 = unknown name
            val json = connection.inputStream.bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
            val raw = json.get("id").asString
            UUID.fromString(raw.replaceFirst(
                Regex("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})"), "$1-$2-$3-$4-$5"
            ))
        }.onFailure { it.printStackTrace() }.getOrNull()
    }
}
