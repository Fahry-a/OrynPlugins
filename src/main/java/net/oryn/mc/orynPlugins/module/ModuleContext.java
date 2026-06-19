package net.oryn.mc.orynPlugins.module;

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

    public ModuleContext(JavaPlugin hostPlugin, File moduleDataFolder, Logger logger) {
        this.hostPlugin = hostPlugin;
        this.moduleDataFolder = moduleDataFolder;
        this.logger = logger;
        this.configManager = new ModuleConfigManager(moduleDataFolder, logger);
    }

    // ==================== Core References ====================

    /**
     * Get the host plugin instance (OrynPlugins)
     */
    public JavaPlugin getHostPlugin() {
        return hostPlugin;
    }

    /**
     * Get the module's data folder
     */
    public File getModuleDataFolder() {
        return moduleDataFolder;
    }

    /**
     * Get the module's prefixed logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Get the config manager for this module
     */
    public ModuleConfigManager getConfigManager() {
        return configManager;
    }

    // ==================== Server Utilities ====================

    /**
     * Get the Bukkit server instance
     */
    public Server getServer() {
        return hostPlugin.getServer();
    }

    /**
     * Get the plugin manager
     */
    public PluginManager getPluginManager() {
        return hostPlugin.getServer().getPluginManager();
    }

    /**
     * Get the Bukkit scheduler
     */
    public BukkitScheduler getScheduler() {
        return hostPlugin.getServer().getScheduler();
    }

    // ==================== Event Helpers ====================

    /**
     * Register an event listener. Automatically unregistered on module disable.
     * 
     * @param listener The listener to register
     * @param eventClass The event class to listen for
     */
    public void registerEvents(Listener listener, Class<? extends org.bukkit.event.Event>... eventClass) {
        getPluginManager().registerEvents(listener, hostPlugin);
    }

    /**
     * Unregister all events for a listener
     */
    public void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    // ==================== Scheduler Helpers ====================

    /**
     * Run a task on the main thread (synchronous)
     * 
     * @param task The task to run
     * @return BukkitTask for cancellation
     */
    public BukkitTask runTask(Runnable task) {
        return getScheduler().runTask(hostPlugin, task);
    }

    /**
     * Run a task asynchronously (off main thread)
     * 
     * @param task The task to run
     * @return BukkitTask for cancellation
     */
    public BukkitTask runTaskAsync(Runnable task) {
        return getScheduler().runTaskAsynchronously(hostPlugin, task);
    }

    /**
     * Run a task after a delay (ticks)
     * 
     * @param task The task to run
     * @param delayTicks Delay in ticks (20 ticks = 1 second)
     * @return BukkitTask for cancellation
     */
    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        return getScheduler().runTaskLater(hostPlugin, task, delayTicks);
    }

    /**
     * Run a task asynchronously after a delay
     * 
     * @param task The task to run
     * @param delayTicks Delay in ticks
     * @return BukkitTask for cancellation
     */
    public BukkitTask runTaskLaterAsync(Runnable task, long delayTicks) {
        return getScheduler().runTaskLaterAsynchronously(hostPlugin, task, delayTicks);
    }

    /**
     * Run a task repeatedly
     * 
     * @param task The task to run
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return BukkitTask for cancellation
     */
    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return getScheduler().runTaskTimer(hostPlugin, task, delayTicks, periodTicks);
    }

    /**
     * Run a task repeatedly (async)
     * 
     * @param task The task to run
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return BukkitTask for cancellation
     */
    public BukkitTask runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        return getScheduler().runTaskTimerAsynchronously(hostPlugin, task, delayTicks, periodTicks);
    }

    // ==================== Player Utilities ====================

    /**
     * Get an online player by name
     */
    public Player getPlayer(String name) {
        return getServer().getPlayer(name);
    }

    /**
     * Get an online player by UUID
     */
    public Player getPlayer(java.util.UUID uuid) {
        return getServer().getPlayer(uuid);
    }

    /**
     * Get all online players
     */
    public java.util.Collection<? extends Player> getOnlinePlayers() {
        return getServer().getOnlinePlayers();
    }

    // ==================== Command Utilities ====================

    /**
     * Get a registered plugin command
     */
    public PluginCommand getCommand(String name) {
        return hostPlugin.getCommand(name);
    }

    // ==================== Resource Loading ====================

    /**
     * Get a resource from the module's JAR
     * 
     * @param resourcePath Path to the resource
     * @return InputStream or null if not found
     */
    public java.io.InputStream getResource(String resourcePath) {
        return hostPlugin.getResource(resourcePath);
    }
}
