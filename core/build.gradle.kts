plugins {
    kotlin("jvm")
}

dependencies {
    // Markdown parsing
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:0.24.0")
    implementation("org.commonmark:commonmark-ext-task-list-items:0.24.0")
    implementation("org.commonmark:commonmark-ext-heading-anchor:0.24.0")
    implementation("org.commonmark:commonmark-ext-yaml-front-matter:0.24.0")
    implementation("org.commonmark:commonmark-ext-autolink:0.24.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // Testing
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
