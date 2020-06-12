import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.72"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.jmailen.kotlinter") version "2.4.0" apply false
    id("com.github.sherter.google-java-format") version "0.9"
    id("org.jetbrains.dokka") version "0.10.1" apply false
    id("com.github.ben-manes.versions") version "0.28.0"
    id("io.gitlab.arturbosch.detekt") version "1.9.1"
}
allprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
    }
    tasks.withType<KotlinCompile> {
        val javaVersion = JavaVersion.VERSION_1_8.toString()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        kotlinOptions {
            jvmTarget = javaVersion
        }
    }
}
subprojects {
    tasks.withType<Test> {
        enableAssertions = true
    }
}
tasks.dependencyUpdates {
    resolutionStrategy {
        componentSelection {
            all {
                if (listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap", "release").any { qualifier ->
                    candidate.version.matches(Regex("(?i).*[.-]$qualifier[.\\d-+]*"))
                }) {
                    reject("Release candidate")
                }
            }
        }
    }
    gradleReleaseChannel = "current"
}
detekt {
    val ignoredProjects = setOf("demo")
    input = files(*subprojects.map { it.name }.minus(ignoredProjects).map { "$it/src/main/kotlin" }.toTypedArray())
    config = files("config/detekt/detekt.yml")
}
tasks.register("check") {
    dependsOn("detekt")
}
