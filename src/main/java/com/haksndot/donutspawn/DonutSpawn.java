package com.haksndot.donutspawn;

import org.bukkit.plugin.java.JavaPlugin;

public class DonutSpawn extends JavaPlugin {

    private static DonutSpawn instance;
    private SpawnManager spawnManager;
    private ConfigManager configManager;
    private GriefPreventionHook gpHook;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Try to hook into GriefPrevention
        gpHook = new GriefPreventionHook(this);
        if (gpHook.isEnabled()) {
            getLogger().info("GriefPrevention integration enabled!");
        } else {
            getLogger().info("GriefPrevention not found - claim checking disabled.");
        }

        // Initialize spawn manager
        spawnManager = new SpawnManager(this, configManager, gpHook);

        // Register event listener
        getServer().getPluginManager().registerEvents(new RespawnListener(this), this);

        // Register commands
        DonutSpawnCommand command = new DonutSpawnCommand(this);
        getCommand("donutspawn").setExecutor(command);
        getCommand("donutspawn").setTabCompleter(command);

        getLogger().info("DonutSpawn enabled! " + configManager.getZones().size() + " spawn zone(s) loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DonutSpawn disabled.");
    }

    public static DonutSpawn getInstance() {
        return instance;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GriefPreventionHook getGPHook() {
        return gpHook;
    }

    public void reload() {
        reloadConfig();
        configManager.loadConfig();
        getLogger().info("Configuration reloaded! " + configManager.getZones().size() + " spawn zone(s) loaded.");
    }
}
