plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.0.0"
}

group = "hu.ogyh"
version = "0.0.1-SNAPSHOT"
description = "Voting API"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// A Boot 4.1.1 által hozott H2 2.4.240 hibásan értékeli ki a CHECK megkötést, ha a táblát létrehozó
// kapcsolat már lezárult (pl. a pool lecserélte) – minden mentés "Check constraint invalid" hibát ad.
// A 2.5.252 javítja; eltávolítható, ha a Boot ennél újabb H2-t hoz.
extra["h2.version"] = "2.5.252"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ugyanazt az image-et építi a Dockerfile-ból, mint a compose (voting-api:latest)
tasks.register<Exec>("dockerBuild") {
    group = "build"
    description = "Builds the voting-api:latest Docker image from the Dockerfile."
    commandLine("docker", "build", "-t", "voting-api:latest", ".")
}

// a build elbukik formázatlan kódon (spotlessCheck a check része); javítás: ./gradlew spotlessApply
spotless {
    java {
        palantirJavaFormat("2.71.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
