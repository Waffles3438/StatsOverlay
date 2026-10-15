package me.redth.statsoverlay.config

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.annotations.*
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import me.redth.statsoverlay.StatsOverlay
import me.redth.statsoverlay.data.Column
import me.redth.statsoverlay.util.HypixelAPI
import org.lwjgl.sdl.SDLScancode

object ModConfig : Config(StatsOverlay.MODID, StatsOverlay.NAME, Category.HYPIXEL) {
    @Info(title = "Set your Hypixel API key in the API tab")
    var ignored = false

    @Keybind(title = "Overlay Keybind")
    var keyBind = KeybindHelper.builder().key(SDLScancode.SDL_SCANCODE_TAB)
        .action { pressed: Boolean -> me.redth.statsoverlay.EventListener.onOverlayKey(pressed); true }
        .register()

    @Switch(title = "Toggle keybind", description = "Off: hold the key. On: press to toggle.")
    var toggleKeyBind = false

    @Switch(title = "Show on any server", description = "Off: only show during Bedwars.")
    var showAnywhere = false

    @Dropdown(title = "Sort by", options = ["Stars", "Wins", "WLR", "Final Kills", "FKDR", "Kills", "KDR", "Beds Broken", "BBLR", "Winstreak"])
    var sorting = 0

    @Checkbox(title = "Sort by teams after game started")
    var sortByTeams = true

    @Checkbox(title = "Wins", subcategory = "Bedwars")
    var bwWins = true

    @Checkbox(title = "WLR", subcategory = "Bedwars")
    var bwWlr = true

    @Checkbox(title = "Final Kills", subcategory = "Bedwars")
    var bwFinalKills = true

    @Checkbox(title = "FKDR", subcategory = "Bedwars")
    var bwFkdr = true

    @Checkbox(title = "Kills", subcategory = "Bedwars")
    var bwKills = true

    @Checkbox(title = "KDR", subcategory = "Bedwars")
    var bwKdr = true

    @Checkbox(title = "Beds Broken", subcategory = "Bedwars")
    var bwBedsBroken = true

    @Checkbox(title = "BBLR", subcategory = "Bedwars")
    var bwBblr = true

    @Checkbox(title = "Winstreak", subcategory = "Bedwars")
    var bwWinstreak = true

    @Switch(title = "Enable overlay", category = "HUD")
    var overlayEnabled = true

    @Button(title = "Position and scale", category = "HUD", text = "Edit HUD")
    fun editHud() {
        HudManager.openEditor()
    }

    @Switch(title = "Background", category = "HUD", subcategory = "Background")
    var overlayBackground = true

    @Color(title = "Background color", category = "HUD", subcategory = "Background")
    var overlayBackgroundColor = PolyColor(0x80000000.toInt())

    @Switch(title = "Rounded corners", category = "HUD", subcategory = "Background")
    var overlayRounded = true

    @Slider(title = "Corner radius", category = "HUD", subcategory = "Background", min = 0f, max = 32f, step = 1f)
    var overlayCornerRadius = 8

    @Switch(title = "Border", category = "HUD", subcategory = "Background")
    var overlayBorder = false

    @Color(title = "Border color", category = "HUD", subcategory = "Background")
    var overlayBorderColor = PolyColor.BLACK

    @Slider(title = "Border thickness", category = "HUD", subcategory = "Background", min = 1f, max = 10f, step = 1f)
    var overlayBorderThickness = 2

    @Slider(title = "Horizontal padding", category = "HUD", subcategory = "Spacing", min = 0f, max = 50f, step = 1f)
    var overlayPaddingX = 5

    @Slider(title = "Vertical padding", category = "HUD", subcategory = "Spacing", min = 0f, max = 50f, step = 1f)
    var overlayPaddingY = 5

    @Switch(title = "Use Hypixel API", category = "API", description = "Off: use Ursa Minor. On: use your Hypixel API key.")
    var useHypixelAPI = true

    @Text(title = "API Key", category = "API")
    var hypixelApiKey = ""

    @Button(title = "Clear cache", category = "Debug", text = "Clear")
    fun clearCache() {
        HypixelAPI.clearCache()
    }

    fun shouldShow(column: Column): Boolean = when (column) {
        Column.STAR, Column.NAME -> true
        Column.WINS -> bwWins
        Column.WLR -> bwWlr
        Column.FINAL_KILLS -> bwFinalKills
        Column.FKDR -> bwFkdr
        Column.KILLS -> bwKills
        Column.KDR -> bwKdr
        Column.BEDS_BROKEN -> bwBedsBroken
        Column.BBLR -> bwBblr
        Column.WINSTREAK -> bwWinstreak
    }

    fun getComparing(): Column = when (sorting) {
        0 -> Column.STAR
        1 -> Column.WINS
        2 -> Column.WLR
        3 -> Column.FINAL_KILLS
        4 -> Column.FKDR
        5 -> Column.KILLS
        6 -> Column.KDR
        7 -> Column.BEDS_BROKEN
        8 -> Column.BBLR
        9 -> Column.WINSTREAK
        else -> Column.STAR
    }

}
