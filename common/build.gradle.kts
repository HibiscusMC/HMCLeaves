//import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "1.9.23"
//    id("xyz.jpenilla.run-paper") version "2.2.4"
//    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
//    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
}

group = "com.hibiscusmc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.oraxen.com/releases")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("io.th0rgal:oraxen:1.178.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
//    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.14-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.14-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.47")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
//    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
//    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
//    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
}

kotlin {
    jvmToolchain(21)
}
