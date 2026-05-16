import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.modrinth.minotaur")
}

dependencies {
    api(project(":lattice-core"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.2")
    testCompileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testCompileOnly("me.clip:placeholderapi:2.12.2")
    testRuntimeOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testRuntimeOnly("me.clip:placeholderapi:2.12.2")
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val runtimeClasspath = configurations.runtimeClasspath
val standalonePluginDescriptor = layout.buildDirectory.file("generated/standalone/paper-plugin.yml")
val releaseNotes = rootProject.layout.projectDirectory.file("docs/release-notes/${project.version}.md")

val generateStandalonePluginDescriptor by tasks.registering {
    inputs.property("version", project.version.toString())
    outputs.file(standalonePluginDescriptor)
    doLast {
        val descriptor = standalonePluginDescriptor.get().asFile
        descriptor.parentFile.mkdirs()
        descriptor.writeText(
            """
            name: Lattice
            version: ${project.version}
            main: dev.beryl.lattice.paper.bootstrap.StandaloneLatticePlugin
            api-version: '1.21'
            folia-supported: true
            description: A Java 21 foundation for structured Paper/Folia plugin development.
            authors:
              - BerylLabs
            """.trimIndent()
                + "\n"
        )
    }
}

val standaloneJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Builds the standalone Paper plugin jar used for Modrinth releases."
    archiveClassifier.set("standalone")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(generateStandalonePluginDescriptor)
    dependsOn(runtimeClasspath)
    from(sourceSets.named("main").map { it.output })
    from(standalonePluginDescriptor)
    from({
        runtimeClasspath.get()
                .filter { it.name.endsWith(".jar") }
                .map { zipTree(it) }
    })
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set(providers.environmentVariable("MODRINTH_PROJECT_ID"))
    versionNumber.set(project.version.toString())
    versionName.set("Lattice ${project.version}")
    changelog.set(providers.fileContents(releaseNotes).asText)
    versionType.set(providers.environmentVariable("MODRINTH_VERSION_TYPE").orElse("beta"))
    uploadFile.set(standaloneJar)
    gameVersions.addAll("1.21.11")
    loaders.add("paper")
    detectLoaders.set(false)
}

tasks.named("modrinth") {
    inputs.file(releaseNotes)
    dependsOn(standaloneJar)
}
