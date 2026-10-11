# StatsOverlay

A configurable Hypixel Bedwars stats overlay for **Ornithe 1.8.9**, using **OneConfig 1.4.0**.

![Overlay](./images/overlay.png)
![Settings](./images/settings.png)

## Installation

Use an Ornithe **generation 2** Minecraft 1.8.9 installation with **Java 25 or newer** and Fabric Loader **0.19.5 or newer**. Install the following Ornithe-compatible mods and their required dependencies:

- [OneConfig 1.4.0](https://modrinth.com/mod/oneconfig/version/ghWURw3V)
- [Compose Multiplatform 1.0.6](https://modrinth.com/mod/compose-multiplatform/version/aQmt5aNV)
- [Fabric Language Kotlin 1.14.1+kotlin.2.4.20 or newer](https://modrinth.com/mod/fabric-language-kotlin)
- [Ornithe Standard Libraries 0.21.1 or newer](https://modrinth.com/mod/osl)
- Pylon 0.1.7 or newer (required by OneConfig's Ornithe platform)

Put `StatsOverlay-1.8.9-ornithe-0.3.0.jar` in the instance's `mods` folder. This release uses Ornithe; remove the previous Forge StatsOverlay JAR from the instance.

Open the OneConfig menu to configure StatsOverlay. Set your Hypixel API key in the API tab, or disable "Use Hypixel API" to use Ursa Minor. Hold **Tab** to show the overlay; enable "Toggle keybind" for toggle mode. Bedwars is the default location filter, and "Show on any server" removes it. Move and scale the overlay in OneConfig's HUD editor.

The OneConfig v1 HUD has separate saved settings. Reconfigure the overlay position and keybind when migrating from the old Forge/OneConfig 0.2 release.

## Building

Set `JAVA_HOME` to a **JDK 25** installation, then run:

```powershell
.\gradlew.bat build
```

On Linux or macOS, use `./gradlew build`. The distributable JAR is written to `build/libs/StatsOverlay-1.8.9-ornithe-0.3.0.jar`; the `-sources.jar` is for development.

`./gradlew runClient` launches a development client with OneConfig, Compose, OSL, Pylon, and Kotlin on the runtime classpath. Development clients need an authenticated Minecraft session to access Hypixel and Ursa Minor.

The build uses Fabric Loom, Ploceus, and Feather generation 2 mappings. OneConfig's Minecraft-rendered HUD API keeps vanilla font formatting and Bedwars prestige colors. StatsOverlay bundles its Caffeine cache dependency; OneConfig is installed separately.
