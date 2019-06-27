import java.net.URI

group = "com.github.cs125-illinois"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

plugins {
    kotlin("jvm")
}

dependencies {
    val ktorVersion = "1.2.1"
    implementation(project(":jeedrunner"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
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