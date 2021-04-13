import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java

    id("org.springframework.boot") version "2.4.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "org.studyintonation.analytics"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.projectreactor:reactor-tools")

    implementation("io.r2dbc:r2dbc-postgresql")
    implementation("io.r2dbc:r2dbc-pool")

    implementation("org.jetbrains:annotations:19.0.0")
    implementation("com.typesafe:config:1.4.0")

    val lombok = "org.projectlombok:lombok:1.18.20"
    compileOnly(lombok)
    annotationProcessor(lombok)

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_16
}

val bootJar = tasks.withType<BootJar> {
    archiveBaseName.set("studyintonation-analytics")
    archiveVersion.set(project.version as String)
    mainClassName = "org.studyintonation.analytics.api.AnalyticsApp"
}

tasks.register("stage") {
    dependsOn(":clean", ":bootJar")
}
