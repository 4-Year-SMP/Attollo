rootProject.name = "Attollo"
includeBuild("build-logic")
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "jmp repository"
            url = uri("https://repo.jpenilla.xyz/snapshots")
        }
    }
}
