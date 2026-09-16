pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "pilcrow"

// Original Android app (kept for reference / dual-target builds)
// include(":app")

// Shared pure-Kotlin domain logic (no platform dependencies)
include(":core")

// New Linux desktop app (Compose Desktop)
include(":desktop-app")
