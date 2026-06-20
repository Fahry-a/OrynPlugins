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
    private String loadedResourceName;
    private ConfigValidator validator;
    private final java.util.List<Runnable> reloadCallbacks = new java.util.concurrent.CopyOnWriteArrayList<>();

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
        loadedResourceName = resourceName;
    }

    public void reload() {
        if (loadedResourceName == null) {
            logger.warning("Cannot reload config: no config has been loaded yet");
            return;
        }
        File configFile = new File(dataFolder, loadedResourceName);
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);

            // Validate after reload
            if (!validate()) {
                logger.warning("Config validation failed after reload!");
            }

            // Fire callbacks
            for (Runnable callback : reloadCallbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.warning("Config reload callback error: " + e.getMessage());
                }
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public void saveConfig() {
        if (config != null && loadedResourceName != null) {
            File configFile = new File(dataFolder, loadedResourceName);
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

    // ==================== Validation ====================

    /**
     * Set a validator for this config. Called after loading/reloading.
     *
     * @param validator Validator to check config values
     */
    public void setValidator(ConfigValidator validator) {
        this.validator = validator;
    }

    /**
     * Validate the current config using the registered validator.
     *
     * @return true if valid (or no validator set), false if validation failed
     */
    public boolean validate() {
        if (validator == null || config == null) {
            return true;
        }
        try {
            return validator.validate(config);
        } catch (Exception e) {
            logger.warning("Config validation error: " + e.getMessage());
            return false;
        }
    }

    // ==================== Reload Callbacks ====================

    /**
     * Register a callback to be called after config is reloaded.
     *
     * @param callback The callback to run after reload
     */
    public void onReload(Runnable callback) {
        reloadCallbacks.add(callback);
    }

    /**
     * Remove a reload callback.
     */
    public void removeReloadCallback(Runnable callback) {
        reloadCallbacks.remove(callback);
    }

    // ==================== Config Validator Interface ====================

    /**
     * Interface for validating config values after load/reload.
     */
    @FunctionalInterface
    public interface ConfigValidator {
        /**
         * Validate the config. Return false if invalid.
         *
         * @param config The config to validate
         * @return true if valid, false otherwise
         */
        boolean validate(FileConfiguration config);
    }
}
