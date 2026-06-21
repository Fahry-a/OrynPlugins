plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

group = "net.oryn.mc"
version = "1.2.0"
description = "A Main Plugin For Oryn Server"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
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
        minecraftVersion(libs.versions.minecraft.get())
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
