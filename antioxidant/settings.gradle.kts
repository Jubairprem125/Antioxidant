pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

include(":core")
include(":client")
include(":datagen")

rootProject.name = "antioxidant-parent"
