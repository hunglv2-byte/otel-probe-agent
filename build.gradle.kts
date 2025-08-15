
plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("net.bytebuddy:byte-buddy:1.14.14")
    implementation("net.bytebuddy:byte-buddy-agent:1.14.14")
    implementation("io.opentelemetry:opentelemetry-api:1.41.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.41.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "org.example.Main",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
    // Create a fat jar with dependencies
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
