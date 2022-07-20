plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "io.github.fisher2911"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    implementation("com.github.retrooper.packetevents:spigot:2.0-SNAPSHOT")
    implementation("com.jeff_media:CustomBlockData:2.0.1")
    implementation("com.jeff_media:MorePersistentDataTypes:2.3.1")
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
        archiveFileName.set("HMCLeaves.jar")

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
