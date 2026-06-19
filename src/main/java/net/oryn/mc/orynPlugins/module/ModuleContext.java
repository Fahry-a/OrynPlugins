package net.oryn.mc.orynPlugins.module;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class ModuleContext {

    private final JavaPlugin hostPlugin;
    private final File moduleDataFolder;
    private final Logger logger;

    public ModuleContext(JavaPlugin hostPlugin, File moduleDataFolder, Logger logger) {
        this.hostPlugin = hostPlugin;
        this.moduleDataFolder = moduleDataFolder;
        this.logger = logger;
    }

    public JavaPlugin getHostPlugin() {
        return hostPlugin;
    }

    public File getModuleDataFolder() {
        return moduleDataFolder;
    }

    public Logger getLogger() {
        return logger;
    }
}
