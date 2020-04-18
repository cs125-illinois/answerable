repositories {
    jcenter()
}

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    kotlin("jvm") version "1.3.70" apply false
}

allprojects {
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}
