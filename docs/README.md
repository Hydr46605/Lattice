# Lattice Developer Guide

Lattice is a Java 21 framework for building structured Paper/Folia plugins. The preferred 0.8 deployment model is a standalone shared runtime where the installed `Lattice` plugin owns common infrastructure for dependent plugins. The legacy isolated library model remains available for self-contained jars.

## Guide

- [Getting Started](getting-started.md) - install from GitHub Packages, create a Lattice-powered Paper plugin, and register your first module.
- [API Guide](api-guide.md) - lifecycle, modules, services, config, commands, text, tasks, UI, storage, integrations, hooks, templates, diagnostics, and Paper adapter APIs.
- [Build And Release](build-and-release.md) - local build commands, GitHub Packages publishing, GitHub Releases, and Modrinth release workflow usage.

## Coordinates

For shared-runtime mode, install the standalone `Lattice` plugin on the server, compile dependent plugins with `compileOnly("dev.beryl:lattice-paper:0.8.0")`, and declare a hard Paper dependency with `join-classpath: true`.

```kotlin
dependencies {
    compileOnly("dev.beryl:lattice-paper:0.8.0")
}
```

Use `lattice-core` directly only for platform-neutral libraries or tests that intentionally avoid Paper APIs.

```kotlin
dependencies {
    implementation("dev.beryl:lattice-core:0.8.0")
}
```

GitHub Packages requires authentication for Gradle consumption. See [Getting Started](getting-started.md#install-from-github-packages) for the complete repository block.

For legacy isolated mode, shade `dev.beryl:lattice-paper:0.8.0` into the dependent plugin and do not expect shared pools or aggregate diagnostics.
