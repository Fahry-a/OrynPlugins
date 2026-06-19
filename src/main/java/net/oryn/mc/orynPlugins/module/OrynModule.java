package net.oryn.mc.orynPlugins.module;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Main interface for OrynPlugins modules.
 * 
 * Modules must implement this interface to be loaded by OrynPlugins.
 * 
 * Metadata can be provided via:
 * 1. @ModuleInfo annotation (recommended) - no need to implement getter methods
 * 2. Implementing getter methods directly (legacy approach)
 */
public interface OrynModule {

    /**
     * Get the module name (unique, lowercase)
     * If @ModuleInfo annotation is present, this method is optional.
     */
    default String getName() {
        return null;
    }

    /**
     * Get the module version
     * If @ModuleInfo annotation is present, this method is optional.
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Get the module description
     * If @ModuleInfo annotation is present, this method is optional.
     */
    default String getDescription() {
        return "";
    }

    /**
     * Get the module author
     * If @ModuleInfo annotation is present, this method is optional.
     */
    default String getAuthor() {
        return "Unknown";
    }

    /**
     * Get hard dependencies (other module names that must be loaded first)
     * If @ModuleInfo annotation is present, this method is optional.
     */
    default List<String> getDependencies() {
        return Collections.emptyList();
    }

    /**
     * Get soft dependencies (optional modules that enhance functionality)
     * If @ModuleInfo annotation is present, this method is optional.
     */
    default List<String> getSoftDependencies() {
        return Collections.emptyList();
    }

    /**
     * Called when module is loaded.
     * 
     * Use this for initialization that must happen before enabling:
     * - Load configuration
     * - Initialize fields
     * - Register events
     * 
     * @param context The module context
     * @return true if load succeeded, false to abort (module will not be enabled)
     */
    boolean onLoad(ModuleContext context);

    /**
     * Called when module is enabled.
     * 
     * Use this for:
     * - Start async tasks
     * - Enable features
     * - Connect to external services
     */
    default void onEnable() {
        // Default empty implementation
    }

    /**
     * Called when module is disabled.
     * 
     * Use this for:
     * - Stop async tasks
     * - Save data
     * - Disconnect from services
     */
    default void onDisable() {
        // Default empty implementation
    }

    /**
     * Called when module is reloaded (disable + enable).
     * 
     * Default implementation calls onDisable() then onEnable().
     * Override for custom reload behavior (e.g., reload config without full restart).
     */
    default void onReload() {
        onDisable();
        onEnable();
    }

    /**
     * Handle command from /oryn modules <name> <args>
     * 
     * @param sender Command sender
     * @param label Command label (module name)
     * @param args Command arguments
     * @return true if command was handled
     */
    default boolean onCommand(CommandSender sender, String label, String[] args) {
        return false;
    }

    /**
     * Tab completion for /oryn modules <name> <args>
     * 
     * @param sender Command sender
     * @param label Command label (module name)
     * @param args Command arguments
     * @return List of completions
     */
    default List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return Collections.emptyList();
    }
}
