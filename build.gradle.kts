import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java

    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

group = "org.studyintonation.analytics"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.r2dbc:r2dbc-postgresql:0.8.2.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.8.2.RELEASE")

    implementation("org.jetbrains:annotations:19.0.0")
    implementation("com.typesafe:config:1.4.0")

    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_14
}

val bootJar = tasks.withType<BootJar> {
    archiveBaseName.set("studyintonation-analytics")
    archiveVersion.set(project.version as String)
    mainClassName = "org.studyintonation.analytics.api.AnalyticsApp"
}

tasks.register("stage") {
    dependsOn(":clean", ":bootJar")
}
