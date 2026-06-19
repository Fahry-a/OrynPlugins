# Module Development Guide

Guide untuk membuat module baru untuk OrynPlugins v1.0.1.

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
    compileOnly("net.oryn.mc:orynplugins:1.0.1")
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
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public boolean onLoad(ModuleContext context) {
        this.context = context;
        context.getLogger().info("MyModule loaded!");
        return true; // Return false to abort loading
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

## Module API Reference

### `OrynModule` Interface

| Method | Return | Description |
|--------|--------|-------------|
| `getName()` | `String` | Nama module (unique, lowercase) |
| `getVersion()` | `String` | Version string |
| `getDescription()` | `String` | Deskripsi singkat |
| `getAuthor()` | `String` | Author name (default: "Unknown") |
| `getDependencies()` | `List<String>` | Hard dependencies (default: empty) |
| `getSoftDependencies()` | `List<String>` | Soft dependencies (default: empty) |
| `onLoad(ModuleContext)` | `boolean` | Called when module is loaded. Return `false` to abort |
| `onEnable()` | `void` | Called when module is enabled |
| `onDisable()` | `void` | Called when module is disabled |
| `onCommand(sender, label, args)` | `boolean` | Handle command dari `/oryn module <name>` |
| `onTabComplete(sender, label, args)` | `List<String>` | Tab completion untuk command |

### `ModuleContext` Class

| Method | Description |
|--------|-------------|
| `getHostPlugin()` | Instance OrynPlugins (`JavaPlugin`) |
| `getModuleDataFolder()` | Folder data module (`plugins/OrynPlugins/modules/<name>/`) |
| `getLogger()` | Per-module logger dengan prefix `[<name>]` |
| `getConfigManager()` | `ModuleConfigManager` instance |

### `ModuleStatus` Enum

| Status | Description |
|--------|-------------|
| `LOADED` | Module loaded but not yet enabled |
| `ENABLED` | Module is running |
| `DISABLED` | Module was disabled |
| `ERRORED` | Module failed to load or enable |

### `ModuleConfigManager` Class

Standardized config management untuk modules:

```java
public class ModuleConfigManager {
    void loadDefaultConfig(Class<?> resourceSource, String resourceName);
    void reload();
    FileConfiguration getConfig();
    File getDataFolder();
    void saveConfig();
    void saveDefaultConfig(Class<?> resourceSource, String resourceName);
}
```

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
   ├── Check name collision
   └── Call onLoad(context) → boolean

3. ModuleLoader.enableAllModules()
   └── Call onEnable() for each LOADED module

4. [Server Shutdown]

5. ModuleLoader.disableAllModules()
   ├── Call onDisable() for each ENABLED module
   └── Close classloaders
```

## Per-Module Logger

Setiap module mendapat logger dengan prefix `[<moduleName>]`:

```java
@Override
public boolean onLoad(ModuleContext context) {
    // Output: [MyModule] Hello world!
    context.getLogger().info("Hello world!");
    return true;
}
```

## Config Pattern

Module menggunakan `ModuleConfigManager` dari host:

```java
public class MyModule implements OrynModule {

    private ModuleConfigManager configManager;

    @Override
    public boolean onLoad(ModuleContext context) {
        configManager = context.getConfigManager();
        configManager.loadDefaultConfig(MyModule.class, "config.yml");
        return true;
    }

    @Override
    public void onEnable() {
        String message = configManager.getConfig().getString("welcome-message", "Hello!");
        context.getLogger().info(message);
    }
}
```

Atau gunakan `ConfigManager` custom seperti di Oryn-Tunnelv2:

```java
configManager = new ConfigManager(
    context.getModuleDataFolder(),
    MyModule.class,
    context.getLogger()
);
```

## Command Pattern

```java
public class MyModule implements OrynModule {

    private MyCommand myCommand;

    @Override
    public boolean onLoad(ModuleContext context) {
        myCommand = new MyCommand();
        return true;
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

## Module Management Commands

Users dapat manage modules dari in-game:

| Command | Description |
|---------|-------------|
| `/oryn module list` | List semua loaded modules dengan status |
| `/oryn module info <name>` | Detail info module (author, deps, status, load time) |
| `/oryn module enable <name>` | Enable module |
| `/oryn module disable <name>` | Disable module |
| `/oryn module reload <name>` | Reload module (disable + enable) |

## Contoh: Module dengan Dependencies

```java
public class MyModule implements OrynModule {

    @Override
    public List<String> getDependencies() {
        return List.of("tunnel"); // Requires tunnel module
    }

    @Override
    public List<String> getSoftDependencies() {
        return List.of("vault"); // Optional dependency
    }

    @Override
    public boolean onLoad(ModuleContext context) {
        // ModuleLoader will check dependencies before enabling
        return true;
    }
}
```

## Contoh: Module dengan Config + Auto-Update

```java
public class MyModule implements OrynModule {

    private ModuleContext context;
    private ConfigManager configManager;
    private CloudflaredManager cloudflaredManager;

    @Override
    public boolean onLoad(ModuleContext context) {
        this.context = context;

        configManager = new ConfigManager(
            context.getModuleDataFolder(),
            MyModule.class,
            context.getLogger()
        );

        cloudflaredManager = new CloudflaredManager(
            context.getHostPlugin(),
            context.getModuleDataFolder(),
            new LogManager(context.getModuleDataFolder(), context.getLogger())
        );

        return configManager.isValid();
    }

    @Override
    public void onEnable() {
        if (configManager.getConfig().getBoolean("auto-update", true)) {
            context.getHostPlugin().getServer().getScheduler().runTaskAsynchronously(
                context.getHostPlugin(),
                () -> cloudflaredManager.checkAndUpdate()
            );
        }
    }
}
```

## Troubleshooting

### Module tidak di-load

1. Pastikan JAR ada di `plugins/OrynPlugins/modules/`
2. Pastikan JAR memiliki `Main-Class` di `MANIFEST.MF`
3. Pastikan class yang di-define implements `OrynModule`
4. Pastikan `onLoad()` return `true`
5. Cek log server untuk error messages

### ClassNotFoundException

- Pastikan dependency module ada di parent classloader (OrynPlugins)
- Atau bundle dependency ke dalam module JAR dengan Shadow

### NoClassDefFoundError

- Pastikan OrynPlugins sudah di-build terlebih dahulu
- Module JAR harus compile against OrynPlugins JAR v1.0.1

### Module tidak bisa akses class lain

- Module classloader menggunakan parent-first delegation
- Class dari module lain tidak bisa diakses (isolated)
- Gunakan event/buka API untuk inter-module communication

### Name Collision

- Module dengan nama yang sama akan di-skip (warning di log)
- Gunakan nama yang unik untuk setiap module

## Best Practices

1. **Gunakan `compileOnly` untuk OrynPlugins dependency** — Jangan bundle OrynPlugins ke dalam module JAR
2. **Config self-contained** — Module baca config dari folder data sendiri
3. **Graceful shutdown** — Handle `onDisable()` dengan benar (stop tasks, close resources)
4. **Logging** — Gunakan `context.getLogger()` (sudah ada prefix)
5. **Error handling** — Return `false` dari `onLoad()` jika config invalid
6. **Permission** — Cek permission sebelum eksekusi command
7. **Resource source** — Pass `MyModule.class` ke ConfigManager untuk load default config
8. **Thread safety** — Gunakan scheduler untuk async tasks, jangan block main thread
9. **Unique names** — Gunakan nama module yang unik untuk menghindari collision
10. **Metadata** — Sediakan `getAuthor()` dan `getDependencies()` untuk info
