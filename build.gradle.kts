/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.github.goooler.shadow") version "8.1.8"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
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
    implementation(project(path = ":v1_20_4", configuration = "reobf"))
    implementation(project(path = ":v1_19", configuration = "reobf"))
    implementation(project(path = ":v1_21", configuration = "reobf"))
    implementation(project(path = ":common"))
}

kotlin {
    jvmToolchain(21)
}

tasks {

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        mergeServiceFiles()

        archiveFileName.set("hmcleaves-${project.version}.jar")
        relocate("com.github.retrooper.packetevents", "com.hibiscusmc.hmcleaves.packetevents")
        relocate("com.github.benmanes.caffeine", "com.hibiscusmc.hmcleaves.caffeine")
        relocate("net.kyori.adventure.text.serializer.legacy", "com.hibiscusmc.hmcleaves.adventure.text.serializer.legacy")

        dependencies {
            exclude(dependency("org.yaml:snakeyaml"))
        }
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }
}

bukkit {
    // Default values can be overridden if needed
    name = "HMCLeaves"
    version = "${getVersion()}"
    description = "HMCLeaves"
    author = "Fisher2911"

    // Plugin main class (required)
    main = "com.hibiscusmc.hmcleaves.HMCLeaves"


    apiVersion = "1.18"

    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    prefix = "HMCLeaves"

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "WorldEdit",
        "FastAsyncWorldEdit"
    )

    commands {
        register("hmcleaves") {
            description = "HMCleaves command"
            aliases = listOf("leaves")
            permission = "hmcleaves.command"
            usage = "/hmcleaves ..."
        }
    }
}