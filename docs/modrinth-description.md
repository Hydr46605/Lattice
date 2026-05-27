<p align="center">
  <img src="https://raw.githubusercontent.com/Hydr46605/Lattice/main/docs/assets/lattice-logo.png" alt="Lattice" width="180">
</p>

<h1 align="center">Lattice</h1>

<p align="center">
  <strong>A developer framework for structured, Folia-aware Paper plugins on Java 21.</strong>
</p>

<p align="center">
  Lattice standardizes lifecycle, modules, services, configuration, commands, text, tasks, UI, storage, diagnostics, and optional integrations so Minecraft plugins can stay small, explicit, and production-ready.
</p>

---

## What Is Lattice?

Lattice is the shared foundation for BerylStudios and BerylLabs Minecraft plugins. It is not a gameplay plugin and it does not add commands, worlds, mechanics, or player-facing features by itself.

Plugin authors use Lattice to build Paper/Folia plugins with a consistent runtime shape: explicit modules, predictable lifecycle phases, typed services, validated configuration, Folia-aware task routing, storage helpers, and diagnostics that are useful when something breaks in production.

---

## Deployment Model

Since `0.8.0`, the preferred deployment model is the standalone shared runtime:

| Mode | Use When | Runtime Behavior |
| --- | --- | --- |
| Standalone shared runtime | New Paper/Folia plugins that can depend on Lattice being installed on the server | The server installs the `Lattice` plugin once. Dependent plugins compile against Lattice without relocation and join the Lattice classpath through an explicit Paper dependency. |
| Legacy isolated library mode | Self-contained plugin jars or compatibility paths that still need shading | Each dependent plugin owns its own Lattice runtime, module graph, service registry, task tracking, storage handles, and diagnostics surface. |

The standalone runtime is the recommended path for new work because it avoids repeating shared infrastructure across every dependent plugin on the same server.

---

## What It Provides

| Area | Built-in Support |
| --- | --- |
| Lifecycle | Load, enable, ready, disable, startup reports, and failure rollback |
| Modules | Explicit feature modules with dependency ordering and cycle detection |
| Services | Typed service registry without reflection-heavy dependency injection |
| Config | Configurate YAML, defaults, validation, schema versions, migrations, and optional Junction variable rendering |
| Commands | Backend-neutral command trees with Paper registration |
| Text | Adventure components, MiniMessage, and legacy input boundaries |
| Tasks | Folia-aware async, global, region, and entity scheduling |
| UI | Inventory menus, books, anvil input, and virtual sign input |
| Storage | SQLite, MySQL, MariaDB, PostgreSQL, HikariCP, migrations, JDBC helpers, and shared pool management in standalone mode |
| Diagnostics | Runtime snapshots for lifecycle, modules, services, commands, tasks, UI, storage, and integrations |
| Integrations | PlaceholderAPI, PacketEvents, Junction, Nexo, Oraxen, ItemsAdder, and CraftEngine |

---

## Modules

Lattice is split into three Maven artifacts:

| Artifact | Purpose |
| --- | --- |
| `io.github.hydr46605:lattice-api` | Minimal API status markers and the first shared-runtime contract boundary. |
| `io.github.hydr46605:lattice-core` | Platform-neutral contracts and base implementations under `dev.beryl.lattice`. |
| `io.github.hydr46605:lattice-paper` | Paper/Folia bootstrap, scheduler, command, UI, diagnostics, storage, hook, integration, and standalone host adapters. |

`lattice-core` intentionally contains no Bukkit, Paper, or Folia classes. Server-specific behavior belongs in `lattice-paper`, keeping core APIs testable and stable for plugin authors.

---

## Compatibility

- Java 21
- Modern Paper `1.21.x`, currently targeting `1.21.11`
- Folia-aware scheduling through explicit task contexts
- Adventure components and MiniMessage as the primary text format
- Pre-1.0 API status while the framework is hardened against real plugin integrations

---

## Current Status

Lattice is in the `0.x` beta line. Stable authoring paths exist, but breaking changes can still happen before `1.0`.

The current focus is the shared-runtime architecture: one installed Lattice host, dependent plugins that register into that host cleanly, shared JDBC pool management, clearer diagnostics, and a smaller public API surface for long-term plugin authoring.
