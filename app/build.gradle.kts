import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.r2dbc:r2dbc-postgresql:0.8.2.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.8.2.RELEASE")
}

val bootJar = tasks.withType<BootJar> {
    archiveBaseName.set("studyintonation-analytics")
    archiveVersion.set(project.version as String)
    mainClassName = "org.studyintonation.analytics.api.AnalyticsApp"
}
