plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.2"
}

group = "net.oryn.mc"
version = "1.1.0"
description = "A Main Plugin For Oryn Server"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("com.github.luben:zstd-jni:1.5.7-11")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "orynplugins"
            version = project.version.toString()

            artifact(tasks.shadowJar)

            pom {
                name.set("OrynPlugins")
                description.set("A Main Plugin For Oryn Server")
                url.set("https://github.com/Fahry-a/OrynPlugins")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("Fahry-a")
                        name.set("Fahry-a")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Fahry-a/OrynPlugins.git")
                    developerConnection.set("scm:git:ssh://github.com/Fahry-a/OrynPlugins.git")
                    url.set("https://github.com/Fahry-a/OrynPlugins")
                }
            }
        }
    }

    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
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