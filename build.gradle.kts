import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.Sign

plugins {
    base
    id("com.modrinth.minotaur") version "2.9.0" apply false
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

allprojects {
    group = "dev.beryl"
    version = providers.gradleProperty("latticeVersion").get()
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.vanniktech.maven.publish")

    val signingConfigured = providers.gradleProperty("signingInMemoryKey").isPresent
            || providers.gradleProperty("signing.secretKeyRingFile").isPresent

    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        configure(
            JavaLibrary(
                javadocJar = JavadocJar.Javadoc(),
                sourcesJar = SourcesJar.Sources(),
            )
        )
        coordinates("dev.beryl", project.name, project.version.toString())
        publishToMavenCentral(
            automaticRelease = true,
            validateDeployment = DeploymentValidation.PUBLISHED,
        )
        signAllPublications()
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
            inceptionYear.set("2026")
            url.set("https://github.com/Hydr46605/Lattice")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("BerylLabs")
                    name.set("BerylLabs")
                    url.set("https://github.com/Hydr46605")
                }
            }
            scm {
                url.set("https://github.com/Hydr46605/Lattice")
                connection.set("scm:git:https://github.com/Hydr46605/Lattice.git")
                developerConnection.set("scm:git:https://github.com/Hydr46605/Lattice.git")
            }
        }
    }

    extensions.configure<PublishingExtension> {
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

    tasks.withType<Sign>().configureEach {
        onlyIf("signing credentials are configured") {
            signingConfigured
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.14.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
