package net.oryn.mc.orynPlugins.module;

public enum ModuleStatus {
    LOADED("Loaded"),
    ENABLED("Enabled"),
    DISABLED("Disabled"),
    ERRORED("Errored");

    private final String displayName;

    ModuleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
