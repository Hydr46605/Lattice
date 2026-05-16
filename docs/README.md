# Lattice Developer Guide

Lattice is a Java 21 framework for building structured Paper/Folia plugins. It is designed to be used as a library from your plugin, with `lattice-paper` providing the Paper adapter and `lattice-core` providing the platform-neutral API.

## Guide

- [Getting Started](getting-started.md) - install from GitHub Packages, create a Lattice-powered Paper plugin, and register your first module.
- [API Guide](api-guide.md) - lifecycle, modules, services, config, commands, text, tasks, UI, storage, integrations, hooks, templates, diagnostics, and Paper adapter APIs.
- [Build And Release](build-and-release.md) - local build commands, GitHub Packages publishing, GitHub Releases, and Modrinth release workflow usage.

## Coordinates

Most Paper plugins should depend on `lattice-paper`; it brings `lattice-core` transitively.

```kotlin
dependencies {
    implementation("dev.beryl:lattice-paper:0.7.2")
}
```

Use `lattice-core` directly only for platform-neutral libraries or tests that intentionally avoid Paper APIs.

```kotlin
dependencies {
    implementation("dev.beryl:lattice-core:0.7.2")
}
```

GitHub Packages requires authentication for Gradle consumption. See [Getting Started](getting-started.md#install-from-github-packages) for the complete repository block.
