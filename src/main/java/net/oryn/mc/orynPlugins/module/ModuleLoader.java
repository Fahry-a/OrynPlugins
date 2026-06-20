package net.oryn.mc.orynPlugins.module;

import net.oryn.mc.orynPlugins.OrynPlugins;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleLoader {

    private final OrynPlugins hostPlugin;
    private final File modulesFolder;
    private final Map<String, OrynModule> modules = new ConcurrentHashMap<>();
    private final Map<String, ModuleClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final Map<String, ModuleStatus> moduleStatuses = new ConcurrentHashMap<>();
    private final Map<String, Long> loadTimes = new ConcurrentHashMap<>();
    private final Map<String, File> moduleFiles = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> moduleClasses = new ConcurrentHashMap<>();
    private final Map<String, ModuleContext> moduleContexts = new ConcurrentHashMap<>();
    private final ModuleServiceRegistry serviceRegistry;
    private final java.util.List<ModuleLifecycleEvent.Listener> lifecycleListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Object lifecycleLock = new Object();

    public ModuleLoader(OrynPlugins hostPlugin) {
        this.hostPlugin = hostPlugin;
        this.modulesFolder = new File(hostPlugin.getDataFolder(), "modules");
        this.serviceRegistry = new ModuleServiceRegistry(hostPlugin.getLogger());
        if (!modulesFolder.exists() && !modulesFolder.mkdirs()) {
            hostPlugin.getLogger().warning("Failed to create modules directory");
        }
    }

    public void loadAllModules() {
        File[] jarFiles = modulesFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            hostPlugin.getLogger().info("No modules found in modules/ folder");
            return;
        }

        for (File jarFile : jarFiles) {
            try {
                loadModule(jarFile);
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to load module: " + jarFile.getName(), e);
            }
        }

        hostPlugin.getLogger().info("Loaded " + modules.size() + " module(s)");
    }

    @SuppressWarnings("unchecked")
    private void loadModule(File jarFile) throws Exception {
        ModuleClassLoader classLoader = null;
        try {
            URL jarUrl = jarFile.toURI().toURL();
            classLoader = new ModuleClassLoader(
                    new URL[]{jarUrl},
                    hostPlugin.getClass().getClassLoader()
            );

            String mainClassName;
            try (JarFile jar = new JarFile(jarFile)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    throw new IllegalStateException("No MANIFEST.MF in: " + jarFile.getName());
                }
                mainClassName = manifest.getMainAttributes().getValue("Main-Class");
                if (mainClassName == null) {
                    throw new IllegalStateException("No Main-Class in manifest: " + jarFile.getName());
                }
            }

            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Object instance = mainClass.getDeclaredConstructor().newInstance();

            if (!(instance instanceof OrynModule module)) {
                throw new IllegalStateException("Main class does not implement OrynModule: " + mainClassName);
            }

            String moduleName = getModuleName(module, mainClass);
            if (moduleName == null || moduleName.isBlank()) {
                throw new IllegalStateException("Module name cannot be null or empty: " + mainClassName
                    + ". Use @ModuleInfo annotation or implement getName()");
            }

            if (modules.containsKey(moduleName.toLowerCase())) {
                hostPlugin.getLogger().warning("Module name collision: '" + moduleName + "' is already loaded. Skipping duplicate from: " + jarFile.getName());
                closeClassLoader(classLoader);
                return;
            }

            File moduleDataFolder = new File(modulesFolder, moduleName.toLowerCase());
            if (!moduleDataFolder.exists() && !moduleDataFolder.mkdirs()) {
                hostPlugin.getLogger().warning("Failed to create data folder for module: " + moduleName);
            }

            Logger moduleLogger = new ModuleLogger(hostPlugin.getLogger(), moduleName);

            ModuleContext context = new ModuleContext(hostPlugin, moduleDataFolder, moduleLogger, classLoader, serviceRegistry, this);

            String moduleVersion = getModuleVersion(module, mainClass);

            hostPlugin.getLogger().info("Loading module: " + moduleName + " v" + moduleVersion);
            long startTime = System.currentTimeMillis();
            boolean loadSuccess = module.onLoad(context);
            long loadTime = System.currentTimeMillis() - startTime;

            if (!loadSuccess) {
                hostPlugin.getLogger().warning("Module " + moduleName + " returned false from onLoad(), skipping");
                closeClassLoader(classLoader);
                moduleStatuses.put(moduleName.toLowerCase(), ModuleStatus.ERRORED);
                return;
            }

            String key = moduleName.toLowerCase();
            modules.put(key, module);
            classLoaders.put(key, classLoader);
            moduleStatuses.put(key, ModuleStatus.LOADED);
            loadTimes.put(key, loadTime);
            moduleFiles.put(key, jarFile);
            moduleClasses.put(key, mainClass);
            moduleContexts.put(key, context);

            fireLifecycleEvent(moduleName, ModuleLifecycleEvent.LifecycleType.LOADED, null, ModuleStatus.LOADED);
            hostPlugin.getLogger().info("Module " + moduleName + " loaded (" + loadTime + "ms)");

        } catch (Exception e) {
            if (classLoader != null) {
                closeClassLoader(classLoader);
            }
            throw e;
        }
    }

    // ==================== Metadata Helpers ====================

    private String getModuleName(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return module.getName();
    }

    private String getModuleVersion(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.version().isEmpty()) {
            return annotation.version();
        }
        return module.getVersion();
    }

    private String getModuleDescription(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }
        return module.getDescription();
    }

    private String getModuleAuthor(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.author().equals("Unknown")) {
            return annotation.author();
        }
        return module.getAuthor();
    }

    private List<String> getModuleDependencies(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && annotation.dependencies().length > 0) {
            return java.util.Arrays.asList(annotation.dependencies());
        }
        return module.getDependencies();
    }

    private List<String> getModuleSoftDependencies(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && annotation.softDependencies().length > 0) {
            return java.util.Arrays.asList(annotation.softDependencies());
        }
        return module.getSoftDependencies();
    }

    // ==================== Dependency Resolution ====================

    /**
     * Check if all hard dependencies of a module are satisfied.
     */
    private boolean areDependenciesMet(String moduleName) {
        OrynModule module = modules.get(moduleName);
        Class<?> mainClass = moduleClasses.get(moduleName);
        if (module == null || mainClass == null) return false;

        List<String> dependencies = getModuleDependencies(module, mainClass);
        for (String dep : dependencies) {
            if (!modules.containsKey(dep.toLowerCase())) {
                hostPlugin.getLogger().warning("Module '" + moduleName + "' is missing required dependency: " + dep);
                return false;
            }
            ModuleStatus depStatus = moduleStatuses.get(dep.toLowerCase());
            if (depStatus != ModuleStatus.ENABLED) {
                hostPlugin.getLogger().warning("Module '" + moduleName + "' dependency '" + dep + "' is not enabled (status: " + depStatus + ")");
                return false;
            }
        }
        return true;
    }

    /**
     * Topological sort of modules based on hard dependencies.
     * Returns modules in dependency-first order.
     */
    private List<String> getDependencyOrder() {
        List<String> sorted = new ArrayList<>();
        Map<String, Boolean> visited = new LinkedHashMap<>();
        Map<String, Boolean> inProgress = new LinkedHashMap<>();

        for (String name : modules.keySet()) {
            visited.put(name, false);
            inProgress.put(name, false);
        }

        for (String name : modules.keySet()) {
            if (!visited.get(name)) {
                resolveDependencies(name, visited, inProgress, sorted);
            }
        }
        return sorted;
    }

    private void resolveDependencies(String name, Map<String, Boolean> visited,
                                      Map<String, Boolean> inProgress, List<String> sorted) {
        if (visited.getOrDefault(name, false)) return;
        if (inProgress.getOrDefault(name, false)) {
            hostPlugin.getLogger().warning("Circular dependency detected involving module: " + name);
            return;
        }

        inProgress.put(name, true);

        OrynModule module = modules.get(name);
        Class<?> mainClass = moduleClasses.get(name);
        if (module != null && mainClass != null) {
            List<String> deps = getModuleDependencies(module, mainClass);
            for (String dep : deps) {
                String depLower = dep.toLowerCase();
                if (modules.containsKey(depLower) && !visited.getOrDefault(depLower, false)) {
                    resolveDependencies(depLower, visited, inProgress, sorted);
                }
            }
        }

        inProgress.put(name, false);
        visited.put(name, true);
        sorted.add(name);
    }

    // ==================== Enable/Disable/Reload ====================

    public void enableAllModules() {
        synchronized (lifecycleLock) {
            List<String> order = getDependencyOrder();
            for (String name : order) {
                if (moduleStatuses.get(name) == ModuleStatus.LOADED) {
                    enableModule(name);
                }
            }
        }
    }

    public void disableAllModules() {
        synchronized (lifecycleLock) {
            // Disable in reverse dependency order
            List<String> order = getDependencyOrder();
            Collections.reverse(order);

            for (String name : order) {
                if (moduleStatuses.get(name) == ModuleStatus.ENABLED) {
                    disableModule(name);
                }
            }

            // Auto-unregister all events for all modules
            for (Map.Entry<String, ModuleContext> entry : moduleContexts.entrySet()) {
                entry.getValue().unregisterAllEvents();
            }

            for (Map.Entry<String, ModuleClassLoader> entry : classLoaders.entrySet()) {
                closeClassLoader(entry.getValue());
            }

            classLoaders.clear();
            modules.clear();
            moduleStatuses.clear();
            loadTimes.clear();
            moduleFiles.clear();
            moduleClasses.clear();
            moduleContexts.clear();
            serviceRegistry.clear();
            lifecycleListeners.clear();
        }
    }

    public boolean disableModule(String name) {
        synchronized (lifecycleLock) {
            OrynModule module = modules.get(name.toLowerCase());
            if (module == null) return false;

            ModuleStatus status = moduleStatuses.get(name.toLowerCase());
            if (status != ModuleStatus.ENABLED) return false;

            try {
                hostPlugin.getLogger().info("Disabling module: " + name);
                module.onDisable();
                fireLifecycleEvent(name, ModuleLifecycleEvent.LifecycleType.DISABLED, status, ModuleStatus.DISABLED);
                // Auto-unregister all events registered by this module
                ModuleContext ctx = moduleContexts.get(name.toLowerCase());
                if (ctx != null) {
                    ctx.unregisterAllEvents();
                }
                moduleStatuses.put(name.toLowerCase(), ModuleStatus.DISABLED);
                return true;
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + name, e);
                return false;
            }
        }
    }

    public boolean enableModule(String name) {
        synchronized (lifecycleLock) {
            String key = name.toLowerCase();
            OrynModule module = modules.get(key);
            if (module == null) return false;

            ModuleStatus status = moduleStatuses.get(key);
            if (status != ModuleStatus.LOADED && status != ModuleStatus.DISABLED) return false;

            // Check hard dependencies
            if (!areDependenciesMet(key)) {
                hostPlugin.getLogger().warning("Cannot enable module '" + name + "': dependencies not met");
                moduleStatuses.put(key, ModuleStatus.ERRORED);
                return false;
            }

            try {
                hostPlugin.getLogger().info("Enabling module: " + name);
                module.onEnable();
                fireLifecycleEvent(name, ModuleLifecycleEvent.LifecycleType.ENABLED, status, ModuleStatus.ENABLED);
                moduleStatuses.put(key, ModuleStatus.ENABLED);
                return true;
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + name, e);
                moduleStatuses.put(key, ModuleStatus.ERRORED);
                fireLifecycleEvent(name, ModuleLifecycleEvent.LifecycleType.ERRORED, status, ModuleStatus.ERRORED);
                return false;
            }
        }
    }

    /**
     * Reload a module by fully unloading and re-loading from disk.
     * If the JAR was updated, the new version will be loaded.
     * If the JAR is unchanged, falls back to onReload().
     *
     * @param name Module name
     * @return true if reload succeeded
     */
    public boolean reloadModule(String name) {
        synchronized (lifecycleLock) {
            String key = name.toLowerCase();
            OrynModule module = modules.get(key);
            if (module == null) return false;

            ModuleStatus status = moduleStatuses.get(key);
            if (status != ModuleStatus.ENABLED && status != ModuleStatus.DISABLED) return false;

            File moduleFile = moduleFiles.get(key);
            if (moduleFile == null || !moduleFile.exists()) {
                // No JAR file recorded, fall back to onReload()
                try {
                    hostPlugin.getLogger().info("Reloading module: " + name);
                    module.onReload();
                    moduleStatuses.put(key, ModuleStatus.ENABLED);
                    return true;
                } catch (Exception e) {
                    hostPlugin.getLogger().log(Level.SEVERE, "Failed to reload module: " + name, e);
                    moduleStatuses.put(key, ModuleStatus.ERRORED);
                    return false;
                }
            }

            // Full reload: unload old, reload from disk
            hostPlugin.getLogger().info("Reloading module from disk: " + name);
            ModuleStatus previousStatus = status;

            try {
                // 1. Disable
                if (status == ModuleStatus.ENABLED) {
                    module.onDisable();
                }
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.WARNING, "Error disabling module during reload: " + name, e);
            }

            // 2. Close old classloader
            ModuleClassLoader oldLoader = classLoaders.remove(key);
            if (oldLoader != null) {
                closeClassLoader(oldLoader);
            }

            // Unregister events for old module
            ModuleContext oldCtx = moduleContexts.remove(key);
            if (oldCtx != null) {
                oldCtx.unregisterAllEvents();
            }

            // 3. Remove old module data
            modules.remove(key);
            moduleStatuses.remove(key);
            loadTimes.remove(key);
            moduleFiles.remove(key);
            moduleClasses.remove(key);

            try {
                // 4. Reload from disk
                loadModule(moduleFile);

                // 5. Enable if it was previously enabled
                if (previousStatus == ModuleStatus.ENABLED) {
                    return enableModule(name);
                }

                return true;
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to reload module: " + name, e);
                moduleStatuses.put(key, ModuleStatus.ERRORED);
                fireLifecycleEvent(name, ModuleLifecycleEvent.LifecycleType.ERRORED, status, ModuleStatus.ERRORED);
                return false;
            }
        }
    }

    // ==================== Queries ====================

    public OrynModule getModule(String name) {
        return modules.get(name.toLowerCase());
    }

    public Collection<OrynModule> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public boolean hasModule(String name) {
        return modules.containsKey(name.toLowerCase());
    }

    public ModuleStatus getModuleStatus(String name) {
        return moduleStatuses.getOrDefault(name.toLowerCase(), null);
    }

    public long getModuleLoadTime(String name) {
        return loadTimes.getOrDefault(name.toLowerCase(), 0L);
    }

    public File getModuleFile(String name) {
        return moduleFiles.get(name.toLowerCase());
    }

    /**
     * Add a listener for module lifecycle events.
     */
    public void addLifecycleListener(ModuleLifecycleEvent.Listener listener) {
        lifecycleListeners.add(listener);
    }

    /**
     * Remove a lifecycle listener.
     */
    public void removeLifecycleListener(ModuleLifecycleEvent.Listener listener) {
        lifecycleListeners.remove(listener);
    }

    private void fireLifecycleEvent(String moduleName, ModuleLifecycleEvent.LifecycleType type,
                                     ModuleStatus previousStatus, ModuleStatus newStatus) {
        ModuleLifecycleEvent event = new ModuleLifecycleEvent(moduleName, type, previousStatus, newStatus);
        for (ModuleLifecycleEvent.Listener listener : lifecycleListeners) {
            try {
                listener.onLifecycleEvent(event);
            } catch (Exception e) {
                hostPlugin.getLogger().warning("Lifecycle listener error for module '" + moduleName + "': " + e.getMessage());
            }
        }
    }

    public ModuleServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public List<String> getModuleDependencies(String name) {
        OrynModule module = modules.get(name.toLowerCase());
        Class<?> mainClass = moduleClasses.get(name.toLowerCase());
        if (module == null || mainClass == null) return Collections.emptyList();
        return getModuleDependencies(module, mainClass);
    }

    public List<String> getModuleSoftDependencies(String name) {
        OrynModule module = modules.get(name.toLowerCase());
        Class<?> mainClass = moduleClasses.get(name.toLowerCase());
        if (module == null || mainClass == null) return Collections.emptyList();
        return getModuleSoftDependencies(module, mainClass);
    }

    // ==================== Hot-Detect ====================

    /**
     * Detect and load new modules from the modules folder.
     * Directly attempts to load any unloaded JARs without a pre-scan.
     *
     * @return List of newly loaded module names
     */
    public List<String> detectNewModules() {
        List<String> newModules = new ArrayList<>();

        File[] jarFiles = modulesFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return newModules;
        }

        for (File jarFile : jarFiles) {
            try {
                // Get module name from JAR manifest without loading the class
                String moduleName = getModuleNameFromManifest(jarFile);
                if (moduleName == null) {
                    continue;
                }

                // Skip if already loaded
                if (modules.containsKey(moduleName.toLowerCase())) {
                    continue;
                }

                // Load the new module
                loadModule(jarFile);
                newModules.add(moduleName);
                hostPlugin.getLogger().info("Detected and loaded new module: " + moduleName);

            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.WARNING, "Failed to detect module: " + jarFile.getName(), e);
            }
        }

        return newModules;
    }

    /**
     * Read Main-Class from JAR manifest without loading the class.
     * Returns the Main-Class name for validation purposes only.
     * This avoids creating a temporary classloader and the associated leak.
     */
    private String getModuleNameFromManifest(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) return null;

            // Prefer Module-Name manifest attribute (avoids loading the class)
            String moduleName = manifest.getMainAttributes().getValue("Module-Name");
            if (moduleName != null && !moduleName.isBlank()) {
                return moduleName.trim();
            }

            // Fallback: read Main-Class and validate
            String mainClassName = manifest.getMainAttributes().getValue("Main-Class");
            if (mainClassName == null) return null;

            String classPath = mainClassName.replace('.', '/') + ".class";
            if (jar.getEntry(classPath) == null) {
                hostPlugin.getLogger().warning("Main-Class '" + mainClassName + "' not found in JAR: " + jarFile.getName());
                return null;
            }

            return mainClassName;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== ClassLoader ====================

    private void closeClassLoader(ClassLoader cl) {
        if (cl instanceof URLClassLoader urlCl) {
            try {
                urlCl.close();
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.WARNING, "Failed to close classloader", e);
            }
        }
    }

    /**
     * Module classloader that delegates to the parent (host plugin) classloader.
     * Supports resource loading from the module JAR.
     */
    static class ModuleClassLoader extends URLClassLoader {

        public ModuleClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }
}
