plugins {
    id("java")
}

group = "com.hibiscusmc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    compileOnly("com.github.retrooper:packetevents-api:2.7.0")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}