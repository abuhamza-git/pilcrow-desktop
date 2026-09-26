plugins {
    // JVM targets (core + desktop-app)
    kotlin("jvm") version "2.0.21" apply false

    // JetBrains Compose Desktop
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Serialization (settings persistence)
    kotlin("plugin.serialization") version "2.0.21" apply false

}
