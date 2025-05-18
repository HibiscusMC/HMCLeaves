plugins {
    id("java")
}

group = "com.hibiscusmc"
version = "3.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    compileOnly("com.github.retrooper:packetevents-api:2.7.0")
    compileOnly("com.google.code.gson:gson:2.13.1")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}