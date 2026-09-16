plugins {
    // Android (applied only to :app)
    // id("com.android.application") version "8.7.3" apply false
    // kotlin("android") version "2.0.21" apply false

    // JVM targets (core + desktop-app)
    kotlin("jvm") version "2.0.21" apply false

    // JetBrains Compose Desktop
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Serialization (settings persistence)
    kotlin("plugin.serialization") version "2.0.21" apply false

    // Static analysis & formatting (Quality Gate: ./gradlew ktlintCheck detekt)
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}
