group = "com.github.cs125-illinois"
version = "2020.4.0"

plugins {
    kotlin("jvm")
    application
    id("org.jmailen.kotlinter")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    api(project(":core"))

    // Jeed and dependencies
    api("com.github.cs125-illinois:jeed:2020.6.1")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

application {
    mainClassName = "edu.illinois.cs.cs125.answerable.jeedrunner.MainKt"
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
