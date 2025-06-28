plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "com.hibiscuscmc.hmcleaves.v1_20"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    compileOnly(project(":nms"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        options.compilerArgs.add("-parameters")
    }
}