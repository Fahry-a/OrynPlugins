package net.oryn.mc.orynPlugins.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages subcommands for a module.
 * Provides registration, execution, and tab completion routing.
 *
 * Usage:
 * <pre>
 * SubcommandHandler handler = new SubcommandHandler("mymodule", "My module commands");
 * handler.register("reload", "Reload config", sender -&gt; { ... });
 * handler.register("status", "Show status", this::handleStatus, this::tabStatus);
 * 
 * // In onCommand:
 * return handler.execute(sender, args);
 * 
 * // In onTabComplete:
 * return handler.tabComplete(sender, args);
 * </pre>
 */
public class SubcommandHandler {

    private final String moduleName;
    private final String description;
    private final Map<String, SubcommandEntry> subcommands = new LinkedHashMap<>();

    public SubcommandHandler(String moduleName, String description) {
        this.moduleName = moduleName;
        this.description = description;
    }

    /**
     * Register a subcommand.
     *
     * @param name Subcommand name (lowercase)
     * @param description Description for help
     * @param executor Handler for execution
     */
    public void register(String name, String description, SubcommandExecutor executor) {
        subcommands.put(name.toLowerCase(), new SubcommandEntry(name, description, executor));
    }

    /**
     * Register a subcommand with a simple lambda.
     *
     * @param name Subcommand name
     * @param description Description for help
     * @param action Handler lambda
     */
    public void register(String name, String description, java.util.function.BiConsumer<CommandSender, String[]> action) {
        subcommands.put(name.toLowerCase(), new SubcommandEntry(name, description, new SubcommandExecutor() {
            @Override
            public void execute(CommandSender sender, String[] args) {
                action.accept(sender, args);
            }
        }));
    }

    /**
     * Execute a subcommand based on the first argument.
     *
     * @param sender Command sender
     * @param args Arguments (first element is subcommand name)
     * @return true if a subcommand was found and executed
     */
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        SubcommandEntry entry = subcommands.get(subName);

        if (entry == null) {
            sender.sendMessage(Component.text("Unknown subcommand: " + subName, NamedTextColor.RED));
            showHelp(sender);
            return true;
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        try {
            entry.executor().execute(sender, subArgs);
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error executing " + subName + ": " + e.getMessage(), NamedTextColor.RED));
        }

        return true;
    }

    /**
     * Tab complete based on current arguments.
     *
     * @param sender Command sender
     * @param args Current arguments
     * @return List of completions
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // Complete subcommand names
            String prefix = args[0].toLowerCase();
            return subcommands.keySet().stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        // Delegate to subcommand
        String subName = args[0].toLowerCase();
        SubcommandEntry entry = subcommands.get(subName);
        if (entry == null) {
            return Collections.emptyList();
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        return entry.executor().tabComplete(sender, subArgs);
    }

    /**
     * Show help for all registered subcommands.
     */
    public void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== " + moduleName + " Commands ==========", NamedTextColor.GOLD));
        for (SubcommandEntry entry : subcommands.values()) {
            sender.sendMessage(
                Component.text("/" + moduleName + " " + entry.name() + " ", NamedTextColor.AQUA)
                    .append(Component.text("- " + entry.description(), NamedTextColor.GRAY))
            );
        }
        sender.sendMessage(Component.text("====================================", NamedTextColor.GOLD));
    }

    /**
     * Get all registered subcommand names.
     */
    public java.util.Set<String> getSubcommandNames() {
        return Collections.unmodifiableSet(subcommands.keySet());
    }

    /**
     * Check if a subcommand is registered.
     */
    public boolean hasSubcommand(String name) {
        return subcommands.containsKey(name.toLowerCase());
    }

    private record SubcommandEntry(String name, String description, SubcommandExecutor executor) {}
}
