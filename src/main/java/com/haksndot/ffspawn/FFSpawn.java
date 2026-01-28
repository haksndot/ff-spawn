package com.haksndot.ffspawn;

import org.bukkit.plugin.java.JavaPlugin;

public class FFSpawn extends JavaPlugin {

    private static FFSpawn instance;
    private SpawnManager spawnManager;
    private ConfigManager configManager;
    private GriefPreventionHook gpHook;
    private SpawnBlockManager spawnBlockManager;

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

        // Initialize spawn block manager
        spawnBlockManager = new SpawnBlockManager(this, configManager);
        spawnBlockManager.loadSpawnBlocks();

        // Initialize spawn manager
        spawnManager = new SpawnManager(this, configManager, gpHook);
        spawnManager.setSpawnBlockManager(spawnBlockManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new RespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnBlockListener(this, spawnBlockManager), this);

        // Register commands
        FFSpawnCommand command = new FFSpawnCommand(this);
        getCommand("ffspawn").setExecutor(command);
        getCommand("ffspawn").setTabCompleter(command);

        getLogger().info("FFSpawn enabled! " + configManager.getZones().size() + " zone(s), " +
                spawnBlockManager.getSpawnBlockCount() + " spawn block(s) loaded.");
    }

    @Override
    public void onDisable() {
        if (spawnBlockManager != null) {
            spawnBlockManager.saveSpawnBlocks();
        }
        getLogger().info("FFSpawn disabled.");
    }

    public static FFSpawn getInstance() {
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

    public SpawnBlockManager getSpawnBlockManager() {
        return spawnBlockManager;
    }

    public void reload() {
        reloadConfig();
        configManager.loadConfig();
        spawnBlockManager.loadSpawnBlocks();
        getLogger().info("Configuration reloaded! " + configManager.getZones().size() + " zone(s), " +
                spawnBlockManager.getSpawnBlockCount() + " spawn block(s) loaded.");
    }
}
