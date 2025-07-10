plugins {
    id("java")
    application
}

version = "1.0-SNAPSHOT"

application {
    mainClass = "Main"
}

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.logging.log4j:log4j-api:2.18.0")
    implementation("org.apache.logging.log4j:log4j-core:2.23.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}