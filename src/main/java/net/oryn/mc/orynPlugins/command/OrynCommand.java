package net.oryn.mc.orynPlugins.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.oryn.mc.orynPlugins.OrynPlugins;
import net.oryn.mc.orynPlugins.module.ModuleLoader;
import net.oryn.mc.orynPlugins.module.ModuleStatus;
import net.oryn.mc.orynPlugins.module.OrynModule;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrynCommand implements CommandExecutor, TabCompleter {

    private final OrynPlugins plugin;
    private final ModuleLoader moduleLoader;

    private static final TextColor GOLD = NamedTextColor.GOLD;
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor RED = NamedTextColor.RED;
    private static final TextColor AQUA = NamedTextColor.AQUA;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor DARK_RED = NamedTextColor.DARK_RED;
    private static final TextColor YELLOW = NamedTextColor.YELLOW;

    private static Component prefix() {
        return Component.text("[Oryn] ", GOLD);
    }

    private static Component success(String text) {
        return Component.text(text, GREEN);
    }

    private static Component error(String text) {
        return Component.text(text, RED);
    }

    private static Component info(String text) {
        return Component.text(text, AQUA);
    }

    private static Component header(String text) {
        return Component.text(text, GOLD);
    }

    private static Component dim(String text) {
        return Component.text(text, GRAY);
    }

    public OrynCommand(OrynPlugins plugin, ModuleLoader moduleLoader) {
        this.plugin = plugin;
        this.moduleLoader = moduleLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // /oryn module <action> - Management commands
        if (subCommand.equals("module")) {
            if (args.length < 2) {
                showModuleHelp(sender);
                return true;
            }

            String action = args[1].toLowerCase();

            switch (action) {
                case "list" -> {
                    listModules(sender);
                    return true;
                }
                case "info" -> {
                    if (args.length < 3) {
                        sender.sendMessage(prefix().append(error("Usage: /oryn module info <name>")));
                        return true;
                    }
                    showModuleInfo(sender, args[2]);
                    return true;
                }
                case "reload" -> {
                    if (args.length < 3) {
                        sender.sendMessage(prefix().append(error("Usage: /oryn module reload <name>")));
                        return true;
                    }
                    reloadModule(sender, args[2]);
                    return true;
                }
                case "enable" -> {
                    if (args.length < 3) {
                        sender.sendMessage(prefix().append(error("Usage: /oryn module enable <name>")));
                        return true;
                    }
                    enableModule(sender, args[2]);
                    return true;
                }
                case "disable" -> {
                    if (args.length < 3) {
                        sender.sendMessage(prefix().append(error("Usage: /oryn module disable <name>")));
                        return true;
                    }
                    disableModule(sender, args[2]);
                    return true;
                }
                case "detect" -> {
                    detectNewModules(sender);
                    return true;
                }
                default -> {
                    sender.sendMessage(prefix().append(error("Unknown action: " + action + ". Use /oryn module list")));
                    return true;
                }
            }
        }

        // /oryn modules <name> <args> - Delegate to module
        if (subCommand.equals("modules")) {
            if (args.length < 2) {
                showModulesHelp(sender);
                return true;
            }

            String moduleName = args[1].toLowerCase();
            String[] moduleArgs = new String[args.length - 2];
            System.arraycopy(args, 2, moduleArgs, 0, moduleArgs.length);
            delegateToModule(sender, moduleName, moduleArgs);
            return true;
        }

        sender.sendMessage(prefix().append(error("Unknown subcommand. Use /oryn help")));
        return true;
    }

    private void listModules(CommandSender sender) {
        sender.sendMessage(header("========== Loaded Modules =========="));

        var modules = moduleLoader.getModules();
        if (modules.isEmpty()) {
            sender.sendMessage(dim("No modules loaded"));
            sender.sendMessage(header("===================================="));
            return;
        }

        for (OrynModule module : modules) {
            ModuleStatus status = moduleLoader.getModuleStatus(module.getName());
            Component statusText;
            if (status == null) {
                statusText = Component.text("?", GRAY);
            } else {
                statusText = switch (status) {
                    case ENABLED -> Component.text("ON", GREEN);
                    case DISABLED -> Component.text("OFF", RED);
                    case LOADED -> Component.text("LOADED", YELLOW);
                    case ERRORED -> Component.text("ERROR", DARK_RED);
                };
            }
            sender.sendMessage(
                info(module.getName()).append(Component.text(" ")).append(dim("v" + module.getVersion() + " ")).append(statusText).append(dim(" - " + module.getDescription()))
            );
        }

        sender.sendMessage(header("===================================="));
    }

    private void showModuleInfo(CommandSender sender, String moduleName) {
        OrynModule module = moduleLoader.getModule(moduleName);
        if (module == null) {
            sender.sendMessage(prefix().append(error("Module not found: " + moduleName)));
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        long loadTime = moduleLoader.getModuleLoadTime(moduleName);

        sender.sendMessage(header("========== Module Info =========="));
        sender.sendMessage(info("Name: ").append(dim(module.getName())));
        sender.sendMessage(info("Version: ").append(dim(module.getVersion())));
        sender.sendMessage(info("Description: ").append(dim(module.getDescription())));
        sender.sendMessage(info("Author: ").append(dim(module.getAuthor())));

        List<String> deps = moduleLoader.getModuleDependencies(moduleName);
        if (!deps.isEmpty()) {
            sender.sendMessage(info("Dependencies: ").append(dim(String.join(", ", deps))));
        }

        List<String> softDeps = moduleLoader.getModuleSoftDependencies(moduleName);
        if (!softDeps.isEmpty()) {
            sender.sendMessage(info("Soft Dependencies: ").append(dim(String.join(", ", softDeps))));
        }

        Component statusText;
        if (status == null) {
            statusText = Component.text("Unknown", GRAY);
        } else {
            statusText = switch (status) {
                case ENABLED -> Component.text("Enabled", GREEN);
                case DISABLED -> Component.text("Disabled", RED);
                case LOADED -> Component.text("Loaded", YELLOW);
                case ERRORED -> Component.text("Errored", DARK_RED);
            };
        }
        sender.sendMessage(info("Status: ").append(statusText));
        sender.sendMessage(info("Load Time: ").append(dim(loadTime + "ms")));

        sender.sendMessage(header("================================="));
    }

    private void reloadModule(CommandSender sender, String moduleName) {
        if (!moduleLoader.hasModule(moduleName)) {
            sender.sendMessage(prefix().append(error("Module not found: " + moduleName)));
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status != ModuleStatus.ENABLED && status != ModuleStatus.DISABLED) {
            sender.sendMessage(prefix().append(error("Module " + moduleName + " is not in a reloadable state")));
            return;
        }

        try {
            moduleLoader.reloadModule(moduleName);
            sender.sendMessage(prefix().append(success("Module " + moduleName + " reloaded")));
        } catch (Exception e) {
            sender.sendMessage(prefix().append(error("Failed to reload module: " + e.getMessage())));
        }
    }

    private void enableModule(CommandSender sender, String moduleName) {
        if (!moduleLoader.hasModule(moduleName)) {
            sender.sendMessage(prefix().append(error("Module not found: " + moduleName)));
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status == ModuleStatus.ENABLED) {
            sender.sendMessage(prefix().append(error("Module " + moduleName + " is already enabled")));
            return;
        }

        if (moduleLoader.enableModule(moduleName)) {
            sender.sendMessage(prefix().append(success("Module " + moduleName + " enabled")));
        } else {
            sender.sendMessage(prefix().append(error("Failed to enable module " + moduleName)));
        }
    }

    private void disableModule(CommandSender sender, String moduleName) {
        if (!moduleLoader.hasModule(moduleName)) {
            sender.sendMessage(prefix().append(error("Module not found: " + moduleName)));
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status != ModuleStatus.ENABLED) {
            sender.sendMessage(prefix().append(error("Module " + moduleName + " is not enabled")));
            return;
        }

        if (moduleLoader.disableModule(moduleName)) {
            sender.sendMessage(prefix().append(success("Module " + moduleName + " disabled")));
        } else {
            sender.sendMessage(prefix().append(error("Failed to disable module " + moduleName)));
        }
    }

    private void detectNewModules(CommandSender sender) {
        sender.sendMessage(prefix().append(info("Scanning for new modules...")));

        List<String> newModules = moduleLoader.detectNewModules();

        if (newModules.isEmpty()) {
            sender.sendMessage(prefix().append(dim("No new modules found")));
            return;
        }

        Component msg = prefix().append(success("Found " + newModules.size() + " new module(s):"));
        sender.sendMessage(msg);
        for (String moduleName : newModules) {
            sender.sendMessage(info("  + " + moduleName).append(dim(" (loaded, use /oryn module enable " + moduleName + " to enable)")));
        }
    }

    private void delegateToModule(CommandSender sender, String moduleName, String[] args) {
        OrynModule module = moduleLoader.getModule(moduleName);
        if (module == null) {
            sender.sendMessage(prefix().append(error("Module not found: " + moduleName)));
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status != ModuleStatus.ENABLED) {
            sender.sendMessage(prefix().append(error("Module " + moduleName + " is not enabled")));
            return;
        }

        String moduleLabel = moduleName;
        if (args.length > 0) {
            moduleLabel = args[0];
        }

        module.onCommand(sender, moduleLabel, args);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(header("========== Oryn Commands =========="));
        sender.sendMessage(info("/oryn help ").append(dim("- Show this help")));
        sender.sendMessage(info("/oryn module list ").append(dim("- List all loaded modules")));
        sender.sendMessage(info("/oryn module info <name> ").append(dim("- Show module info")));
        sender.sendMessage(info("/oryn module enable <name> ").append(dim("- Enable a module")));
        sender.sendMessage(info("/oryn module disable <name> ").append(dim("- Disable a module")));
        sender.sendMessage(info("/oryn module reload <name> ").append(dim("- Reload a module")));
        sender.sendMessage(info("/oryn module detect ").append(dim("- Detect new modules in folder")));
        sender.sendMessage(info("/oryn modules <name> <args> ").append(dim("- Execute module command")));
        sender.sendMessage(header("===================================="));
    }

    private void showModuleHelp(CommandSender sender) {
        sender.sendMessage(header("========== Module Management =========="));
        sender.sendMessage(info("/oryn module list ").append(dim("- List all loaded modules")));
        sender.sendMessage(info("/oryn module info <name> ").append(dim("- Show module info")));
        sender.sendMessage(info("/oryn module enable <name> ").append(dim("- Enable a module")));
        sender.sendMessage(info("/oryn module disable <name> ").append(dim("- Disable a module")));
        sender.sendMessage(info("/oryn module reload <name> ").append(dim("- Reload a module")));
        sender.sendMessage(info("/oryn module detect ").append(dim("- Detect new modules in folder")));
        sender.sendMessage(header("========================================"));
    }

    private void showModulesHelp(CommandSender sender) {
        sender.sendMessage(header("========== Module Commands =========="));
        sender.sendMessage(info("/oryn modules <name> <args> ").append(dim("- Execute module command")));

        var modules = moduleLoader.getModules();
        if (!modules.isEmpty()) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(dim("Available modules:"));
            for (OrynModule module : modules) {
                ModuleStatus status = moduleLoader.getModuleStatus(module.getName());
                Component statusStr = status != null
                    ? Component.text(" [" + status.getDisplayName() + "]", GRAY)
                    : Component.empty();
                sender.sendMessage(info("  " + module.getName()).append(dim(" - " + module.getDescription())).append(statusStr));
            }
        }

        sender.sendMessage(header("===================================="));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("oryn.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("module", "modules").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // /oryn module <action> - Management commands
        if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            List<String> completions = List.of("list", "info", "enable", "disable", "reload", "detect");
            return completions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // /oryn module info|enable|disable|reload <name>
        if (args.length == 3 && args[0].equalsIgnoreCase("module")) {
            String action = args[1].toLowerCase();
            if (List.of("info", "enable", "disable", "reload").contains(action)) {
                return moduleLoader.getModules().stream()
                        .map(OrynModule::getName)
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // /oryn modules <name> - Module names
        if (args.length == 2 && args[0].equalsIgnoreCase("modules")) {
            return moduleLoader.getModules().stream()
                    .map(OrynModule::getName)
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // /oryn modules <name> <args...> - Delegate tab complete to module
        if (args.length >= 3 && args[0].equalsIgnoreCase("modules")) {
            String moduleName = args[1].toLowerCase();
            OrynModule module = moduleLoader.getModule(moduleName);
            ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
            if (module != null && status == ModuleStatus.ENABLED) {
                try {
                    String[] moduleArgs = new String[args.length - 2];
                    System.arraycopy(args, 2, moduleArgs, 0, moduleArgs.length);

                    String[] filteredArgs = moduleArgs;
                    if (moduleArgs.length > 0 && moduleArgs[moduleArgs.length - 1].isEmpty()) {
                        filteredArgs = new String[moduleArgs.length - 1];
                        System.arraycopy(moduleArgs, 0, filteredArgs, 0, filteredArgs.length);
                    }

                    List<String> result = module.onTabComplete(sender, moduleName, filteredArgs);
                    if (result != null) {
                        return result;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Tab completion error for module '" + moduleName + "': " + e.getMessage());
                }
            }
        }

        return Collections.emptyList();
    }
}
