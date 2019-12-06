allprojects {
    group = "org.studyintonation.analytics"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
    }
}

plugins {
    java
}

tasks.register("stage") {
    dependsOn(":clean", "api:bootJar")
}
