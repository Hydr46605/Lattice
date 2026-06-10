import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":lattice-api"))
    api(platform("net.kyori:adventure-bom:4.26.1"))
    api("net.kyori:adventure-api")
    api("net.kyori:adventure-text-minimessage")
    api("net.kyori:adventure-text-serializer-legacy")
    api("net.kyori:adventure-text-serializer-plain")
    api("org.spongepowered:configurate-core:4.2.0")
    api("org.spongepowered:configurate-yaml:4.2.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
    runtimeOnly("org.xerial:sqlite-jdbc:3.53.1.0")
    runtimeOnly("com.mysql:mysql-connector-j:9.7.0")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.8")
    runtimeOnly("org.postgresql:postgresql:42.7.11")
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val generatedVersionSourceDirectory = layout.buildDirectory.dir("generated/sources/latticeVersion/java")
val generatedVersionSource = generatedVersionSourceDirectory.map {
    it.file("dev/beryl/lattice/LatticeBuildVersion.java")
}

val generateLatticeBuildVersion by tasks.registering {
    inputs.property("version", project.version.toString())
    outputs.file(generatedVersionSource)

    doLast {
        val output = generatedVersionSource.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """
            package dev.beryl.lattice;

            final class LatticeBuildVersion {
                static final String VALUE = "${project.version}";

                private LatticeBuildVersion() {
                }
            }
            """.trimIndent() + "\n"
        )
    }
}

sourceSets.named("main") {
    java.srcDir(generatedVersionSourceDirectory)
}

tasks.named("compileJava") {
    dependsOn(generateLatticeBuildVersion)
}
