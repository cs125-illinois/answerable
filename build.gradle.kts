repositories {
    jcenter()
}

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    kotlin("jvm") version "1.3.61" apply false
}

allprojects {
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}
