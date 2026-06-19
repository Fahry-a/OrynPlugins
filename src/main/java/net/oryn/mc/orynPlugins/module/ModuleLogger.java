package net.oryn.mc.orynPlugins.module;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleLogger extends Logger {

    private final Logger parent;
    private final String prefix;

    public ModuleLogger(Logger parent, String moduleName) {
        super("OrynModules." + moduleName, null);
        this.parent = parent;
        this.prefix = "[" + moduleName + "] ";
    }

    @Override
    public void log(Level level, String msg) {
        parent.log(level, prefix + msg);
    }

    @Override
    public void log(Level level, String msg, Object param1) {
        parent.log(level, prefix + msg, param1);
    }

    @Override
    public void log(Level level, String msg, Object[] params) {
        parent.log(level, prefix + msg, params);
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        parent.log(level, prefix + msg, thrown);
    }

    @Override
    public void severe(String msg) {
        parent.severe(prefix + msg);
    }

    @Override
    public void warning(String msg) {
        parent.warning(prefix + msg);
    }

    @Override
    public void info(String msg) {
        parent.info(prefix + msg);
    }

    @Override
    public void fine(String msg) {
        parent.fine(prefix + msg);
    }

    @Override
    public void finer(String msg) {
        parent.finer(prefix + msg);
    }

    @Override
    public void finest(String msg) {
        parent.finest(prefix + msg);
    }
}
