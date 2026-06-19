package net.oryn.mc.orynPlugins.module;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class ModuleContext {

    private final JavaPlugin hostPlugin;
    private final File moduleDataFolder;
    private final Logger logger;
    private final ModuleConfigManager configManager;

    public ModuleContext(JavaPlugin hostPlugin, File moduleDataFolder, Logger logger) {
        this.hostPlugin = hostPlugin;
        this.moduleDataFolder = moduleDataFolder;
        this.logger = logger;
        this.configManager = new ModuleConfigManager(moduleDataFolder, logger);
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

    public ModuleConfigManager getConfigManager() {
        return configManager;
    }
}
