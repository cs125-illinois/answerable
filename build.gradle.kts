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
        mavenLocal()
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}
subprojects {
    group = "com.github.cs125-illinois.answerable"
    version = "2020.6.5"
    tasks.withType<KotlinCompile> {
        val javaVersion = JavaVersion.VERSION_1_8.toString()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        kotlinOptions {
            jvmTarget = javaVersion
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
        enableAssertions = true
        if (JavaVersion.current() >= JavaVersion.VERSION_11) {
            jvmArgs("-ea", "-Xmx1G", "-Xss256k", "--enable-preview")
        } else {
            jvmArgs("-ea", "-Xmx1G", "-Xss256k")
        }
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
    input = files(
        "cli/src/main/kotlin", "core/src/main/kotlin", "jeedrunner/src/main/kotlin"
    )
    buildUponDefaultConfig = true
}
tasks.register("check") {
    dependsOn("detekt")
}
