package net.oryn.mc.orynPlugins.module;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

public class ModuleConfigManager {

    private final File dataFolder;
    private final Logger logger;
    private FileConfiguration config;

    public ModuleConfigManager(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void loadDefaultConfig(Class<?> resourceSource, String resourceName) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, resourceName);

        if (!configFile.exists()) {
            try (InputStream in = resourceSource.getResourceAsStream("/" + resourceName)) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    logger.warning(resourceName + " not found in module resources");
                }
            } catch (IOException e) {
                logger.warning("Failed to save default " + resourceName + ": " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        File configFile = new File(dataFolder, config.getName());
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public void saveConfig() {
        if (config != null) {
            File configFile = new File(dataFolder, config.getName());
            try {
                config.save(configFile);
            } catch (IOException e) {
                logger.warning("Failed to save config: " + e.getMessage());
            }
        }
    }

    public void saveDefaultConfig(Class<?> resourceSource, String resourceName) {
        File configFile = new File(dataFolder, resourceName);
        if (!configFile.exists()) {
            try (InputStream in = resourceSource.getResourceAsStream("/" + resourceName)) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                logger.warning("Failed to save default " + resourceName + ": " + e.getMessage());
            }
        }
    }
}
