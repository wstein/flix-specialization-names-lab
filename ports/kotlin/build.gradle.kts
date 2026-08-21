plugins {
    kotlin("jvm") version "2.4.10"
    application
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Section 13's near-native mapping for channels/select/par-yield -- see
    // docs/ports.md for why this is the one construct not approximated.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("dev.wstein.flixlab.allconstructs.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
