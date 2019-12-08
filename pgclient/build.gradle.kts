plugins {
    java
}

dependencies {
    implementation("org.jetbrains:annotations:17.0.0")
    implementation("io.r2dbc:r2dbc-postgresql:0.8.0.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.8.0.RELEASE")
    implementation("com.typesafe:config:1.4.0")
    implementation("io.projectreactor:reactor-core:3.3.1.RELEASE")
    implementation("org.slf4j:slf4j-api:1.7.25")

    compileOnly("org.projectlombok:lombok:1.18.10")
    annotationProcessor("org.projectlombok:lombok:1.18.10")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_13
}
