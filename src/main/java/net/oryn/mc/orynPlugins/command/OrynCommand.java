package net.oryn.mc.orynPlugins.command;

import net.oryn.mc.orynPlugins.OrynPlugins;
import net.oryn.mc.orynPlugins.module.ModuleLoader;
import net.oryn.mc.orynPlugins.module.OrynModule;

import org.bukkit.ChatColor;
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

            if (action.equals("list")) {
                listModules(sender);
                return true;
            }

            String[] moduleArgs = new String[args.length - 2];
            System.arraycopy(args, 2, moduleArgs, 0, moduleArgs.length);

            delegateToModule(sender, action, moduleArgs);
            return true;
        }

        sender.sendMessage(PREFIX + ERROR + "Unknown subcommand. Use /oryn help");
        return true;
    }

    private void listModules(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Loaded Modules ==========");

        var modules = moduleLoader.getModules();
        if (modules.isEmpty()) {
            sender.sendMessage(DIM + "No modules loaded");
            return;
        }

        for (OrynModule module : modules) {
            sender.sendMessage(INFO + module.getName() + " " + DIM + "v" + module.getVersion()
                    + " " + DIM + "- " + module.getDescription());
        }

        sender.sendMessage(HEADER + "====================================");
    }

    private void delegateToModule(CommandSender sender, String moduleName, String[] args) {
        OrynModule module = moduleLoader.getModule(moduleName);
        if (module == null) {
            sender.sendMessage(PREFIX + ERROR + "Module not found: " + moduleName);
            return;
        }

        String label = moduleName;
        if (args.length > 0) {
            label = args[0];
        }

        module.onCommand(sender, label, args);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Commands ==========");
        sender.sendMessage(INFO + "/oryn module list " + DIM + "- List all loaded modules");
        sender.sendMessage(INFO + "/oryn module <name> <args> " + DIM + "- Execute module command");
        sender.sendMessage(HEADER + "====================================");
    }

    private void showModuleHelp(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Module Commands ==========");
        sender.sendMessage(INFO + "/oryn module list " + DIM + "- List all loaded modules");
        sender.sendMessage(INFO + "/oryn module <name> " + DIM + "- Module help/commands");

        var modules = moduleLoader.getModules();
        if (!modules.isEmpty()) {
            sender.sendMessage("");
            sender.sendMessage(DIM + "Available modules:");
            for (OrynModule module : modules) {
                sender.sendMessage(INFO + "  " + module.getName() + DIM + " - " + module.getDescription());
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
            completions.addAll(moduleLoader.getModules().stream()
                    .map(OrynModule::getName)
                    .collect(Collectors.toList()));
            return completions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("module")) {
            String moduleName = args[1].toLowerCase();
            OrynModule module = moduleLoader.getModule(moduleName);
            if (module != null) {
                String[] moduleArgs = new String[args.length - 2];
                System.arraycopy(args, 2, moduleArgs, 0, moduleArgs.length);
                return module.onTabComplete(sender, args[2], moduleArgs);
            }
        }

        return Collections.emptyList();
    }
}
