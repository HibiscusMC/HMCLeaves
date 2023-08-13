plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.1"
}

group = "io.github.fisher2911"
version = "1.1.0"

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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.18.2-R0.1-SNAPSHOT")
    implementation(project(":common"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}