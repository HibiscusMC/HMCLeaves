plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.papermc.paperweight.userdev") version "1.3.10-SNAPSHOT"
}

group = "io.github.fisher2911"
version = "1.0.0-beta"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://jitpack.io")
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.0.0")
    implementation("com.jeff_media:CustomBlockData:2.0.1")
    implementation("com.jeff_media:MorePersistentDataTypes:2.3.1")
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")

}

tasks {

    build {
        dependsOn(shadowJar)
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    shadowJar {
        relocate("com.jeff_media.customblockdata", "io.github.fisher2911.hmcleaves.customblockdata")
        relocate("com.jeff_media.morepersistentdatatypes", "io.github.fisher2911.hmcleaves.morepersistentdatatypes")

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
