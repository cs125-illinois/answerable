import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm")
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
    api("com.github.cs125-illinois:jeed:master-SNAPSHOT") { isChanging = true }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}