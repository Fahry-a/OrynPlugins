package net.oryn.mc.orynPlugins;

import net.oryn.mc.orynPlugins.command.OrynCommand;
import net.oryn.mc.orynPlugins.module.ModuleLoader;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrynPlugins extends JavaPlugin {

    private ModuleLoader moduleLoader;

    @Override
    public void onEnable() {
        moduleLoader = new ModuleLoader(this);

        OrynCommand orynCommand = new OrynCommand(this, moduleLoader);
        PluginCommand cmd = getCommand("oryn");
        if (cmd != null) {
            cmd.setExecutor(orynCommand);
            cmd.setTabCompleter(orynCommand);
        }

        moduleLoader.loadAllModules();
        moduleLoader.enableAllModules();

        getLogger().info("OrynPlugins enabled with " + moduleLoader.getModules().size() + " module(s)");
    }

    @Override
    public void onDisable() {
        if (moduleLoader != null) {
            moduleLoader.disableAllModules();
        }

        getLogger().info("OrynPlugins disabled");
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }
}
