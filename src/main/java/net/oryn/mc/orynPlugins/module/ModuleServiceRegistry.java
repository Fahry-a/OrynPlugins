package net.oryn.mc.orynPlugins.module;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Simple service registry for inter-module communication.
 * 
 * Modules can register services (API implementations) that other modules can consume.
 * This provides a clean way for modules to share functionality without direct coupling.
 * 
 * Usage:
 * <pre>
 * // Module A provides a service
 * context.getServiceRegistry().register("vault-bridge", new VaultBridgeImpl());
 * 
 * // Module B consumes the service
 * Optional&lt;VaultBridge&gt; bridge = context.getServiceRegistry().get("vault-bridge", VaultBridge.class);
 * </pre>
 */
public class ModuleServiceRegistry {

    private final Map<String, ServiceEntry> services = new ConcurrentHashMap<>();
    private final Logger logger;

    public ModuleServiceRegistry(Logger logger) {
        this.logger = logger;
    }

    /**
     * Register a service with a unique name.
     */
    public <T> void register(String name, T service) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be null or blank");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service instance cannot be null");
        }

        ServiceEntry existing = services.put(name.toLowerCase(), new ServiceEntry(service, service.getClass()));
        if (existing != null) {
            logger.warning("Service '" + name + "' was replaced (old type: " + existing.type().getSimpleName() + ")");
        }
    }

    /**
     * Get a service by name and type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String name, Class<T> type) {
        ServiceEntry entry = services.get(name.toLowerCase());
        if (entry == null) {
            return Optional.empty();
        }
        if (!type.isInstance(entry.service())) {
            return Optional.empty();
        }
        return Optional.of((T) entry.service());
    }

    /**
     * Get a service by name (no type check).
     */
    public Optional<Object> get(String name) {
        ServiceEntry entry = services.get(name.toLowerCase());
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(entry.service());
    }

    /**
     * Check if a service is registered.
     */
    public boolean isRegistered(String name) {
        return services.containsKey(name.toLowerCase());
    }

    /**
     * Unregister a service.
     */
    public boolean unregister(String name) {
        return services.remove(name.toLowerCase()) != null;
    }

    /**
     * Get all registered service names.
     */
    public Set<String> getRegisteredServices() {
        return Collections.unmodifiableSet(services.keySet());
    }

    /**
     * Clear all registered services.
     */
    public void clear() {
        services.clear();
    }

    private static final class ServiceEntry {
        private final Object service;
        private final Class<?> type;

        ServiceEntry(Object service, Class<?> type) {
            this.service = service;
            this.type = type;
        }

        Object service() { return service; }
        Class<?> type() { return type; }
    }
}
