plugins {
    id("java")
    id("application")
}

group = "org.bsnelson"
version = "1.0-SNAPSHOT"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    implementation("ch.qos.logback:logback-core:1.5.7")
    implementation("ch.qos.logback:logback-classic:1.5.7")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.json:json:20240303")

//    implementation("org.wiremock:wiremock:3.9.1")
    implementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

application {
    // Define the main class for your application
    mainClass.set("org.bsnelson.ShadesMockServerApp")
}