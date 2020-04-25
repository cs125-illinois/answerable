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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    api(project(":core"))

    // Jeed and dependencies
    api("com.github.cs125-illinois:jeed:2020.4.7")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.1")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    if (JavaVersion.current() >= JavaVersion.VERSION_11) {
        jvmArgs("-ea", "-Xmx1G", "-Xss256k", "--enable-preview")
    } else {
        jvmArgs("-ea", "-Xmx1G", "-Xss256k")
    }
    environment["JEED_MAX_THREAD_POOL_SIZE"] = 4
    useJUnitPlatform()
}
