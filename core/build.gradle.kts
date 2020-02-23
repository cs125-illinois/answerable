buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:0.21.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
    }
}

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.3.61"
    id("org.jetbrains.dokka") version "0.9.18"
    id("com.github.ben-manes.versions") version "0.21.0"
}

repositories {
    jcenter()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.javassist:javassist:3.25.0-GA")
    implementation("org.apache.bcel:bcel:6.4.1")
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")
    implementation("org.objenesis:objenesis:3.0.1")
    implementation("com.google.code.gson:gson:2.8.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    implementation(kotlin("reflect"))
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}