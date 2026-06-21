package net.oryn.mc.orynPlugins;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

/**
 * Paper PluginLoader implementation for OrynPlugins.
 *
 * <p>This class is referenced by the {@code paperPluginLoader} field in plugin.yml
 * and is invoked by Paper's plugin loading system <b>before</b> the main plugin
 * class ({@link OrynPlugins}) is instantiated. Its purpose is to configure the
 * plugin's classpath by adding additional JARs or libraries that should be
 * available at runtime.</p>
 *
 * <h2>Why is this needed?</h2>
 * <p>Paper introduced the {@link PluginLoader} interface to give plugins fine-grained
 * control over their classpath. Without a loader, Paper uses a default classloader
 * that may not include all required dependencies. By implementing this interface,
 * OrynPlugins ensures its classpath is correctly set up — even though in the current
 * version all dependencies are bundled into the Shadow JAR at build time.</p>
 *
 * <h2>Current behavior</h2>
 * <p>The {@link #classloader(PluginClasspathBuilder)} method is intentionally empty
 * because:</p>
 * <ul>
 *   <li>All compile-time dependencies (Paper API) are {@code compileOnly} and provided
 *       by the server at runtime.</li>
 *   <li>No runtime libraries need to be dynamically loaded.</li>
 * </ul>
 *
 * <h2>Future extensibility</h2>
 * <p>If OrynPlugins ever needs to load external libraries at runtime (e.g., a database
 * driver, a JSON library, or a networking stack), add them here:</p>
 * <pre>{@code
 * @Override
 * public void classloader(final PluginClasspathBuilder builder) {
 *     builder.path(new File(getDataFolder(), "libs/my-library.jar").toPath());
 * }
 * }</pre>
 *
 * @see OrynPlugins The main plugin class instantiated after this loader runs.
 * @see io.papermc.paper.plugin.loader.PluginLoader Paper's PluginLoader interface.
 */
class OrynPluginsLoader implements PluginLoader {

    /**
     * Configures the plugin's classpath before the main plugin class is loaded.
     *
     * <p>Currently no additional classpath entries are needed. This method is kept
     * as a hook for future use when runtime libraries may need to be dynamically
     * added to the plugin's classloader.</p>
     *
     * @param builder the classpath builder used to add JARs and libraries
     */
    @Override
    public void classloader(final PluginClasspathBuilder builder) {
        // All dependencies are bundled into the Shadow JAR at build time.
        // No additional classpath entries are required at runtime.
        //
        // To add a runtime library in the future:
        //   builder.path(new File(hostPlugin.getDataFolder(), "libs/example.jar").toPath());
    }
}
