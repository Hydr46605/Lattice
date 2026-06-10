# API Guide

This guide covers the current Lattice API surface by package and subsystem.

## Modules

| Artifact | Purpose |
| --- | --- |
| `io.github.hydr46605:lattice-api` | Shared API marker artifact used as the stable boundary grows toward 1.0. |
| `io.github.hydr46605:lattice-core` | Platform-neutral runtime, API contracts, and base implementations. |
| `io.github.hydr46605:lattice-paper` | Paper/Folia bootstrap, command registration, scheduler routing, UI rendering, diagnostics, storage helpers, optional integrations, and standalone host. |

Use `compileOnly("io.github.hydr46605:lattice-paper:0.8.5")` for shared-runtime Paper plugins. Use `implementation("io.github.hydr46605:lattice-paper:0.8.5")` only for legacy isolated jars that intentionally shade Lattice.

## Stable Core Packages

| Package | Main Types | Use |
| --- | --- | --- |
| `dev.beryl.lattice` | `Lattice` | Framework metadata. |
| `dev.beryl.lattice.api` | `StableApi`, `ExperimentalApi`, `InternalApi` | Source-visible API status markers. |
| `dev.beryl.lattice.lifecycle` | `LatticeBuilder`, `LatticeRuntime`, `LatticeContext`, `LifecyclePhase`, `StartupReport` | Runtime construction, startup, phase transitions, service access, and shutdown. |
| `dev.beryl.lattice.module` | `LatticeModule`, `ModuleDescriptor`, `ModuleId`, `ModuleDependency`, `ModuleGraph`, `ModuleManager` | Feature module declaration, dependency ordering, cycle checks, and enable/disable ordering. |
| `dev.beryl.lattice.service` | `ServiceRegistry`, `ServiceKey`, `ServiceHandle`, `ServiceScope`, `CloseableService` | Typed service registration and lookup without global singletons. |
| `dev.beryl.lattice.config` | `ConfigService`, `ConfigSpec`, `ConfigHandle`, `ConfigMigration`, `ConfigValidator`, `ReloadResult`, `YamlConfigService` | Typed Configurate YAML loading, defaults, validation, migration, saving, and reload. |
| `dev.beryl.lattice.command` | `CommandService`, `CommandNode`, `CommandContext`, `CommandArgument`, `CommandArgumentParser`, `CommandSuggestionProvider`, `CommandHelpEntry`, `CommandSenderRef`, `CommandUsage` | Backend-neutral commands with permissions, custom parsing, completions, usage/help metadata, aliases, and Paper registration. |
| `dev.beryl.lattice.text` | `TextService`, `MessageBundle`, `MessageKey`, `MiniMessageTemplate`, `LegacyText`, `AudienceRef` | Adventure-first text rendering, MiniMessage, message bundles, and legacy text boundaries. |
| `dev.beryl.lattice.task` | `TaskService`, `TaskOwner`, `TaskContext`, `TaskContextType`, `TaskSchedule`, `TaskHandle`, `RegionRef`, `EntityRef` | Folia-aware async, global, region, and entity scheduling contracts. |
| `dev.beryl.lattice.ui` | `UiService`, `UiScreen`, `UiPage`, `UiButton`, `UiIcon`, `BookViewSurface`, `AnvilTextInputSurface`, `VirtualSignTextInputSurface` | Platform-neutral inventory, book, anvil-input, and virtual-sign UI surfaces. |
| `dev.beryl.lattice.ui.config` | `ConfiguredInventoryScreen`, `ConfiguredInventoryPage`, `ConfiguredInventoryButton`, `ConfiguredUiIcon`, `ConfiguredUiAction`, `ConfiguredUiActionResolver`, `ConfiguredInventoryUiCompiler`, `ConfiguredUiException` | YAML-driven inventory screens compiled into `UiScreen`. |
| `dev.beryl.lattice.storage` | `StorageService`, `StorageConfig`, `JdbcStorageConnection`, `JdbcMigrationRunner`, `SqlMigration`, `JdbcStatementExecutor`, `JdbcTransactionRunner` | SQLite, MySQL, MariaDB, PostgreSQL, migrations, transactions, pool health, and JDBC helpers. |
| `dev.beryl.lattice.integration` | `IntegrationManager`, `IntegrationKey`, `Integration`, `IntegrationStatus`, `Capability`, `SimpleIntegration` | Optional plugin capability registration and lookup. |
| `dev.beryl.lattice.diagnostics` | `DiagnosticService`, `DiagnosticSnapshot`, `DiagnosticFinding`, subsystem diagnostic records | Read-only runtime snapshots for support commands and health checks. |

## Stable Paper Packages

| Package | Main Types | Use |
| --- | --- | --- |
| `dev.beryl.lattice.paper.bootstrap` | `LatticePaperPlugin`, `LatticePaper`, `LatticeHost`, `LatticeHostProvider`, `LatticePluginHandle`, `PaperServices`, `StandaloneLatticeBootstrap`, `StandaloneLatticePlugin` | Paper runtime bootstrap for isolated plugins, shared-host discovery, and the standalone release jar. |
| `dev.beryl.lattice.paper.config` | `PaperConfigPaths` | Paper data-directory path helpers. |
| `dev.beryl.lattice.paper.storage` | `PaperAsyncStorageRunner`, `PaperStorageDefaults` | Async database execution and Paper storage defaults. |
| `dev.beryl.lattice.paper.text` | `PaperAudiences` | Conversion and send helpers for Paper command senders and Adventure audiences. |
| `dev.beryl.lattice.paper.integration` | `PaperIntegrations`, `PlaceholderApiService`, `PlaceholderExpansionSpec`, `PacketEventsService`, `PacketEventsPacketListener`, custom item services | Optional PlaceholderAPI, PacketEvents, Junction, Nexo, Oraxen, ItemsAdder, and CraftEngine bindings. |
| `dev.beryl.lattice.paper.diagnostics` | `PaperDiagnosticRenderer` | Render diagnostics as Adventure components or plain lines. |

Paper command, lifecycle, task, and UI implementation packages are internal. Use the stable core contracts instead of constructing Paper implementation classes directly.

## Shared Runtime Host

In shared-runtime mode, the standalone `Lattice` plugin registers a `LatticeHost` service with Paper. Dependent plugins normally use `LatticePaperPlugin` or `LatticePaper.bootstrap(...)`; both paths discover the host automatically when it is available and fall back to isolated mode only when the plugin is not declaring a hard `Lattice` dependency.

The host owns shared infrastructure such as JDBC pools and aggregate diagnostics. Each dependent plugin still receives its own `LatticeRuntime`, module graph, services, tasks, UI sessions, and diagnostics subtree. When a dependent runtime is disabled, its host handle is detached and runtime services are closed. Host shutdown disables managed plugins in reverse registration order and continues cleanup even if one dependent plugin fails during disable.

Use `LatticeHost`, `LatticeHostProvider`, and `LatticePluginHandle` only for integration code that needs explicit host control. Normal plugin authoring should prefer `LatticePaperPlugin` and let Paper call `load()`, `enable()`, and `disable()` through the plugin lifecycle.

## Runtime Lifecycle

`LatticeBuilder` collects modules and runtime customization. `LatticeRuntime` owns startup and shutdown:

- `load()` registers and prepares modules.
- `enable()` enables modules in dependency order, runs `onReady` hooks, and marks the runtime usable after platform startup.
- `disable()` disables modules in reverse order, cancels tasks, and closes services. Calling `disable()` before `load()` still closes registered services.

Use `LatticeContext` inside modules to access typed services:

```java
TextService text = context.require(LatticeRuntime.TEXT_SERVICE);
CommandService commands = context.require(LatticeRuntime.COMMAND_SERVICE);
```

Default service keys on `LatticeRuntime`:

- `TEXT_SERVICE`
- `CONFIG_SERVICE`
- `COMMAND_SERVICE`
- `COMMAND_EXCEPTION_MAPPER`
- `TASK_SERVICE`
- `STORAGE_SERVICE`
- `INTEGRATION_SERVICE`
- `HOOK_SERVICE`
- `UI_SERVICE`
- `DIAGNOSTIC_SERVICE`

## Modules And Services

`LatticeModule` has a `ModuleDescriptor` and lifecycle hooks. Use `ModuleDependency.required(...)` or optional dependencies when a module must start after another module.

```java
@Override
public ModuleDescriptor descriptor() {
    return ModuleDescriptor.builder("shops")
            .dependency(ModuleDependency.required(ModuleId.of("economy")))
            .build();
}
```

Use `ServiceKey<T>` for plugin services:

```java
public static final ServiceKey<UserRepository> USERS =
        ServiceKey.named(UserRepository.class, "example.users");

context.services().register(USERS, repository, ServiceScope.MODULE);
UserRepository users = context.require(USERS);
```

Services that implement `AutoCloseable` or `CloseableService` are closed during runtime shutdown when registered through the service registry.

### Service Customization

`LatticeBuilder#service(key, service)` is strict registration and fails if a service already exists for that key.

Use typed helpers such as `textService`, `configService`, `commandService`, `taskService`, `storageService`, `integrationService`, `hookService`, `uiService`, and `diagnosticService` when you want to provide a default only if the runtime has not already supplied one.

Use `replaceService(key, service)` when replacement is intentional:

```java
builder.replaceService(LatticeRuntime.COMMAND_SERVICE, commands);
```

Paper plugins that only need custom command failure text should prefer `commandExceptionMapper(...)`:

```java
builder.commandExceptionMapper((throwable, context) ->
        Component.text("That command could not be completed."));
```

## Config

Lattice config is typed and backed by Configurate YAML.

```java
ConfigHandle<MyConfig> handle = context.require(LatticeRuntime.CONFIG_SERVICE)
        .load(ConfigSpec.builder(MyConfig.class, dataDirectory.resolve("config.yml"))
                .schemaVersion(1)
                .defaults(MyConfig::defaults)
                .validator(config -> config.enabled()
                        ? List.of()
                        : List.of("Plugin is disabled"))
                .build());
```

Use `ConfigMigration<T>` for schema upgrades, `ConfigHandle#reload()` for owner-triggered reloads, and `ReloadResult` to report success or validation errors.

When Junction is installed before the plugin loads, the Paper bootstrap installs a template-aware YAML service. Config files can contain variables such as `{{server}}`; unresolved variables fail loading unless a fallback is provided with `{{missing|fallback}}`.

## Commands

Declare commands with `CommandNode`; Paper registration is handled by the adapter.

```java
commands.register(CommandNode.command("example")
        .description("Example command")
        .permission("example.command")
        .child(CommandNode.command("reload")
                .permission("example.reload")
                .executor(ctx -> {
                    config.reload();
                    ctx.replyPlain("Reloaded.");
                })
                .build())
        .build());
```

Command permissions are cumulative along the resolved command path.

Use argument builders when a command needs custom parsing, completions, or a greedy final string:

```java
commands.register(CommandNode.command("example")
        .child(CommandNode.command("mode")
                .argument(CommandArgument.argument("value", Mode.class)
                        .parser(input -> Mode.valueOf(input.toUpperCase(Locale.ROOT)))
                        .suggestions(CommandSuggestionProvider.choices("safe", "strict", "silent"))
                        .required()
                        .build())
                .build())
        .child(CommandNode.command("broadcast")
                .argument(CommandArgument.greedyString("message").required().build())
                .build())
        .build());
```

Command failures use `CommandExceptionMapper`. The default mapper handles permission, usage, parse, and unexpected executor failures with concise player-facing text while Paper logs contextual details.

Use `CommandExceptionMappers.defaultMapper()` if you build a command registrar manually. Use `LatticeBuilder#commandExceptionMapper(...)` in Paper bootstrap code when you want plugin-owned wording.

`CommandUsage.help(root)` is the recommended source for plugin-owned help and diagnostics output. It returns a flattened command tree with usage strings, descriptions, aliases, depth, and permissions. Lattice does not register a global command for you.

## Text

Adventure `Component` is the message boundary. MiniMessage is the preferred owner-editable format.

```java
TextService text = context.require(LatticeRuntime.TEXT_SERVICE);
Component title = text.miniMessage("<green>Example</green>");
Component legacy = text.legacy("&aMigrated text");
```

Use `MessageBundle`, `MessageKey`, and `MessageRenderer` when a plugin has reusable message catalogs. Use `LegacyText` only at migration or compatibility boundaries.

## Tasks

Lattice makes Folia context explicit:

```java
TaskService tasks = context.require(LatticeRuntime.TASK_SERVICE);
TaskOwner owner = new TaskOwner(context.runtimeId(), ModuleId.of("example"));

TaskHandle handle = tasks.runRepeating(
        owner,
        TaskContext.async(),
        TaskSchedule.seconds(5),
        () -> refreshCache()
);
```

Use async tasks for blocking or external work. Use entity or region contexts for player/world operations that must run on the correct Paper/Folia scheduler.

## UI

Declare UI in core types and let `lattice-paper` render it.

```java
UiService ui = context.require(LatticeRuntime.UI_SERVICE);
UiOwner owner = new UiOwner(context.runtimeId(), ModuleId.of("example"));

UiScreen screen = UiScreen.screen("menu", text.miniMessage("<green>Menu</green>"))
        .rows(3)
        .page(UiPage.page("main")
                .button(UiButton.display(13, UiIcon.material("diamond")
                        .name(text.miniMessage("<aqua>Status</aqua>"))))
                .build())
        .build();

ui.open(owner, UiViewerRef.player(player.getUniqueId(), player.getName(), player.getWorld().getName()), screen);
```

Available surfaces:

- `UiScreen` for inventory menus.
- `BookViewSurface` for read-only books.
- `AnvilTextInputSurface` for single-line input.
- `VirtualSignTextInputSurface` for four-line Paper virtual sign input.

Use `ConfiguredInventoryUiCompiler` when server owners should edit menus in YAML. Lattice compiles the shape; your plugin owns action semantics.

### Configured Inventory Authoring

Configured inventory files map to `ConfiguredInventoryScreen`, then compile into a normal `UiScreen`:

```yaml
title: "<dark_gray>Profile Menu</dark_gray>"
rows: 3
pages:
  - id: main
    buttons:
      - slot: 11
        icon:
          source: material
          key: player_head
          name: "<aqua>Your Profile</aqua>"
          lore:
            - "<gray>View stats and settings.</gray>"
        actions:
          - type: "profile:open"
            data:
              target: "overview"
      - slot: 15
        icon:
          source: custom
          provider-id: craftengine
          key: "menu:daily_crate"
          amount: 1
          name: "<gold>Daily Crate</gold>"
          fallback:
            source: material
            key: chest
        actions:
          - type: "crate:open"
            data:
              category: "daily"
      - slot: 26
        icon:
          source: material
          key: arrow
          name: "<yellow>Next</yellow>"
        actions:
          - type: "menu:page"
            data:
              page: confirm
  - id: confirm
    buttons:
      - slot: 11
        icon:
          source: material
          key: lime_dye
          name: "<green>Confirm</green>"
        actions:
          - type: "purchase:confirm"
            data:
              offer: "daily_crate"
      - slot: 15
        icon:
          source: material
          key: barrier
          name: "<red>Back</red>"
        actions:
          - type: "menu:page"
            data:
              page: main
```

Lattice compiles the UI shape and the plugin resolves action semantics via `ConfiguredUiActionResolver`. The `actions` list is data for your resolver; if an action type is shared with commands or other entry points, route it through the same plugin service instead of treating it as inventory-only behavior.

Diagnostics are contextual via `ConfiguredUiException`, with paths such as `profile-menu.pages[0].buttons[1].icon.fallback.key` or `profile-menu.pages[1].buttons[0].actions[0]`.

Custom item icons can target Nexo, Oraxen, ItemsAdder, CraftEngine, or any registered custom provider, with a material fallback:

```java
UiIcon icon = UiIcon.nexo("menu_next")
        .fallback(UiIcon.material("arrow"))
        .name(text.miniMessage("<green>Next</green>"));
```

## Storage

Use `StorageService` and `StorageConfig` for JDBC-backed data.

```java
StorageService storage = context.require(LatticeRuntime.STORAGE_SERVICE);

try (StorageConnection connection = storage.connect(StorageConfig.sqlite(dataDirectory.resolve("data.db")))) {
    new JdbcMigrationRunner().run(connection, List.of(SqlMigration.of(
            "create-users",
            1,
            "create table users (id integer primary key autoincrement, name varchar(64) not null)"
    )));

    JdbcStatementExecutor sql = ((JdbcStorageConnection) connection).executor();
    sql.update("save user", "insert into users (name) values (?)",
            statement -> statement.setString(1, name));
}
```

Supported providers:

- `StorageProviderId.SQLITE`
- `StorageProviderId.MYSQL`
- `StorageProviderId.MARIADB`
- `StorageProviderId.POSTGRESQL`

SQLite enables WAL, foreign keys, and busy timeout defaults. Remote database configs should keep secrets out of logs by using `StorageConfig#redactedSummary()`.

On Paper, use `PaperAsyncStorageRunner` to keep blocking JDBC work off region, entity, and global scheduler threads:

```java
PaperAsyncStorageRunner asyncStorage = new PaperAsyncStorageRunner(tasks, owner, plugin.getLogger());
CompletableFuture<UserProfile> profile = asyncStorage.supply("load user", () -> repository.load(uuid));
```

## Integrations

`IntegrationManager` exposes optional capabilities without hard dependencies.

Optional integrations are runtime capabilities. Check `IntegrationManager#status(...)`, `available(...)`, `service(...)`, or `requireService(...)`, and use the typed integration service interfaces instead of depending on Lattice's reflective adapters.

Junction variables are resolved through the `PaperIntegrations.JUNCTION_VARIABLES` capability when Junction is present and exposes the expected API. Lattice does not require your plugin to hard-depend on Junction, but declare an optional dependency if your plugin needs Junction loaded before its config is read.

```java
IntegrationManager integrations = context.require(LatticeRuntime.INTEGRATION_SERVICE);
integrations.ifAvailable(PaperIntegrations.PLACEHOLDER_API, papi -> {
    String expanded = papi.setPlaceholders(player, "Rank: %vault_rank%");
});
```

Stable Paper integration keys:

- `PaperIntegrations.PLACEHOLDER_API`
- `PaperIntegrations.JUNCTION_VARIABLES`
- `PaperIntegrations.PACKET_EVENTS`
- `PaperIntegrations.NEXO_ITEMS`
- `PaperIntegrations.ORAXEN_ITEMS`
- `PaperIntegrations.ITEMSADDER_ITEMS`
- `PaperIntegrations.CRAFTENGINE_ITEMS`
- `PaperIntegrations.CUSTOM_ITEM_REGISTRY`

Register PlaceholderAPI expansions through Lattice when your plugin owns placeholders:

```java
PlaceholderExpansionRegistration registration = integrations.requireService(PaperIntegrations.PLACEHOLDER_API)
        .registerExpansion(PlaceholderExpansionSpec.builder("example")
                .authors(plugin.getPluginMeta().getAuthors())
                .version(plugin.getPluginMeta().getVersion())
                .placeholder("status")
                .handler((player, params) -> "ok")
                .build());
```

Close returned registrations during module shutdown if your module owns them.

PacketEvents access is exposed as a typed bridge without requiring plugin code to touch Lattice internals:

```java
PacketEventsListenerRegistration registration = integrations.requireService(PaperIntegrations.PACKET_EVENTS)
        .registerListener(new PacketEventsPacketListener() {
            @Override
            public void onPacketReceive(PacketEventsPacketEvent event) {
                event.packetType().ifPresent(type -> logger.fine(type.toString()));
            }
        }, PacketEventsListenerPriority.NORMAL);
```

Close returned PacketEvents registrations during module shutdown. `PacketEventsService#apiHandle()` exposes safe access to the raw PacketEvents API for plugins that need a version-specific escape hatch.

## Diagnostics

Diagnostics are read-only snapshots. Lattice does not add a global command; expose diagnostics through your plugin command if useful.

```java
DiagnosticService diagnostics = context.require(LatticeRuntime.DIAGNOSTIC_SERVICE);
commands.register(CommandNode.command("example")
        .child(CommandNode.command("diagnostics")
                .permission("example.diagnostics")
                .executor(command -> PaperDiagnosticRenderer.components(diagnostics.snapshot()).forEach(command::reply))
                .build())
        .build());
```

Snapshots include lifecycle, startup report, modules, services, integrations with details, published hooks, command tree entries, tasks, UI, and storage state. Shared-runtime and isolated storage both report active JDBC health when connections are open.

### Debugging Startup And Config Failures

`LifecycleException` carries optional context for failures during runtime startup and shutdown. Use `runtimeIdOptional()`, `phaseOptional()`, `operationOptional()`, and `moduleIdOptional()` when reporting the failure. Some failures may leave fields empty, especially compatibility-created exceptions or non-module failures.

```java
try {
    runtime.enable();
} catch (LifecycleException exception) {
    logger.warning("Lattice startup failed"
            + exception.runtimeIdOptional().map(value -> " runtime=" + value).orElse("")
            + exception.phaseOptional().map(value -> " phase=" + value).orElse("")
            + exception.operationOptional().map(value -> " operation=" + value).orElse("")
            + exception.moduleIdOptional().map(value -> " module=" + value).orElse("")
            + ": " + exception.getMessage());
    throw exception;
}
```

`ConfigException` carries optional path and operation context. This lets reload commands and startup logs report whether the failure came from `load`, `parse`, `render`, `defaults`, `deserialize`, `validate`, `migrate`, `serialize`, or `save` without parsing the exception message.

```java
try {
    config.reload();
} catch (ConfigException exception) {
    logger.warning("Config reload failed"
            + exception.pathOptional().map(path -> " path=" + path).orElse("")
            + exception.operationOptional().map(operation -> " operation=" + operation).orElse("")
            + ": " + exception.getMessage());
}
```

Lifecycle diagnostics include startup events and the most recent failed operation. When startup fails, the lifecycle snapshot exposes the last failed operation, module id, and message when those values are available, so a plugin-owned diagnostics command can show useful context without requiring server owners to search the full console log.

## Experimental APIs

### Hooks

`dev.beryl.lattice.hook` provides typed extension channels between plugins. The provider owns the contract interface and consumers publish implementations with `HookPriority`.

```java
public interface ExampleHook {
    Component decorate(UUID playerId, Component current);
}

public static final HookKey<ExampleHook> EXAMPLE_HOOK =
        new HookKey<>("example.decorate", ExampleHook.class);

context.require(LatticeRuntime.HOOK_SERVICE)
        .publish(EXAMPLE_HOOK, hook, HookPriority.NORMAL);
```

This API is experimental because cross-plugin classloading and load-order contracts need deliberate plugin design.

### Templates

`dev.beryl.lattice.template` and `dev.beryl.lattice.template.annotation` provide early generation and binding helpers:

- `PluginTemplate`
- `ModuleTemplate`
- `TemplateCatalog`
- `TemplateRenderer`
- `BraceTemplateRenderer`
- `TemplateVariables`
- `@LatticePlugin`
- `@LatticeModuleSpec`
- `@LatticeCommandSpec`
- `@LatticeConfig`
- `@LatticeIntegrationSpec`
- `@LatticeListener`

Prefer explicit modules and service registration for production code until templates are promoted from experimental.

## Internal APIs

Do not depend on `dev.beryl.lattice.util` or Paper implementation packages such as `dev.beryl.lattice.paper.command`, `paper.task`, `paper.ui`, and `paper.lifecycle`. They exist so stable core contracts can run on Paper/Folia.
