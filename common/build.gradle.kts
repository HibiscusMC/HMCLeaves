import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "1.9.23"
    id("xyz.jpenilla.run-paper") version "2.2.4"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

group = "com.hibiscusmc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    implementation("com.github.retrooper.packetevents:spigot:2.2.1")
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

tasks {

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveFileName.set("hmcleaves-${project.version}.jar")
        relocate("com.github.retrooper.packetevents", "com.hibiscusmc.hmcleaves.packetevents")
        relocate("com.github.benmanes.caffeine", "com.hibiscusmc.hmcleaves.caffeine")

//        minimize()

        dependencies {
            exclude(dependency("org.yaml:snakeyaml"))
        }
    }

    runServer {
        minecraftVersion("1.20.4")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

bukkit {
    // Default values can be overridden if needed
     name = "HMCLeaves"
     version = "${getVersion()}"
     description = "HMCLeaves"
     author = "Fisher2911"

    // Plugin main class (required)
    main = "com.hibiscusmc.hmcleaves.HMCLeaves"


    apiVersion = "1.17"

    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    prefix = "HMCLeaves"

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot"
        )

    commands {
        register("hmcleaves") {
            description = "HMCleaves command"
            aliases = listOf("leaves")
            permission = "hmcleaves.command"
            usage = "/hmcleaves ..."
        }
        // ...
    }
}