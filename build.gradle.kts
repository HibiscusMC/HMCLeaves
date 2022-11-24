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
version = "1.1.0-beta"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://jitpack.io")
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.0.0")
    implementation("com.github.retrooper.packetevents:spigot:2.0-SNAPSHOT")
    implementation("com.jeff_media:CustomBlockData:2.0.1")
    implementation("com.jeff_media:MorePersistentDataTypes:2.3.1")
    implementation("org.bstats:bstats-bukkit:3.0.0")
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
        relocate("com.github.retrooper.packetevents", "io.github.fisher2911.hmcleaves.packetevents.spigot")
        relocate("com.jeff_media.customblockdata", "io.github.fisher2911.hmcleaves.customblockdata")
        relocate("com.jeff_media.morepersistentdatatypes", "io.github.fisher2911.hmcleaves.morepersistentdatatypes")
        relocate("org.bstats", "io.github.fisher2911.hmcleaves.bstats")
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
