import java.net.URI

plugins {
    kotlin("jvm") version "1.3.31"
}

group = "com.github.cs125-illinois"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    val ktorVersion = "1.2.1"
    implementation(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("com.github.cs125-illinois:jeed:master-SNAPSHOT") { isChanging = true }
    implementation("io.github.cdimascio:java-dotenv:5.0.1")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}
tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
}