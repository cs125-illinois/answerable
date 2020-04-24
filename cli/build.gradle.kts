import java.util.Properties
import java.io.StringWriter
import java.io.File

group = "com.github.cs125-illinois"
version = "2020.4.0"

plugins {
    kotlin("jvm")
    application
    id("org.jmailen.kotlinter")
}
dependencies {
    implementation(project(":core"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("com.github.ajalt:clikt:2.6.0")
}
val mainClass = "edu.illinois.cs.cs125.answerable.cli.MainKt"
application {
    applicationName = "answerable"
    mainClassName = mainClass
}
tasks.test {
    useJUnitPlatform()
    systemProperties["logback.configurationFile"] = File(projectDir, "src/test/resources/logback-test.xml").absolutePath
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = mainClass
    }
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
