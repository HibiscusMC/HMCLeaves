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

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "io.github.fisher2911"
version = "2.4.1-beta"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.0.0")
    compileOnly("org.xerial:sqlite-jdbc:3.39.2.0")
    implementation("com.zaxxer:HikariCP:3.3.0")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.14-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.14-SNAPSHOT")
    implementation(platform("com.intellectualsites.bom:bom-1.18.x:1.19"))
//    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
//    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    implementation("com.github.retrooper.packetevents:spigot:2.0.0-SNAPSHOT")
    //
//    implementation("com.github.retrooper.packetevents:spigot:23e0861192d5803bb72878a491833cbc7c6243a2")

//    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    shadowJar {
        relocate("com.github.retrooper.packetevents", "io.github.fisher2911.hmcleaves.packetevents.api")
        relocate("io.github.retrooper.packetevents", "io.github.fisher2911.hmcleaves.packetevents.impl")
        relocate("net.kyori", "io.github.fisher2911.hmcleaves.packetevents.kyori")
        relocate("org.bstats", "io.github.fisher2911.hmcleaves.bstats")
        relocate("com.zaxxer.hikari", "io.github.fisher2911.hmcleaves.hikari")

        archiveFileName.set("HMCLeaves-${version}.jar")

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
