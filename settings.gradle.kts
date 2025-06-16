rootProject.name = "HMCLeaves"
include("common")
include("spigot")

include("v1_20_6")
include("v1_20_3")
include("v1_20")
include("nms")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}