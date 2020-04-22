plugins {
    java
}

allprojects {
    group = "org.studyintonation.analytics"
    version = "1.0-SNAPSHOT"
}

configure(subprojects) {
    repositories {
        jcenter()
    }

    apply(plugin = "java")

    dependencies {
        implementation("org.jetbrains:annotations:19.0.0")
        implementation("com.typesafe:config:1.4.0")

        val lombok = "org.projectlombok:lombok:1.18.12"
        compileOnly(lombok)
        annotationProcessor(lombok)
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_14
    }
}

tasks.register("stage") {
    dependsOn(":clean", "api:bootJar")
}
