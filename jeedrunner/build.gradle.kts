import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm")
    application
}

application {
    mainClassName = "edu.illinois.cs.cs125.answerable.MainKt"
}

version = "SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")
    api(project(":core"))

    // Jeed and dependencies
    api("com.github.cs125-illinois:jeed:answerable_stable-SNAPSHOT") { isChanging = true }
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}