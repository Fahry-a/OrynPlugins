package net.oryn.mc.orynPlugins.module;

import net.oryn.mc.orynPlugins.OrynPlugins;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    public ModuleLoader(OrynPlugins hostPlugin) {
        this.hostPlugin = hostPlugin;
        this.modulesFolder = new File(hostPlugin.getDataFolder(), "modules");
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

            // Get module name from @ModuleInfo annotation or getName() method
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

            ModuleContext context = new ModuleContext(hostPlugin, moduleDataFolder, moduleLogger);

            // Get version from @ModuleInfo annotation or getVersion() method
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

            modules.put(moduleName.toLowerCase(), module);
            classLoaders.put(moduleName.toLowerCase(), classLoader);
            moduleStatuses.put(moduleName.toLowerCase(), ModuleStatus.LOADED);
            loadTimes.put(moduleName.toLowerCase(), loadTime);

            hostPlugin.getLogger().info("Module " + moduleName + " loaded (" + loadTime + "ms)");

        } catch (Exception e) {
            if (classLoader != null) {
                closeClassLoader(classLoader);
            }
            throw e;
        }
    }

    /**
     * Get module name from @ModuleInfo annotation or getName() method
     */
    private String getModuleName(OrynModule module, Class<?> mainClass) {
        // Check @ModuleInfo annotation first
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        // Fall back to getName() method
        return module.getName();
    }

    /**
     * Get module version from @ModuleInfo annotation or getVersion() method
     */
    private String getModuleVersion(OrynModule module, Class<?> mainClass) {
        // Check @ModuleInfo annotation first
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.version().isEmpty()) {
            return annotation.version();
        }
        // Fall back to getVersion() method
        return module.getVersion();
    }

    /**
     * Get module description from @ModuleInfo annotation or getDescription() method
     */
    private String getModuleDescription(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }
        return module.getDescription();
    }

    /**
     * Get module author from @ModuleInfo annotation or getAuthor() method
     */
    private String getModuleAuthor(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && !annotation.author().equals("Unknown")) {
            return annotation.author();
        }
        return module.getAuthor();
    }

    /**
     * Get module dependencies from @ModuleInfo annotation or getDependencies() method
     */
    private java.util.List<String> getModuleDependencies(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && annotation.dependencies().length > 0) {
            return java.util.Arrays.asList(annotation.dependencies());
        }
        return module.getDependencies();
    }

    /**
     * Get module soft dependencies from @ModuleInfo annotation or getSoftDependencies() method
     */
    private java.util.List<String> getModuleSoftDependencies(OrynModule module, Class<?> mainClass) {
        ModuleInfo annotation = mainClass.getAnnotation(ModuleInfo.class);
        if (annotation != null && annotation.softDependencies().length > 0) {
            return java.util.Arrays.asList(annotation.softDependencies());
        }
        return module.getSoftDependencies();
    }

    public void enableAllModules() {
        for (Map.Entry<String, OrynModule> entry : modules.entrySet()) {
            String name = entry.getKey();
            if (moduleStatuses.get(name) != ModuleStatus.LOADED) {
                continue;
            }
            try {
                hostPlugin.getLogger().info("Enabling module: " + name);
                entry.getValue().onEnable();
                moduleStatuses.put(name, ModuleStatus.ENABLED);
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + name, e);
                moduleStatuses.put(name, ModuleStatus.ERRORED);
            }
        }
    }

    public void disableAllModules() {
        for (Map.Entry<String, OrynModule> entry : modules.entrySet()) {
            try {
                hostPlugin.getLogger().info("Disabling module: " + entry.getKey());
                entry.getValue().onDisable();
                moduleStatuses.put(entry.getKey(), ModuleStatus.DISABLED);
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + entry.getKey(), e);
            }
        }

        for (Map.Entry<String, ModuleClassLoader> entry : classLoaders.entrySet()) {
            closeClassLoader(entry.getValue());
        }

        classLoaders.clear();
        modules.clear();
        moduleStatuses.clear();
        loadTimes.clear();
    }

    public boolean disableModule(String name) {
        OrynModule module = modules.get(name.toLowerCase());
        if (module == null) {
            return false;
        }

        ModuleStatus status = moduleStatuses.get(name.toLowerCase());
        if (status != ModuleStatus.ENABLED) {
            return false;
        }

        try {
            hostPlugin.getLogger().info("Disabling module: " + name);
            module.onDisable();
            moduleStatuses.put(name.toLowerCase(), ModuleStatus.DISABLED);
            return true;
        } catch (Exception e) {
            hostPlugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + name, e);
            return false;
        }
    }

    public boolean enableModule(String name) {
        OrynModule module = modules.get(name.toLowerCase());
        if (module == null) {
            return false;
        }

        ModuleStatus status = moduleStatuses.get(name.toLowerCase());
        if (status != ModuleStatus.LOADED && status != ModuleStatus.DISABLED) {
            return false;
        }

        try {
            hostPlugin.getLogger().info("Enabling module: " + name);
            module.onEnable();
            moduleStatuses.put(name.toLowerCase(), ModuleStatus.ENABLED);
            return true;
        } catch (Exception e) {
            hostPlugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + name, e);
            moduleStatuses.put(name.toLowerCase(), ModuleStatus.ERRORED);
            return false;
        }
    }

    /**
     * Reload a module by calling its onReload() method.
     * Default implementation calls onDisable() then onEnable().
     * 
     * @param name Module name
     * @return true if reload succeeded
     */
    public boolean reloadModule(String name) {
        OrynModule module = modules.get(name.toLowerCase());
        if (module == null) {
            return false;
        }

        ModuleStatus status = moduleStatuses.get(name.toLowerCase());
        if (status != ModuleStatus.ENABLED) {
            return false;
        }

        try {
            hostPlugin.getLogger().info("Reloading module: " + name);
            module.onReload();
            moduleStatuses.put(name.toLowerCase(), ModuleStatus.ENABLED);
            return true;
        } catch (Exception e) {
            hostPlugin.getLogger().log(Level.SEVERE, "Failed to reload module: " + name, e);
            moduleStatuses.put(name.toLowerCase(), ModuleStatus.ERRORED);
            return false;
        }
    }

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

    private void closeClassLoader(ClassLoader cl) {
        if (cl instanceof URLClassLoader urlCl) {
            try {
                urlCl.close();
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.WARNING, "Failed to close classloader", e);
            }
        }
    }

    private static class ModuleClassLoader extends URLClassLoader {

        public ModuleClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }
}
