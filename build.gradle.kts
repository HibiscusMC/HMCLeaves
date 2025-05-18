plugins {
    id("java")
}

group = "com.hibiscusmc"
version = "3.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
