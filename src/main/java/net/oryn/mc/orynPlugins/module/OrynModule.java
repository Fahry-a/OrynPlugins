package net.oryn.mc.orynPlugins.module;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface OrynModule {

    String getName();

    String getVersion();

    String getDescription();

    void onLoad(ModuleContext context);

    void onEnable();

    void onDisable();

    boolean onCommand(CommandSender sender, String label, String[] args);

    List<String> onTabComplete(CommandSender sender, String label, String[] args);
}
