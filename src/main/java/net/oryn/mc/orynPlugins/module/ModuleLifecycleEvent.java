package net.oryn.mc.orynPlugins.module;

/**
 * Represents a module lifecycle event that can be listened to by other modules.
 * 
 * Modules can register a {@link Listener} to be notified when other modules
 * are loaded, enabled, disabled, or reloaded.
 * 
 * Usage:
 * <pre>
 * public class MyModule implements OrynModule {
 *     {@literal @}Override
 *     public boolean onLoad(ModuleContext context) {
 *         context.getModuleLoader().addLifecycleListener(event -&gt; {
 *             if (event.type() == LifecycleType.ENABLED &amp;&amp; event.moduleName().equals("vault")) {
 *                 context.getLogger().info("Vault module just enabled!");
 *                 // Connect to vault...
 *             }
 *         });
 *         return true;
 *     }
 * }
 * </pre>
 */
public record ModuleLifecycleEvent(
    String moduleName,
    LifecycleType type,
    ModuleStatus previousStatus,
    ModuleStatus newStatus
) {

    /**
     * Types of lifecycle events.
     */
    public enum LifecycleType {
        LOADED,
        ENABLED,
        DISABLED,
        RELOADED,
        ERRORED
    }

    /**
     * Listener interface for module lifecycle events.
     */
    @FunctionalInterface
    public interface Listener {
        void onLifecycleEvent(ModuleLifecycleEvent event);
    }
}
