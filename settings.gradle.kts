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

rootProject.name = "HMCLeaves"
include(
    "common",
    "v1_20_3",
    "v1_20",
    "v1_19_4",
    "v1_19_3",
    "v1_19",
    "v1_18_2",
    "v1_18"
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/central")
        maven("https://jitpack.io")
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://mvn.lumine.io/repository/maven-public/") { metadataSources { artifact() } }
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.hibiscusmc.com/snapshots")
    }
}
