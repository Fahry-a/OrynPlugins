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

## Persiapan

### 1. Buat Project Gradle Baru

```kotlin
// build.gradle.kts
plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly(files("../OrynPlugins/build/libs/OrynPlugins-1.0-SNAPSHOT.jar"))
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
| `getHostPlugin()` | Instance OrynPlugins (JavaPlugin) |
| `getModuleDataFolder()` | Folder data module (`modules/<name>/`) |
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

## Contoh: Module dengan Config

```java
public class MyModule implements OrynModule {

    private ModuleContext context;
    private FileConfiguration config;

    @Override
    public void onLoad(ModuleContext context) {
        this.context = context;

        // Load config dari module data folder
        File configFile = new File(context.getModuleDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // Save default config
            context.getHostPlugin().saveResource("config.yml", false);
        }
        YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void onEnable() {
        String message = config.getString("welcome-message", "Hello!");
        context.getLogger().info(message);
    }
}
```

## Contoh: Module dengan Command

```java
public class MyModule implements OrynModule {

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> sender.sendMessage("Module info: " + getName() + " v" + getVersion());
            case "reload" -> {
                // Reload config
                sender.sendMessage("Config reloaded!");
            }
            default -> sender.sendMessage("Unknown subcommand. Use /oryn module " + getName() + " help");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            return List.of("info", "reload", "help");
        }
        return List.of();
    }
}
```

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

1. **Gunakan `compileOnly` untuk OrynPlugins dependency** - Jangan bundle OrynPlugins ke dalam module JAR
2. **Config self-contained** - Module baca config dari folder data sendiri
3. **Graceful shutdown** - Handle `onDisable()` dengan benar
4. **Logging** - Gunakan `context.getLogger()` bukan `System.out`
5. **Error handling** - Jangan biarkan exception crash server
6. **Permission** - Cek permission sebelum eksekusi command
