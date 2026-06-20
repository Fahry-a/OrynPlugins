package net.oryn.mc.orynPlugins.module;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Interface for a subcommand handler.
 * Implement this to create a subcommand for your module.
 *
 * Usage in a module:
 * <pre>
 * public class MyModule implements OrynModule {
 *     private SubcommandHandler handler;
 * 
 *     {@literal @}Override
 *     public boolean onLoad(ModuleContext context) {
 *         handler = new SubcommandHandler("mymodule", "My module commands");
 *         handler.register("reload", "Reload configuration", this::handleReload);
 *         handler.register("status", "Show status", this::handleStatus, this::tabStatus);
 *         return true;
 *     }
 * 
 *     {@literal @}Override
 *     public boolean onCommand(CommandSender sender, String label, String[] args) {
 *         return handler.execute(sender, args);
 *     }
 * 
 *     {@literal @}Override
 *     public List&lt;String&gt; onTabComplete(CommandSender sender, String label, String[] args) {
 *         return handler.tabComplete(sender, args);
 *     }
 * 
 *     private void handleReload(CommandSender sender, String[] args) {
 *         sender.sendMessage("Reloading...");
 *     }
 * }
 * </pre>
 */
public interface SubcommandExecutor {

    /**
     * Handle execution of this subcommand.
     *
     * @param sender The command sender
     * @param args Arguments (without the subcommand name)
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Provide tab completions for this subcommand.
     *
     * @param sender The command sender
     * @param args Current arguments (without the subcommand name)
     * @return List of completions, or empty list
     */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
