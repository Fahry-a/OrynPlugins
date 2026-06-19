package net.oryn.mc.orynPlugins.module;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface OrynModule {

    String getName();

    String getVersion();

    String getDescription();

    default String getAuthor() {
        return "Unknown";
    }

    default List<String> getDependencies() {
        return Collections.emptyList();
    }

    default List<String> getSoftDependencies() {
        return Collections.emptyList();
    }

    /**
     * Called when module is loaded.
     * @return true if load succeeded, false to abort (module will not be enabled)
     */
    boolean onLoad(ModuleContext context);

    void onEnable();

    void onDisable();

    boolean onCommand(CommandSender sender, String label, String[] args);

    List<String> onTabComplete(CommandSender sender, String label, String[] args);
}
