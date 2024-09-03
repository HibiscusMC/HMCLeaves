plugins {
    kotlin("jvm") version "1.9.23"
    id("io.papermc.paperweight.userdev") version "1.7.1"
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
    paperweight.paperDevBundle("1.19-R0.1-SNAPSHOT")
    pluginRemapper("net.fabricmc:tiny-remapper:0.10.4:fat")
    compileOnly("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT")
    implementation(project(":common"))
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

kotlin {
    jvmToolchain(21)
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}