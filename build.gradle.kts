plugins {
    id("java")
}

group = "net.tsuzuito"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.google.code.gson:gson:2.10.1")

    // YouTube Data API v3
    implementation("com.google.apis:google-api-services-youtube:v3-rev20261019-2.9.2")
// Google OAuth 2.0
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
// Google HTTP Client
    implementation("com.google.http-client:google-http-client-gson:2.0.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
}

// Create start.bat automatically
val createBat = tasks.register("createBat") {
    dependsOn(tasks.jar)

    doLast {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val batFile = jarFile.resolveSibling("start.bat")

        batFile.writeText(
            """
            @echo off
            cd /d "%~dp0"
            java -jar "${jarFile.name}"
            pause
            """.trimIndent()
        )

        println("Created: ${batFile.absolutePath}")
    }
}

tasks.named("build") {
    dependsOn(createBat)
}