package net.oryn.mc.orynPlugins.module;

import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Context provided to modules during their lifecycle.
 * Contains references to host plugin, utilities, and helper methods.
 */
public class ModuleContext {

    private final JavaPlugin hostPlugin;
    private final File moduleDataFolder;
    private final Logger logger;
    private final ModuleConfigManager configManager;
    private final ClassLoader moduleClassLoader;
    private final ModuleServiceRegistry serviceRegistry;
    private final ModuleLoader moduleLoader;
    private final List<Listener> registeredListeners = new ArrayList<>();

    public ModuleContext(JavaPlugin hostPlugin, File moduleDataFolder, Logger logger) {
        this(hostPlugin, moduleDataFolder, logger, null, null, null);
    }

    public ModuleContext(JavaPlugin hostPlugin, File moduleDataFolder, Logger logger, ClassLoader moduleClassLoader) {
        this(hostPlugin, moduleDataFolder, logger, moduleClassLoader, null, null);
    }

    public ModuleContext(JavaPlugin hostPlugin, File moduleDataFolder, Logger logger,
                         ClassLoader moduleClassLoader, ModuleServiceRegistry serviceRegistry, ModuleLoader moduleLoader) {
        this.hostPlugin = hostPlugin;
        this.moduleDataFolder = moduleDataFolder;
        this.logger = logger;
        this.configManager = new ModuleConfigManager(moduleDataFolder, logger);
        this.moduleClassLoader = moduleClassLoader;
        this.serviceRegistry = serviceRegistry;
        this.moduleLoader = moduleLoader;
    }

    // ==================== Core References ====================

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

    public ModuleServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    // ==================== Server Utilities ====================

    public Server getServer() {
        return hostPlugin.getServer();
    }

    public PluginManager getPluginManager() {
        return hostPlugin.getServer().getPluginManager();
    }

    public BukkitScheduler getScheduler() {
        return hostPlugin.getServer().getScheduler();
    }

    // ==================== Event Helpers ====================

    /**
     * Register an event listener. Automatically unregistered when module is disabled.
     */
    public void registerEvents(Listener listener) {
        getPluginManager().registerEvents(listener, hostPlugin);
        registeredListeners.add(listener);
    }

    /**
     * Unregister all events for a listener.
     */
    public void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
        registeredListeners.remove(listener);
    }

    /**
     * Unregister all events registered by this module.
     * Called automatically when the module is disabled.
     */
    public void unregisterAllEvents() {
        for (Listener listener : registeredListeners) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();
    }

    // ==================== Scheduler Helpers ====================

    public BukkitTask runTask(Runnable task) {
        return getScheduler().runTask(hostPlugin, task);
    }

    public BukkitTask runTaskAsync(Runnable task) {
        return getScheduler().runTaskAsynchronously(hostPlugin, task);
    }

    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        return getScheduler().runTaskLater(hostPlugin, task, delayTicks);
    }

    public BukkitTask runTaskLaterAsync(Runnable task, long delayTicks) {
        return getScheduler().runTaskLaterAsynchronously(hostPlugin, task, delayTicks);
    }

    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return getScheduler().runTaskTimer(hostPlugin, task, delayTicks, periodTicks);
    }

    public BukkitTask runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        return getScheduler().runTaskTimerAsynchronously(hostPlugin, task, delayTicks, periodTicks);
    }

    // ==================== Player Utilities ====================

    public Player getPlayer(String name) {
        return getServer().getPlayer(name);
    }

    public Player getPlayer(java.util.UUID uuid) {
        return getServer().getPlayer(uuid);
    }

    public Collection<? extends Player> getOnlinePlayers() {
        return getServer().getOnlinePlayers();
    }

    // ==================== Broadcast Helpers ====================

    /**
     * Send a message to all online players.
     */
    public void broadcast(Component message) {
        getServer().broadcast(message);
    }

    /**
     * Send a message to all online players (string version).
     */
    public void broadcast(String message) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Send a message to all online players with a specific permission.
     */
    public void broadcastPermission(Component message, String permission) {
        for (Player player : getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Send a message to all online players with a specific permission (string version).
     */
    public void broadcastPermission(String message, String permission) {
        for (Player player : getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }
    }

    // ==================== Command Utilities ====================

    public PluginCommand getCommand(String name) {
        return hostPlugin.getCommand(name);
    }

    // ==================== Module Utilities ====================

    /**
     * Get another loaded module by name.
     * Useful for inter-module dependencies at runtime.
     *
     * @param name Module name
     * @return The module, or null if not found
     */
    public OrynModule getModule(String name) {
        if (moduleLoader == null) return null;
        return moduleLoader.getModule(name);
    }

    /**
     * Check if a module is loaded and enabled.
     *
     * @param name Module name
     * @return true if the module exists and is enabled
     */
    public boolean isModuleEnabled(String name) {
        if (moduleLoader == null) return false;
        ModuleStatus status = moduleLoader.getModuleStatus(name);
        return status == ModuleStatus.ENABLED;
    }

    /**
     * Get the status of a module.
     *
     * @param name Module name
     * @return Module status, or null if not found
     */
    public ModuleStatus getModuleStatus(String name) {
        if (moduleLoader == null) return null;
        return moduleLoader.getModuleStatus(name);
    }

    // ==================== Module Loader ====================

    /**
     * Get the module loader instance.
     * Useful for registering lifecycle listeners or accessing loader-level operations.
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    // ==================== Resource Loading ====================

    /**
     * Get a resource from the module's JAR using the module classloader.
     * Falls back to the host plugin classloader if module classloader is not available.
     *
     * @param resourcePath Path to the resource (e.g. "config.yml")
     * @return InputStream or null if not found
     */
    public InputStream getResource(String resourcePath) {
        InputStream stream = null;
        if (moduleClassLoader != null) {
            stream = moduleClassLoader.getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            stream = hostPlugin.getResource(resourcePath);
        }
        return stream;
    }

    /**
     * Save a resource from the module's JAR to the module's data folder.
     * If the file already exists and replace is false, does nothing.
     *
     * @param resourcePath Resource path in JAR (e.g. "config.yml")
     * @param replace Whether to replace an existing file
     * @return true if the resource was saved
     */
    public boolean saveResource(String resourcePath, boolean replace) {
        File outFile = new File(moduleDataFolder, resourcePath);
        if (outFile.exists() && !replace) {
            return false;
        }

        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                logger.warning("Resource not found in module JAR: " + resourcePath);
                return false;
            }

            // Ensure parent directories exist
            File parentDir = outFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Files.copy(in, outFile.toPath(), replace ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            logger.warning("Failed to save resource " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }
}
