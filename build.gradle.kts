import groovy.util.Node
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
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

plugins {
    base
    id("com.modrinth.minotaur") version "2.9.0" apply false
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

allprojects {
    group = "io.github.hydr46605"
    version = providers.gradleProperty("latticeVersion").get()
}

val publishGitHubPackagesMirror by tasks.registering {
    group = "publishing"
    description = "Publishes the legacy dev.beryl Maven coordinates to the GitHub Packages mirror."
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "signing")
    apply(plugin = "com.vanniktech.maven.publish")

    val signingInMemoryKey = providers.gradleProperty("signingInMemoryKey")
    val signingInMemoryKeyId = providers.gradleProperty("signingInMemoryKeyId")
    val signingInMemoryKeyPassword = providers.gradleProperty("signingInMemoryKeyPassword")
    val signingConfigured = signingInMemoryKey.isPresent
            || providers.gradleProperty("signing.secretKeyRingFile").isPresent

    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        configure(
            JavaLibrary(
                javadocJar = JavadocJar.Javadoc(),
                sourcesJar = SourcesJar.Sources(),
            )
        )
        coordinates("io.github.hydr46605", project.name, project.version.toString())
        publishToMavenCentral(
            automaticRelease = true,
            validateDeployment = DeploymentValidation.PUBLISHED,
        )
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

    extensions.configure<SigningExtension>("signing") {
        isRequired = signingConfigured
        if (signingInMemoryKey.isPresent) {
            useInMemoryPgpKeys(
                signingInMemoryKeyId.orNull,
                signingInMemoryKey.get(),
                signingInMemoryKeyPassword.orNull,
            )
        }
    }

    extensions.configure<PublishingExtension> {
        publications.matching { it.name == "maven" }.all {
            extensions.configure<SigningExtension>("signing") {
                sign(this@all)
            }
        }

        publications {
            create<MavenPublication>("githubPackages") {
                groupId = "dev.beryl"
                artifactId = project.name
                version = project.version.toString()
                artifact(tasks.named("jar"))
                artifact(tasks.named("sourcesJar"))
                artifact(tasks.named("plainJavadocJar"))
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
                    withXml {
                        val root = asNode()
                        if (project.name == "lattice-core") {
                            root.appendDependencyManagementBom("net.kyori", "adventure-bom", "4.26.1")
                            root.appendDependencies {
                                dependency("dev.beryl", "lattice-api", project.version.toString())
                                dependency("net.kyori", "adventure-api")
                                dependency("net.kyori", "adventure-text-minimessage")
                                dependency("net.kyori", "adventure-text-serializer-legacy")
                                dependency("net.kyori", "adventure-text-serializer-plain")
                                dependency("org.spongepowered", "configurate-core", "4.2.0")
                                dependency("org.spongepowered", "configurate-yaml", "4.2.0")
                                dependency("com.zaxxer", "HikariCP", "7.0.2", "runtime")
                                dependency("org.xerial", "sqlite-jdbc", "3.53.1.0", "runtime")
                                dependency("com.mysql", "mysql-connector-j", "9.7.0", "runtime")
                                dependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.8", "runtime")
                                dependency("org.postgresql", "postgresql", "42.7.11", "runtime")
                            }
                        }
                        if (project.name == "lattice-paper") {
                            root.appendDependencies {
                                dependency("dev.beryl", "lattice-core", project.version.toString())
                            }
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

    tasks.withType<Sign>().configureEach {
        onlyIf("signing credentials are configured") {
            signingConfigured
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf("publication belongs to this repository target") {
            when (repository.name) {
                "mavenCentral" -> publication.name == "maven"
                "GitHubPackages" -> publication.name == "githubPackages"
                else -> true
            }
        }
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = !name.contains("GithubPackages")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("lattice.test.projectVersion", project.version.toString())
    }

    rootProject.tasks.named("publishGitHubPackagesMirror") {
        dependsOn(tasks.named("publishGithubPackagesPublicationToGitHubPackagesRepository"))
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.14.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}

private fun Node.appendDependencyManagementBom(groupId: String, artifactId: String, version: String) {
    val dependencyManagement = appendNode("dependencyManagement")
    val dependencies = dependencyManagement.appendNode("dependencies")
    val dependency = dependencies.appendNode("dependency")
    dependency.appendNode("groupId", groupId)
    dependency.appendNode("artifactId", artifactId)
    dependency.appendNode("version", version)
    dependency.appendNode("type", "pom")
    dependency.appendNode("scope", "import")
}

private fun Node.appendDependencies(block: PomDependencies.() -> Unit) {
    PomDependencies(appendNode("dependencies")).block()
}

private class PomDependencies(private val dependencies: Node) {
    fun dependency(groupId: String, artifactId: String, version: String? = null, scope: String = "compile") {
        val dependency = dependencies.appendNode("dependency")
        dependency.appendNode("groupId", groupId)
        dependency.appendNode("artifactId", artifactId)
        if (version != null) {
            dependency.appendNode("version", version)
        }
        dependency.appendNode("scope", scope)
    }
}
