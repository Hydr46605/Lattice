import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    base
    id("com.modrinth.minotaur") version "2.9.0" apply false
}

allprojects {
    group = "dev.beryl"
    version = providers.gradleProperty("latticeVersion").get()
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set(
                        when (project.name) {
                            "lattice-api" -> "Stable Lattice authoring contracts and API status markers."
                            "lattice-core" -> "Platform-neutral Lattice APIs and runtime services."
                            "lattice-paper" -> "Paper and Folia adapter APIs for Lattice-powered plugins."
                            else -> "Lattice framework artifact."
                        }
                    )
                    url.set("https://github.com/Hydr46605/Lattice")
                    scm {
                        connection.set("scm:git:https://github.com/Hydr46605/Lattice.git")
                        developerConnection.set("scm:git:https://github.com/Hydr46605/Lattice.git")
                        url.set("https://github.com/Hydr46605/Lattice")
                    }
                    developers {
                        developer {
                            id.set("BerylLabs")
                            name.set("BerylLabs")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/hydr46605/Lattice")
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orNull
                    password = providers.gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orNull
                }
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
    }

    tasks.withType<Jar>().configureEach {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.14.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
