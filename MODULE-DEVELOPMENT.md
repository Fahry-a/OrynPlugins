# Module Development Guide

Guide untuk membuat module baru untuk OrynPlugins.

## Apa itu Module?

Module adalah JAR plugin yang di-load oleh OrynPlugins dari folder `modules/`. Module berjalan sebagai bagian dari OrynPlugins, bukan sebagai plugin standalone.

**Perbedaan Standalone vs Module:**

| Aspek | Standalone | Module |
|-------|-----------|--------|
| Command | `/otunnel` | `/oryn module tunnel` |
| Config | `plugins/Oryn-Tunnelv2/config.yml` | `plugins/OrynPlugins/modules/tunnel/config.yml` |
| Loading | Bukkit plugin loader | OrynPlugins ModuleLoader |
| Dependencies | Bundled dalam JAR | Menggunakan classloader dari OrynPlugins |
| Publish | GitHub Releases | Maven repository (`maven.oryn.my.id`) |

## Persiapan

### 1. Buat Project Gradle Baru

```kotlin
// build.gradle.kts
plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.2"
}

group = "net.oryn.mc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.oryn.my.id")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.oryn.mc:orynplugins:1.0.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    jar {
        manifest {
            attributes(
                "Main-Class" to "com.example.mymodule.MyModule"
            )
        }
    }

    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}
```

### 2. Implement `OrynModule` Interface

```java
package com.example.mymodule;

import net.oryn.mc.orynPlugins.module.ModuleContext;
import net.oryn.mc.orynPlugins.module.OrynModule;

import org.bukkit.command.CommandSender;

import java.util.List;

public class MyModule implements OrynModule {

    private ModuleContext context;

    @Override
    public String getName() {
        return "mymodule";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "My custom module";
    }

    @Override
    public void onLoad(ModuleContext context) {
        this.context = context;
        context.getLogger().info("MyModule loaded!");
    }

    @Override
    public void onEnable() {
        context.getLogger().info("MyModule enabled!");
    }

    @Override
    public void onDisable() {
        context.getLogger().info("MyModule disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        sender.sendMessage("Hello from MyModule!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return List.of();
    }
}
```

### 3. Build & Install

```bash
./gradlew build
cp build/libs/MyModule-1.0.jar <server>/plugins/OrynPlugins/modules/
```

### 4. Publish ke Maven Repository (Optional)

Jika ingin publish module untuk akses dari project lain:

```bash
./gradlew publish
```

Artifacts akan di-publish ke `https://maven.oryn.my.id`. Lihat [MODULE-DEVELOPMENT.md](../MODULE-DEVELOPMENT.md) untuk detail publish workflow.

## Module API Reference

### `OrynModule` Interface

| Method | Description |
|--------|-------------|
| `getName()` | Nama module (unique, lowercase) |
| `getVersion()` | Version string |
| `getDescription()` | Deskripsi singkat |
| `onLoad(ModuleContext)` | Dipanggil saat module di-load |
| `onEnable()` | Dipanggil saat module di-enable |
| `onDisable()` | Dipanggil saat module di-disable |
| `onCommand(sender, label, args)` | Handle command dari `/oryn module <name>` |
| `onTabComplete(sender, label, args)` | Tab completion untuk command |

### `ModuleContext` Class

| Method | Description |
|--------|-------------|
| `getHostPlugin()` | Instance OrynPlugins (`JavaPlugin`) |
| `getModuleDataFolder()` | Folder data module (`plugins/OrynPlugins/modules/<name>/`) |
| `getLogger()` | Logger untuk module |

## ClassLoader Hierarchy

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

**Yang bisa diakses module:**
- Semua class dari OrynPlugins (parent delegation)
- Semua class Bukkit/Paper API
- Semua class JDK

**Yang TIDAK bisa diakses:**
- Class dari module lain (isolated classloaders)
- Class yang hanya ada di module JAR (tanpa parent delegation)

## Module Lifecycle

```
1. ModuleLoader.scan()
   └── Find JARs in modules/ folder

2. ModuleLoader.loadModule()
   ├── Create URLClassLoader
   ├── Read Main-Class from MANIFEST.MF
   ├── Instantiate class
   ├── Cast to OrynModule
   └── Call onLoad(context)

3. ModuleLoader.enableAllModules()
   └── Call onEnable() for each module

4. [Server Shutdown]

5. ModuleLoader.disableAllModules()
   ├── Call onDisable() for each module
   └── Close classloaders
```

## Config Pattern

Module menggunakan `ConfigManager` pattern untuk config:

```java
public class MyModule implements OrynModule {

    private ConfigManager configManager;

    @Override
    public void onLoad(ModuleContext context) {
        // ConfigManager(dataFolder, resourceSource, logger)
        // - dataFolder: module data folder
        // - resourceSource: class for resource loading (use MyModule.class)
        // - logger: module logger
        configManager = new ConfigManager(
            context.getModuleDataFolder(),
            MyModule.class,
            context.getLogger()
        );
    }
}
```

**ConfigManager constructor:**

```java
public ConfigManager(File dataFolder, Class<?> resourceSource, Logger logger)
```

- `dataFolder` — `context.getModuleDataFolder()` (folder data module)
- `resourceSource` — `MyModule.class` (class untuk load resource dari JAR)
- `logger` — `context.getLogger()`

**Metode utama:**

| Method | Description |
|--------|-------------|
| `load()` | Load/reload config dari file |
| `reload()` | Reload config |
| `getConfig()` | Get `FileConfiguration` instance |
| `isValid()` | Cek apakah config valid |
| `getValidationErrors()` | List error validasi |

## Log Pattern

Module menggunakan `LogManager` untuk log archiving:

```java
LogManager logManager = new LogManager(
    context.getModuleDataFolder(),
    context.getLogger()
);
```

**LogManager constructor:**

```java
public LogManager(File dataFolder, Logger logger)
```

**Metode utama:**

| Method | Description |
|--------|-------------|
| `log(message)` | Tulis log ke file |
| `archive()` | Kompres log ke .zst format |
| `close()` | Tutup log writer |

## Command Pattern

Module menggunakan `TunnelCommand` pattern untuk command handling:

```java
public class MyModule implements OrynModule {

    private MyCommand myCommand;

    @Override
    public void onLoad(ModuleContext context) {
        myCommand = new MyCommand(context.getHostPlugin(), ...);
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        return myCommand.onModuleCommand(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return myCommand.onModuleTabComplete(sender, label, args);
    }
}
```

**Module command methods:**

| Method | Description |
|--------|-------------|
| `onModuleCommand(sender, label, args)` | Handle command dari module mode |
| `onModuleTabComplete(sender, label, args)` | Tab completion untuk module mode |

## Contoh: Module dengan Config

```java
public class MyModule implements OrynModule {

    private ModuleContext context;
    private ConfigManager configManager;

    @Override
    public void onLoad(ModuleContext context) {
        this.context = context;
        configManager = new ConfigManager(
            context.getModuleDataFolder(),
            MyModule.class,
            context.getLogger()
        );
    }

    @Override
    public void onEnable() {
        String message = configManager.getConfig().getString("welcome-message", "Hello!");
        context.getLogger().info(message);
    }
}
```

## Contoh: Module dengan Command

```java
public class MyModule implements OrynModule {

    private MyCommand myCommand;

    @Override
    public void onLoad(ModuleContext context) {
        myCommand = new MyCommand();
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        return myCommand.onModuleCommand(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return myCommand.onModuleTabComplete(sender, label, args);
    }
}

public class MyCommand {

    public boolean onModuleCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> sender.sendMessage("Module info!");
            case "reload" -> sender.sendMessage("Config reloaded!");
            default -> sender.sendMessage("Unknown subcommand. Use /oryn module <name> help");
        }

        return true;
    }

    public List<String> onModuleTabComplete(CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            return List.of("info", "reload", "help");
        }
        return List.of();
    }
}
```

## Contoh: Module dengan Auto-Update

```java
public class MyModule implements OrynModule {

    private CloudflaredManager cloudflaredManager;
    private ConfigManager configManager;
    private JavaPlugin hostPlugin;

    @Override
    public void onLoad(ModuleContext context) {
        hostPlugin = context.getHostPlugin();
        configManager = new ConfigManager(
            context.getModuleDataFolder(),
            MyModule.class,
            context.getLogger()
        );

        cloudflaredManager = new CloudflaredManager(
            hostPlugin,
            context.getModuleDataFolder(),
            new LogManager(context.getModuleDataFolder(), context.getLogger())
        );
    }

    @Override
    public void onEnable() {
        if (configManager.getConfig().getBoolean("auto-update", true)) {
            hostPlugin.getServer().getScheduler().runTaskAsynchronously(hostPlugin, () -> {
                cloudflaredManager.checkAndUpdate();
            });
        }
    }
}
```

**CloudflaredManager constructor:**

```java
public CloudflaredManager(JavaPlugin plugin, File dataFolder, LogManager logManager)
```

- `plugin` — `context.getHostPlugin()` (untuk scheduler)
- `dataFolder` — `context.getModuleDataFolder()` (bin folder di dalamnya)
- `logManager` — `LogManager` instance

## Troubleshooting

### Module tidak di-load

1. Pastikan JAR ada di `plugins/OrynPlugins/modules/`
2. Pastikan JAR memiliki `Main-Class` di `MANIFEST.MF`
3. Pastikan class yang di-define implements `OrynModule`
4. Cek log server untuk error messages

### ClassNotFoundException

- Pastikan dependency module ada di parent classloader (OrynPlugins)
- Atau bundle dependency ke dalam module JAR dengan Shadow

### NoClassDefFoundError

- Pastikan OrynPlugins sudah di-build terlebih dahulu
- Module JAR harus compile against OrynPlugins JAR

### Module tidak bisa akses class lain

- Module classloader menggunakan parent-first delegation
- Class dari module lain tidak bisa diakses (isolated)
- Gunakan event/buka API untuk inter-module communication

## Best Practices

1. **Gunakan `compileOnly` untuk OrynPlugins dependency** — Jangan bundle OrynPlugins ke dalam module JAR
2. **Config self-contained** — Module baca config dari folder data sendiri
3. **Graceful shutdown** — Handle `onDisable()` dengan benar (stop tasks, close resources)
4. **Logging** — Gunakan `context.getLogger()` bukan `System.out`
5. **Error handling** — Jangan biarkan exception crash server
6. **Permission** — Cek permission sebelum eksekusi command
7. **Resource source** — Pass `MyModule.class` ke ConfigManager untuk load default config dari JAR
8. **Thread safety** — Gunakan scheduler untuk async tasks, jangan block main thread
