plugins {
    kotlin("jvm")
    application
    id("org.jmailen.kotlinter")
    `maven-publish`
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    api(project(":core"))
    api("com.github.cs125-illinois.jeed:core:2020.6.2")
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
}
publishing {
    publications {
        create<MavenPublication>("jeedrunner") {
            from(components["java"])
        }
    }
}