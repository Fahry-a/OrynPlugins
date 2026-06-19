# OrynPlugins

Module host plugin for Oryn server. Loads and manages modules from `plugins/OrynPlugins/modules/`.

## Features

- **Module system** — Load JAR modules from `modules/` folder
- **Auto-scan** — Automatically discovers and loads module JARs
- **Isolated classloaders** — Each module runs in its own classloader
- **Command routing** — Modules register commands under `/oryn module <name>`
- **Module management** — Enable, disable, reload modules at runtime
- **Per-module logger** — Each module gets a prefixed logger
- **Config utility** — Standardized config management via `ModuleConfigManager`
- **Thread safety** — ConcurrentHashMap-based module storage
- **Maven repository** — Published to `https://maven.oryn.my.id`

## Requirements

- Paper 1.21.1+ (or compatible fork)
- Java 21+

## Installation

1. Download `OrynPlugins-1.0.1.jar` from [Releases](https://github.com/Fahry-a/OrynPlugins/releases)
2. Place in `plugins/` folder
3. Start server

## Commands

| Command | Description |
|---------|-------------|
| `/oryn module list` | List all loaded modules with status |
| `/oryn module info <name>` | Show module details (author, deps, status) |
| `/oryn module enable <name>` | Enable a module |
| `/oryn module disable <name>` | Disable a module |
| `/oryn module reload <name>` | Reload a module (calls onReload) |
| `/oryn modules <name> <args>` | Execute module command |
| `/oryn help` | Show help |

## Permissions

| Permission | Description |
|------------|-------------|
| `oryn.admin` | Access to OrynPlugins commands (default: op) |

## Module System

Modules are JAR files placed in `plugins/OrynPlugins/modules/`. Each module:

- Implements `OrynModule` interface
- Has `Main-Class` in `MANIFEST.MF`
- Runs in isolated classloader (parent = OrynPlugins)
- Has its own data folder (`modules/<name>/`)
- Gets a per-module prefixed logger

### Module Status

| Status | Description |
|--------|-------------|
| `LOADED` | Module loaded but not yet enabled |
| `ENABLED` | Module is running |
| `DISABLED` | Module was disabled |
| `ERRORED` | Module failed to load or enable |

### Available Modules

| Module | Description | Repository |
|--------|-------------|------------|
| [Oryn-Tunnelv2](https://github.com/Fahry-a/Oryn-Tunnelv2) | Cloudflare Tunnel management | [Maven](https://maven.oryn.my.id) |

### Installing Modules

```bash
# Copy module JAR to modules folder
cp MyModule-1.0.jar plugins/OrynPlugins/modules/

# Or install from Maven repository
# Add to build.gradle.kts:
# implementation("net.oryn.mc:mymodule:1.0.0")
```

### Creating Modules

See [MODULE-DEVELOPMENT.md](MODULE-DEVELOPMENT.md) for full guide.

```kotlin
// build.gradle.kts
repositories {
    maven("https://maven.oryn.my.id")
}

dependencies {
    compileOnly("net.oryn.mc:orynplugins:1.1.0")
}
```

```java
@ModuleInfo(
    name = "mymodule",
    version = "1.0.0",
    description = "My awesome module",
    author = "YourName"
)
public class MyModule implements OrynModule {
    @Override
    public boolean onLoad(ModuleContext context) {
        context.getLogger().info("MyModule loaded!");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        sender.sendMessage("Hello from MyModule!");
        return true;
    }
}
```

## Maven Repository

Published artifacts are available at `https://maven.oryn.my.id`.

### Usage

**Gradle (Kotlin DSL):**

```kotlin
repositories {
    maven("https://maven.oryn.my.id")
}

dependencies {
    compileOnly("net.oryn.mc:orynplugins:1.1.0")
}
```

**Maven (pom.xml):**

```xml
<repository>
    <id>oryn-maven</id>
    <url>https://maven.oryn.my.id</url>
</repository>

<dependency>
    <groupId>net.oryn.mc</groupId>
    <artifactId>orynplugins</artifactId>
    <version>1.1.0</version>
    <scope>provided</scope>
</dependency>
```

## Architecture

```
Paper Server
  └── OrynPlugins (JavaPlugin)
        ├── OrynCommand (/oryn)
        └── ModuleLoader
              ├── TunnelModule (module JAR) [ENABLED]
              ├── AnotherModule (module JAR) [DISABLED]
              └── ...
```

### ClassLoader Hierarchy

```
Bootstrap ClassLoader (JDK)
    ↑
Application ClassLoader (CLASSPATH)
    ↑
Server ClassLoader (Minecraft, Paper, Bukkit)
    ↑
OrynPlugins ClassLoader (host plugin)
    ↑
Module ClassLoader (module JAR)
```

## Building

```bash
./gradlew build
```

Output: `build/libs/OrynPlugins-1.1.0.jar`

## Publishing

```bash
# Publish to local repo
./gradlew publish

# Publish to Maven repository (via GitHub Actions)
git push origin main
```

## Changelog

### v1.1.0
- Added `@ModuleInfo` annotation for metadata (alternative to getter methods)
- Enhanced `ModuleContext` with scheduler helpers (runTask, runTaskAsync, etc.)
- Enhanced `ModuleContext` with event helpers (registerEvents, unregisterEvents)
- Enhanced `ModuleContext` with player utilities (getPlayer, getOnlinePlayers)
- Added `onReload()` method to `OrynModule` interface
- All `OrynModule` methods now have default implementations (except `onLoad`)
- Command structure change: `/oryn modules <name> <args>` for module commands
- Management commands remain: `/oryn module list|info|enable|disable|reload`

### v1.0.2
- Changed command structure: `/oryn modules <name>` for module commands
- Management commands: `/oryn module list|info|enable|disable|reload`

### v1.0.1
- Fixed `plugin.yml` description placeholder
- Fixed `onCommand()` return value handling
- Fixed tab completion label passing
- Fixed classloader leak on load failure
- Added `ConcurrentHashMap` for thread safety
- Added module name collision detection
- Added `ModuleStatus` enum (LOADED, ENABLED, DISABLED, ERRORED)
- Added `getAuthor()`, `getDependencies()`, `getSoftDependencies()` to `OrynModule`
- Changed `onLoad()` to return `boolean` (false = abort loading)
- Added per-module logger with `[<name>]` prefix
- Added `ModuleConfigManager` utility
- Added `/oryn module info/enable/disable/reload` commands

### v1.0.0
- Initial release

## License

MIT License
