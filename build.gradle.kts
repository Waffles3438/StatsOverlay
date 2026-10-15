plugins {
    kotlin("jvm") version "2.4.20"
    id("net.fabricmc.fabric-loom-remap") version "1.17.21"
    id("ploceus") version "1.17.4"
}

group = "me.redth"
version = providers.gradleProperty("mod_version").get()
base.archivesName.set("StatsOverlay-1.8.9-ornithe")

repositories {
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://maven.hypixel.net/releases") { content { includeGroup("net.hypixel") } }
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://api.modrinth.com/maven")
    google()
}

ploceus {
    setIntermediaryGeneration(2)
}

val oneconfigVersion = providers.gradleProperty("oneconfig_version").get()
val developmentMods by configurations.creating
dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings(ploceus.featherMappings("2"))
    modImplementation("net.fabricmc:fabric-loader:0.19.5")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.14.1+kotlin.2.4.20")
    ploceus.dependOsl("0.21.1")

    // API modules for compilation; the distributed bootstrap supplies them in-game.
    modCompileOnly("org.polyfrost.oneconfig:1.8.9-ornithe:$oneconfigVersion") { isTransitive = false }
    listOf("config", "config-impl", "events", "hud", "poly-compose", "ui", "utils").forEach {
        compileOnly("org.polyfrost.oneconfig:$it:$oneconfigVersion") { isTransitive = false }
    }
    compileOnly("net.hypixel:hypixel-data:0.2.0")
    compileOnly("net.hypixel:mod-api:1.0.2")
    developmentMods("maven.modrinth:oneconfig:zOz05Fjk") { isTransitive = false }
    developmentMods("maven.modrinth:compose-multiplatform:aQmt5aNV") { isTransitive = false }
    modImplementation("pl.tomgirl:pylon:0.1.7")

    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    include("com.github.ben-manes.caffeine:caffeine:2.9.3")

    testImplementation("junit:junit:4.13.2")
}

// Load published distributions from mods so Fabric also discovers and remaps
// their nested libraries. Loom strips nested JARs from its development classpath.
val installDevelopmentMods by tasks.registering(Copy::class) {
    from(developmentMods)
    into(layout.projectDirectory.dir("run/mods"))
}
tasks.named("runClient") { dependsOn(installDevelopmentMods) }

configurations.configureEach {
    // OneConfig's Ornithe renderer uses Pylon/LWJGL 3.
    exclude(group = "org.lwjgl.lwjgl")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}
kotlin {
    jvmToolchain(25)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("oneconfig_version", oneconfigVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version, "oneconfig_version" to oneconfigVersion)
    }
}
