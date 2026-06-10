<p align="center">
  <img src="docs/assets/lattice-logo.png" alt="Lattice logo" height="220">
</p>

<h1 align="center">Lattice</h1>

<p align="center">
  <strong>A developer framework for structured, Folia-aware Paper plugins on Java 21.</strong>
</p>

<p align="center">
  Lattice standardizes lifecycle, modules, services, configuration, commands, text, tasks, UI, storage, diagnostics, and optional integrations so Minecraft plugins can stay small, explicit, and production-ready.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Paper 1.21.11" src="https://img.shields.io/badge/Paper-1.21.11-2ea44f">
  <img alt="Version" src="https://img.shields.io/badge/version-0.8.5-blue">
  <img alt="Status" src="https://img.shields.io/badge/API-pre--1.0_beta-yellow">
</p>

---

## Links

- [Developer Guide](docs/README.md)
- [Getting Started](docs/getting-started.md)
- [API Guide](docs/api-guide.md)
- [Build And Release](docs/build-and-release.md)
- [GitHub Releases](https://github.com/Hydr46605/Lattice/releases)
- [Modrinth](https://modrinth.com/project/Ed95iBPq)

## What Is Lattice?

Lattice is the shared foundation for BerylStudios and BerylLabs Minecraft plugins. It is not a gameplay plugin and it does not add commands, features, worlds, or mechanics by itself. Plugin authors can install Lattice as a standalone Paper/Folia host for shared runtime infrastructure, or use the legacy isolated library mode when a self-contained jar is required.

The framework gives plugin projects a clear structure from the first class:

- lifecycle phases with startup reports and failure rollback
- explicit feature modules with dependency ordering and cycle checks
- typed service registration without reflection-heavy dependency injection
- Configurate YAML loading with defaults, validation, schema versions, migrations, and optional Junction variable rendering
- backend-neutral command declarations with permissions, custom parsing, completions, usage/help metadata, aliases, and Paper registration
- Adventure and MiniMessage-first text rendering with legacy input boundaries
- Folia-aware task contexts for async, global, region, and entity work
- platform-neutral UI surfaces for inventories, books, anvil input, and virtual sign input
- JDBC storage helpers for SQLite, MySQL, MariaDB, and PostgreSQL through HikariCP
- diagnostics snapshots for lifecycle, modules, services, integrations, hooks, command trees, tasks, UI, and storage
- optional Paper integrations for PlaceholderAPI, PacketEvents, Junction, Nexo, Oraxen, ItemsAdder, and CraftEngine

## Modules

| Module | Purpose |
| --- | --- |
| `lattice-api` | Minimal API status markers and future shared-runtime contracts. |
| `lattice-core` | Platform-neutral contracts and base implementations under `dev.beryl.lattice`. |
| `lattice-paper` | Paper/Folia bootstrap, scheduler, command, UI, diagnostics, storage, hook, and integration adapters. |

`lattice-core` intentionally contains no Bukkit, Paper, or Folia classes. Server-specific behavior belongs in `lattice-paper`, keeping core APIs testable and stable for plugin authors.

## Plugins That Use Lattice

These public plugins use Lattice as a general plugin framework rather than as gameplay-specific or private implementation surface:

| Plugin | Repository | Uses Lattice For |
| --- | --- | --- |
| Tabular | https://github.com/Hydr46605/Tabular | Tablists, scoreboards, nametags, templates, animations, command/config structure, and optional PlaceholderAPI/PacketEvents integration status. |
| Junction | https://github.com/Hydr46605/Junction | Safe config preprocessing, variable rendering, managed templates, backups, reports, and Lattice config-loading integration for dependent plugins. |
| Onboard | https://github.com/Hydr46605/Onboard | First-join onboarding flows, configured inventory screens, storage, auth-aware lifecycle gates, commands, and optional custom item integrations. |

## Compatibility

- Java 21
- Modern Paper `1.21.x`, currently targeting `1.21.11`
- Folia-aware scheduling through explicit task contexts
- Adventure components and MiniMessage as the primary text format
- Pre-1.0 API status: stable authoring paths exist, but breaking changes can still happen during the `0.x` hardening line

## Quick Start

Install the standalone `Lattice` plugin on the server and compile your plugin against the Paper adapter from Maven Central:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.github.hydr46605:lattice-paper:0.8.5")
}
```

Use the checked-in Gradle wrapper to build Lattice itself:

```bash
./gradlew build
```

Minimal Paper plugin shape:

```java
public final class ExamplePlugin extends LatticePaperPlugin {
    @Override
    protected void configure(LatticeBuilder builder) {
        builder.module(new ExampleModule(getDataFolder().toPath()));
    }
}
```

Modules own their config, commands, tasks, storage, UI, and service registrations. See the [Getting Started](docs/getting-started.md) guide and [API Guide](docs/api-guide.md) for the full direct API path.

## Current Status

Lattice is a pre-1.0 beta framework. The preferred 0.8 deployment model is the standalone shared runtime: the server installs the `Lattice` plugin, dependent plugins compile against Lattice without relocation, and Paper loads the shared classes through an explicit dependency.

Release builds also attach a standalone Paper/Folia jar for distribution channels that need a server-installable artifact.

Legacy shaded library usage remains supported for compatibility. In that mode each dependent plugin owns its own Lattice runtime, module graph, service registry, task tracking, storage handles, and diagnostics surface.
