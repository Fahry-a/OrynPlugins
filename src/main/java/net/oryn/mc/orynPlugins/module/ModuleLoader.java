package net.oryn.mc.orynPlugins.module;

import net.oryn.mc.orynPlugins.OrynPlugins;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

public class ModuleLoader {

    private final OrynPlugins hostPlugin;
    private final File modulesFolder;
    private final Map<String, OrynModule> modules = new LinkedHashMap<>();
    private final Map<String, ModuleClassLoader> classLoaders = new LinkedHashMap<>();

    public ModuleLoader(OrynPlugins hostPlugin) {
        this.hostPlugin = hostPlugin;
        this.modulesFolder = new File(hostPlugin.getDataFolder(), "modules");
        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs();
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
        URL jarUrl = jarFile.toURI().toURL();
        ModuleClassLoader classLoader = new ModuleClassLoader(
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

        File moduleDataFolder = new File(modulesFolder, module.getName());
        if (!moduleDataFolder.exists()) {
            moduleDataFolder.mkdirs();
        }

        ModuleContext context = new ModuleContext(hostPlugin, moduleDataFolder, hostPlugin.getLogger());

        modules.put(module.getName().toLowerCase(), module);
        classLoaders.put(module.getName().toLowerCase(), classLoader);

        hostPlugin.getLogger().info("Loading module: " + module.getName() + " v" + module.getVersion());
        module.onLoad(context);
    }

    public void enableAllModules() {
        for (Map.Entry<String, OrynModule> entry : modules.entrySet()) {
            try {
                hostPlugin.getLogger().info("Enabling module: " + entry.getKey());
                entry.getValue().onEnable();
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + entry.getKey(), e);
            }
        }
    }

    public void disableAllModules() {
        for (Map.Entry<String, OrynModule> entry : modules.entrySet()) {
            try {
                hostPlugin.getLogger().info("Disabling module: " + entry.getKey());
                entry.getValue().onDisable();
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + entry.getKey(), e);
            }
        }

        for (Map.Entry<String, ModuleClassLoader> entry : classLoaders.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                hostPlugin.getLogger().log(Level.WARNING, "Failed to close classloader: " + entry.getKey(), e);
            }
        }

        classLoaders.clear();
        modules.clear();
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

    private static class ModuleClassLoader extends URLClassLoader {

        public ModuleClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    }
}
