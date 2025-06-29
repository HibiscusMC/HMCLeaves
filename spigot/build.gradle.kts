plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.hibiscusmc.hmcleaves.spigot"
version = "3.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.nexomc.com/releases")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.nexomc:nexo:0.7.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation(project(":nms"))
    implementation(project(path = ":v1_20", configuration = "reobf"))
    implementation(project(path = ":v1_20_3", configuration = "reobf"))
    implementation(project(path = ":v1_20_6", configuration = "reobf"))
    implementation(project(path = ":v1_21_4", configuration = "reobf"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        options.compilerArgs.add("-parameters")
    }

    shadowJar {
        mergeServiceFiles()
        relocate("co.aikar.commands", "com.hibiscusmc.hmcleaves.paper.acf")
        relocate("co.aikar.locales", "com.hibiscusmc.hmcleaves.paper.locales")

        archiveFileName.set("hmcleaves-${project.version}.jar")

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

tasks {
    runServer {
        minecraftVersion("1.21.6")
    }
}
