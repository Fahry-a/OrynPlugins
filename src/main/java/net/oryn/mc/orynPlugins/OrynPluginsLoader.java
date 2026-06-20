package net.oryn.mc.orynPlugins;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

/**
 * Required by Paper's plugin loader system (referenced in plugin.yml).
 * No additional classpath entries needed — all dependencies are bundled via Shadow JAR.
 */
class OrynPluginsLoader implements PluginLoader {

    @Override
    public void classloader(final PluginClasspathBuilder builder) {
        // All dependencies are bundled into the Shadow JAR at build time.
        // Add dynamically loaded libraries here if needed in the future.
    }
}
