# OrynPlugins

Module host plugin for Oryn server. Loads and manages modules from `plugins/OrynPlugins/modules/`.

## Features

- **Module system** — Load JAR modules from `modules/` folder
- **Auto-scan** — Automatically discovers and loads module JARs
- **Isolated classloaders** — Each module runs in its own classloader
- **Command routing** — Modules register commands under `/oryn module <name>`
- **Maven repository** — Published to `https://maven.oryn.my.id`

## Requirements

- Paper 1.21.1+ (or compatible fork)
- Java 21+

## Installation

1. Download `OrynPlugins-1.0.0.jar` from [Releases](https://github.com/Fahry-a/OrynPlugins/releases)
2. Place in `plugins/` folder
3. Start server

## Commands

| Command | Description |
|---------|-------------|
| `/oryn module list` | List all loaded modules |
| `/oryn module <name> <args>` | Execute module command |
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
    compileOnly("net.oryn.mc:orynplugins:1.0.0")
}
```

```java
public class MyModule implements OrynModule {
    @Override
    public String getName() { return "mymodule"; }

    @Override
    public void onLoad(ModuleContext context) {
        context.getLogger().info("MyModule loaded!");
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
    compileOnly("net.oryn.mc:orynplugins:1.0.0")
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
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

## Architecture

```
Paper Server
  └── OrynPlugins (JavaPlugin)
        ├── OrynCommand (/oryn)
        └── ModuleLoader
              ├── TunnelModule (module JAR)
              ├── AnotherModule (module JAR)
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

Output: `build/libs/OrynPlugins-1.0.0.jar`

## Publishing

```bash
# Publish to local repo
./gradlew publish

# Publish to Maven repository (via GitHub Actions)
git push origin main
```

## License

MIT License
