# Lattice Developer Guide

Lattice is a Java 21 framework for building structured Paper/Folia plugins. The preferred 0.8 deployment model is a standalone shared runtime where the installed `Lattice` plugin owns common infrastructure for dependent plugins. The legacy isolated library model remains available for self-contained jars.

## Guide

- [Getting Started](getting-started.md) - install from Maven Central, create a Lattice-powered Paper plugin, and register your first module.
- [API Guide](api-guide.md) - lifecycle, modules, services, config, commands, text, tasks, UI, storage, integrations, hooks, templates, diagnostics, and Paper adapter APIs.
- [Build And Release](build-and-release.md) - local build commands, Maven Central publishing, GitHub Packages mirroring, GitHub Releases, and Modrinth release workflow usage.

## Coordinates

For shared-runtime mode, install the standalone `Lattice` plugin on the server, compile dependent plugins with `compileOnly("io.github.hydr46605:lattice-paper:0.8.3")`, and declare a hard Paper dependency with `join-classpath: true`.

```kotlin
dependencies {
    compileOnly("io.github.hydr46605:lattice-paper:0.8.3")
}
```

Use `lattice-core` directly only for platform-neutral libraries or tests that intentionally avoid Paper APIs.

```kotlin
dependencies {
    implementation("io.github.hydr46605:lattice-core:0.8.3")
}
```

Maven Central is the primary public Maven source under `io.github.hydr46605`. GitHub Packages remains available as an authenticated `dev.beryl` mirror for maintainers and existing fallback builds.

For legacy isolated mode, shade `io.github.hydr46605:lattice-paper:0.8.3` into the dependent plugin and do not expect shared pools or aggregate diagnostics.
