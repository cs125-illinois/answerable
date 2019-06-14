repositories {
    jcenter()
}
buildscript {
    repositories {
        jcenter()
    }
}

allprojects {
    ext {
        set("kotlinVersion", "1.3.31")
    }
}