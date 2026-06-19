package net.oryn.mc.orynPlugins.command;

import net.oryn.mc.orynPlugins.OrynPlugins;
import net.oryn.mc.orynPlugins.module.ModuleLoader;
import net.oryn.mc.orynPlugins.module.ModuleStatus;
import net.oryn.mc.orynPlugins.module.OrynModule;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrynCommand implements CommandExecutor, TabCompleter {

    private final OrynPlugins plugin;
    private final ModuleLoader moduleLoader;

    private static final String PREFIX = ChatColor.GOLD + "[Oryn] " + ChatColor.RESET;
    private static final String SUCCESS = ChatColor.GREEN + "";
    private static final String ERROR = ChatColor.RED + "";
    private static final String INFO = ChatColor.AQUA + "";
    private static final String HEADER = ChatColor.GOLD + "";
    private static final String DIM = ChatColor.GRAY + "";

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
                        sender.sendMessage(PREFIX + ERROR + "Usage: /oryn module info <name>");
                        return true;
                    }
                    showModuleInfo(sender, args[2]);
                    return true;
                }
                case "reload" -> {
                    if (args.length < 3) {
                        sender.sendMessage(PREFIX + ERROR + "Usage: /oryn module reload <name>");
                        return true;
                    }
                    reloadModule(sender, args[2]);
                    return true;
                }
                case "enable" -> {
                    if (args.length < 3) {
                        sender.sendMessage(PREFIX + ERROR + "Usage: /oryn module enable <name>");
                        return true;
                    }
                    enableModule(sender, args[2]);
                    return true;
                }
                case "disable" -> {
                    if (args.length < 3) {
                        sender.sendMessage(PREFIX + ERROR + "Usage: /oryn module disable <name>");
                        return true;
                    }
                    disableModule(sender, args[2]);
                    return true;
                }
                default -> {
                    // Delegate to module
                    String[] moduleArgs = new String[args.length - 2];
                    System.arraycopy(args, 2, moduleArgs, 0, moduleArgs.length);
                    delegateToModule(sender, action, moduleArgs);
                    return true;
                }
            }
        }

        sender.sendMessage(PREFIX + ERROR + "Unknown subcommand. Use /oryn help");
        return true;
    }

    private void listModules(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Loaded Modules ==========");

        var modules = moduleLoader.getModules();
        if (modules.isEmpty()) {
            sender.sendMessage(DIM + "No modules loaded");
            sender.sendMessage(HEADER + "====================================");
            return;
        }

        for (OrynModule module : modules) {
            ModuleStatus status = moduleLoader.getModuleStatus(module.getName());
            String statusColor;
            if (status == null) {
                statusColor = ChatColor.GRAY + "?";
            } else {
                statusColor = switch (status) {
                    case ENABLED -> ChatColor.GREEN + "ON";
                    case DISABLED -> ChatColor.RED + "OFF";
                    case LOADED -> ChatColor.YELLOW + "LOADED";
                    case ERRORED -> ChatColor.DARK_RED + "ERROR";
                };
            }
            sender.sendMessage(INFO + module.getName() + " " + DIM + "v" + module.getVersion()
                    + " " + statusColor + DIM + " - " + module.getDescription());
        }

        sender.sendMessage(HEADER + "====================================");
    }

    private void showModuleInfo(CommandSender sender, String moduleName) {
        OrynModule module = moduleLoader.getModule(moduleName);
        if (module == null) {
            sender.sendMessage(PREFIX + ERROR + "Module not found: " + moduleName);
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        long loadTime = moduleLoader.getModuleLoadTime(moduleName);

        sender.sendMessage(HEADER + "========== Module Info ==========");
        sender.sendMessage(INFO + "Name: " + DIM + module.getName());
        sender.sendMessage(INFO + "Version: " + DIM + module.getVersion());
        sender.sendMessage(INFO + "Description: " + DIM + module.getDescription());
        sender.sendMessage(INFO + "Author: " + DIM + module.getAuthor());

        if (!module.getDependencies().isEmpty()) {
            sender.sendMessage(INFO + "Dependencies: " + DIM + String.join(", ", module.getDependencies()));
        }
        if (!module.getSoftDependencies().isEmpty()) {
            sender.sendMessage(INFO + "Soft Dependencies: " + DIM + String.join(", ", module.getSoftDependencies()));
        }

        String statusStr;
        if (status == null) {
            statusStr = ChatColor.GRAY + "Unknown";
        } else {
            statusStr = switch (status) {
                case ENABLED -> ChatColor.GREEN + "Enabled";
                case DISABLED -> ChatColor.RED + "Disabled";
                case LOADED -> ChatColor.YELLOW + "Loaded";
                case ERRORED -> ChatColor.DARK_RED + "Errored";
            };
        }
        sender.sendMessage(INFO + "Status: " + statusStr);
        sender.sendMessage(INFO + "Load Time: " + DIM + loadTime + "ms");

        sender.sendMessage(HEADER + "=================================");
    }

    private void reloadModule(CommandSender sender, String moduleName) {
        OrynModule module = moduleLoader.getModule(moduleName);
        if (module == null) {
            sender.sendMessage(PREFIX + ERROR + "Module not found: " + moduleName);
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status != ModuleStatus.ENABLED) {
            sender.sendMessage(PREFIX + ERROR + "Module " + moduleName + " is not enabled");
            return;
        }

        try {
            moduleLoader.disableModule(moduleName);
            moduleLoader.enableModule(moduleName);
            sender.sendMessage(PREFIX + SUCCESS + "Module " + moduleName + " reloaded");
        } catch (Exception e) {
            sender.sendMessage(PREFIX + ERROR + "Failed to reload module: " + e.getMessage());
        }
    }

    private void enableModule(CommandSender sender, String moduleName) {
        if (!moduleLoader.hasModule(moduleName)) {
            sender.sendMessage(PREFIX + ERROR + "Module not found: " + moduleName);
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status == ModuleStatus.ENABLED) {
            sender.sendMessage(PREFIX + ERROR + "Module " + moduleName + " is already enabled");
            return;
        }

        if (moduleLoader.enableModule(moduleName)) {
            sender.sendMessage(PREFIX + SUCCESS + "Module " + moduleName + " enabled");
        } else {
            sender.sendMessage(PREFIX + ERROR + "Failed to enable module " + moduleName);
        }
    }

    private void disableModule(CommandSender sender, String moduleName) {
        if (!moduleLoader.hasModule(moduleName)) {
            sender.sendMessage(PREFIX + ERROR + "Module not found: " + moduleName);
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status != ModuleStatus.ENABLED) {
            sender.sendMessage(PREFIX + ERROR + "Module " + moduleName + " is not enabled");
            return;
        }

        if (moduleLoader.disableModule(moduleName)) {
            sender.sendMessage(PREFIX + SUCCESS + "Module " + moduleName + " disabled");
        } else {
            sender.sendMessage(PREFIX + ERROR + "Failed to disable module " + moduleName);
        }
    }

    private void delegateToModule(CommandSender sender, String moduleName, String[] args) {
        OrynModule module = moduleLoader.getModule(moduleName);
        if (module == null) {
            sender.sendMessage(PREFIX + ERROR + "Module not found: " + moduleName);
            return;
        }

        ModuleStatus status = moduleLoader.getModuleStatus(moduleName);
        if (status != ModuleStatus.ENABLED) {
            sender.sendMessage(PREFIX + ERROR + "Module " + moduleName + " is not enabled");
            return;
        }

        String moduleLabel = moduleName;
        if (args.length > 0) {
            moduleLabel = args[0];
        }

        module.onCommand(sender, moduleLabel, args);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Commands ==========");
        sender.sendMessage(INFO + "/oryn module list " + DIM + "- List all loaded modules");
        sender.sendMessage(INFO + "/oryn module info <name> " + DIM + "- Show module info");
        sender.sendMessage(INFO + "/oryn module enable <name> " + DIM + "- Enable a module");
        sender.sendMessage(INFO + "/oryn module disable <name> " + DIM + "- Disable a module");
        sender.sendMessage(INFO + "/oryn module reload <name> " + DIM + "- Reload a module");
        sender.sendMessage(INFO + "/oryn module <name> <args> " + DIM + "- Execute module command");
        sender.sendMessage(HEADER + "====================================");
    }

    private void showModuleHelp(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Module Commands ==========");
        sender.sendMessage(INFO + "/oryn module list " + DIM + "- List all loaded modules");
        sender.sendMessage(INFO + "/oryn module info <name> " + DIM + "- Show module info");
        sender.sendMessage(INFO + "/oryn module enable <name> " + DIM + "- Enable a module");
        sender.sendMessage(INFO + "/oryn module disable <name> " + DIM + "- Disable a module");
        sender.sendMessage(INFO + "/oryn module reload <name> " + DIM + "- Reload a module");
        sender.sendMessage(INFO + "/oryn module <name> " + DIM + "- Module help/commands");

        var modules = moduleLoader.getModules();
        if (!modules.isEmpty()) {
            sender.sendMessage("");
            sender.sendMessage(DIM + "Available modules:");
            for (OrynModule module : modules) {
                ModuleStatus status = moduleLoader.getModuleStatus(module.getName());
                String statusStr = status != null ? " [" + status.getDisplayName() + "]" : "";
                sender.sendMessage(INFO + "  " + module.getName() + DIM + " - " + module.getDescription() + ChatColor.GRAY + statusStr);
            }
        }

        sender.sendMessage(HEADER + "====================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("oryn.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("module").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            List<String> completions = new ArrayList<>();
            completions.add("list");
            completions.add("info");
            completions.add("enable");
            completions.add("disable");
            completions.add("reload");
            completions.addAll(moduleLoader.getModules().stream()
                    .map(OrynModule::getName)
                    .collect(Collectors.toList()));
            return completions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("module")) {
            String action = args[1].toLowerCase();
            if (List.of("info", "enable", "disable", "reload").contains(action)) {
                return moduleLoader.getModules().stream()
                        .map(OrynModule::getName)
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("module")) {
            String moduleName = args[1].toLowerCase();
            OrynModule module = moduleLoader.getModule(moduleName);
            if (module != null) {
                String[] moduleArgs = new String[args.length - 2];
                System.arraycopy(args, 2, moduleArgs, 0, moduleArgs.length);
                return module.onTabComplete(sender, moduleName, moduleArgs);
            }
        }

        return Collections.emptyList();
    }
}
