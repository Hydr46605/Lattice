# Getting Started

This guide shows the default Lattice authoring path for a Paper/Folia plugin.

## Requirements

- Java 21
- Gradle Kotlin DSL
- Paper `1.21.x`
- A GitHub token with `read:packages` for consuming Lattice from GitHub Packages

## Install From GitHub Packages

Add the GitHub Packages repository. GitHub's Gradle registry requires authentication, including for public packages.

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.pkg.github.com/hydr46605/Lattice") {
        name = "GitHubPackages"
        credentials {
            username = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .get()
            password = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .get()
        }
    }
}
```

Store local credentials outside the project:

```properties
# ~/.gradle/gradle.properties
gpr.user=YourGitHubUsername
gpr.key=YourClassicTokenWithReadPackages
```

For the shared-runtime path, install the standalone `Lattice` plugin in the server `plugins/` folder and compile against Lattice without shading or relocating it:

```kotlin
dependencies {
    compileOnly("dev.beryl:lattice-paper:0.8.0")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
```

Declare a hard Paper dependency on `Lattice` and join its classpath so both plugins see the same Lattice API classes:

```yaml
name: Example
version: 1.0.0
main: com.example.ExamplePlugin
api-version: '1.21'
folia-supported: true
dependencies:
  server:
    Lattice:
      load: BEFORE
      required: true
      join-classpath: true
```

Do not relocate `dev.beryl.lattice` in shared-runtime mode. If a plugin declares a hard `Lattice` dependency but still loads an isolated framework copy, Lattice fails startup with a direct diagnostic instead of continuing with broken type identity.

For legacy isolated mode, depend on the Paper adapter with `implementation("dev.beryl:lattice-paper:0.8.0")` and shade it into your plugin. Paper plugin jars normally need their runtime libraries shaded or otherwise provided. Isolated mode keeps its own runtime, services, storage handles, and diagnostics instead of sharing the standalone host.

## Plugin Entry Point

Extend `LatticePaperPlugin` and register modules in `configure`.

```java
import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.paper.bootstrap.LatticePaperPlugin;

public final class ExamplePlugin extends LatticePaperPlugin {
    @Override
    protected void configure(LatticeBuilder builder) {
        builder.module(new ExampleModule(getDataFolder().toPath()));
    }
}
```

`LatticePaperPlugin` starts the runtime during the Paper plugin lifecycle and closes it during shutdown.

If your plugin already has its own `JavaPlugin` base class, bootstrap manually:

```java
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.paper.bootstrap.LatticePaper;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePlugin extends JavaPlugin {
    private LatticeRuntime runtime;

    @Override
    public void onLoad() {
        runtime = LatticePaper.bootstrap(this, builder -> {
            builder.module(new ExampleModule(getDataFolder().toPath()));
        });
        runtime.load();
    }

    @Override
    public void onEnable() {
        runtime.enable();
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.disable();
        }
    }
}
```

## First Module

Modules are explicit feature units. They own config, commands, tasks, services, storage handles, UI sessions, and integrations for that feature.

```java
import dev.beryl.lattice.command.CommandNode;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.config.ConfigHandle;
import dev.beryl.lattice.config.ConfigSpec;
import dev.beryl.lattice.lifecycle.LatticeContext;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;

import java.nio.file.Path;

public final class ExampleModule implements LatticeModule {
    private final Path dataDirectory;
    private ConfigHandle<ExampleConfig> config;

    public ExampleModule(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public ModuleDescriptor descriptor() {
        return ModuleDescriptor.of("example");
    }

    @Override
    public void onLoad(LatticeContext context) throws Exception {
        config = context.require(LatticeRuntime.CONFIG_SERVICE).load(ConfigSpec.builder(
                        ExampleConfig.class,
                        dataDirectory.resolve("config.yml")
                )
                .schemaVersion(1)
                .defaults(ExampleConfig::defaults)
                .build());

        CommandService commands = context.require(LatticeRuntime.COMMAND_SERVICE);
        commands.register(CommandNode.command("example")
                .permission("example.command")
                .executor(command -> command.replyPlain("ok"))
                .build());
    }
}
```

## API Status

Lattice is pre-1.0. Public packages are annotated so plugin authors can see what is intended for use:

- `@StableApi`: supported authoring surface for the current `0.8.x` line.
- `@ExperimentalApi`: usable, but can change quickly.
- `@InternalApi`: framework implementation detail.

Start with stable lifecycle, module, service, config, command, text, task, UI, storage, integration, diagnostics, and Paper bootstrap APIs. Use hooks and templates only when their experimental status is acceptable for your plugin.
