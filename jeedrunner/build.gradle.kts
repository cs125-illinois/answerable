import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40"
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(project(":core"))
    api("com.github.cs125-illinois:jeed:master-SNAPSHOT") { isChanging = true }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}