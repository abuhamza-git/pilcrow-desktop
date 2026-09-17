import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // PDF Export
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10")
    implementation("org.commonmark:commonmark-ext-heading-anchor:0.24.0")

    // Markdown parsing
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:0.24.0")
    implementation("org.commonmark:commonmark-ext-task-list-items:0.24.0")
    implementation("org.commonmark:commonmark-ext-yaml-front-matter:0.24.0")
    implementation("org.commonmark:commonmark-ext-autolink:0.24.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // JSON for settings
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

compose.desktop {
    application {
        mainClass = "com.pilcrowmd.desktop.MainKt"
        nativeDistributions {
            packageName = "PilcrowMD"
            packageVersion = "1.0.6"
            description = "A beautiful Markdown reader & editor"
            modules("java.instrument", "jdk.unsupported")

            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                appCategory = "TextEditor"
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "PilcrowMD"
            }
            macOS {
                
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}
