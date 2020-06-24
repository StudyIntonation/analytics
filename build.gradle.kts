import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java

    id("org.springframework.boot") version "2.3.1.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.11"
}

group = "org.studyintonation.analytics"
version = "1.0-SNAPSHOT"

val byteBuddyPlugin: Configuration by configurations.creating

repositories {
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    compileOnly("io.projectreactor:reactor-tools:3.3.5.RELEASE")
    byteBuddyPlugin(group = "io.projectreactor", name = "reactor-tools", classifier = "original")

    implementation("io.r2dbc:r2dbc-postgresql")
    implementation("io.r2dbc:r2dbc-pool")

    implementation("org.jetbrains:annotations:19.0.0")
    implementation("com.typesafe:config:1.4.0")

    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
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

byteBuddy {
    transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
        plugin = "reactor.tools.agent.ReactorDebugByteBuddyPlugin"
        setClassPath(byteBuddyPlugin)
    })
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}
