plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.2"
}

group = "net.oryn.mc"
version = "1.2.0"
description = "A Main Plugin For Oryn Server"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

publishing {
    repositories {
        maven {
            name = "orynRepo"
            url = uri("https://maven.oryn.my.id/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "orynplugins"
            version = project.version.toString()
            artifact(tasks.shadowJar)
        }
    }
}

tasks {

    runServer {
        minecraftVersion("1.21.1")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "description" to project.description
        )

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    publish {
        dependsOn(shadowJar)
    }
}
