import java.util.Properties
import java.io.StringWriter
import java.io.File

plugins {
    kotlin("jvm")
    application
    id("org.jmailen.kotlinter")
}
dependencies {
    implementation(project(":jeedrunner"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.10")
    implementation("com.github.ajalt:clikt:2.7.1")
}
application {
    applicationName = "answerable"
    mainClassName = "edu.illinois.cs.cs125.answerable.cli.MainKt"
}
task("createProperties") {
    doLast {
        val properties = Properties().also {
            it["version"] = project.version.toString()
        }
        File(
            projectDir,
            "src/main/resources/edu.illinois.cs.cs125.answerable.cli.version"
        )
            .printWriter().use { printWriter ->
                printWriter.print(
                    StringWriter().also { properties.store(it, null) }.buffer.toString()
                        .lines().drop(1).joinToString(separator = "\n").trim()
                )
            }
    }
}
tasks.processResources {
    dependsOn("createProperties")
}
