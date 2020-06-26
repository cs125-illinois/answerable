import java.io.File
import java.io.StringWriter
import java.util.Properties

plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
    java
    checkstyle
    `maven-publish`
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.github.classgraph:classgraph:4.8.86")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
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
        create<MavenPublication>("cored") {
            from(components["java"])
        }
    }
}
