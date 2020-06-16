import java.io.File
import java.io.StringWriter
import java.util.Properties

plugins {
    kotlin("jvm")
    java
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("org.jmailen.kotlinter")
    `maven-publish`
}
dependencies {
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.javassist:javassist:3.27.0-GA")
    implementation("org.apache.bcel:bcel:6.5.0")
    implementation("org.junit.jupiter:junit-jupiter:5.6.2")
    implementation("org.objenesis:objenesis:3.1")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.github.classgraph:classgraph:4.8.86")

    testImplementation("com.marcinmoskala:DiscreteMathToolkit:1.0.3")
    testImplementation("com.github.cs125-illinois.jeed:core:2020.6.2")
}
tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}
tasks.compileKotlin {
    dependsOn("createProperties")
}
task("createProperties") {
    dependsOn(tasks.processResources)
    doLast {
        val properties = Properties().also {
            it["version"] = project.version.toString()
        }
        File(projectDir, "src/main/resources/edu.illinois.cs.cs125.answerable.core.version")
        .printWriter().use { printWriter ->
            printWriter.print(
                StringWriter().also { properties.store(it, null) }.buffer.toString()
                    .lines().drop(1).joinToString(separator = "\n").trim()
            )
        }
    }
}
publishing {
    publications {
        create<MavenPublication>("core") {
            from(components["java"])
        }
    }
}
