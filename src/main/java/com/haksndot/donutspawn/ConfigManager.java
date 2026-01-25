package com.haksndot.donutspawn;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final DonutSpawn plugin;
    private List<SpawnZone> zones;
    private int maxAttempts;
    private boolean requireSolidGround;
    private int minY;
    private int maxY;
    private Set<String> blockedBiomes;
    private String messageSpawned;
    private String messageFallback;

    public ConfigManager(DonutSpawn plugin) {
        this.plugin = plugin;
        this.zones = new ArrayList<>();
        this.blockedBiomes = new HashSet<>();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        zones.clear();
        blockedBiomes.clear();

        // Load general settings
        maxAttempts = config.getInt("max-attempts", 50);

        // Load safety settings
        ConfigurationSection safety = config.getConfigurationSection("safety");
        if (safety != null) {
            requireSolidGround = safety.getBoolean("require-solid-ground", true);
            minY = safety.getInt("min-y", 63);
            maxY = safety.getInt("max-y", 255);
            List<String> biomes = safety.getStringList("blocked-biomes");
            blockedBiomes.addAll(biomes);
        } else {
            requireSolidGround = true;
            minY = 63;
            maxY = 255;
        }

        // Load spawn zones
        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection != null) {
            for (String key : zonesSection.getKeys(false)) {
                ConfigurationSection zoneConfig = zonesSection.getConfigurationSection(key);
                if (zoneConfig != null) {
                    try {
                        SpawnZone zone = new SpawnZone(
                                key,
                                zoneConfig.getString("world", "world"),
                                zoneConfig.getDouble("center-x", 0),
                                zoneConfig.getDouble("center-z", 0),
                                zoneConfig.getDouble("inner-radius", 0),
                                zoneConfig.getDouble("outer-radius", 500),
                                zoneConfig.getDouble("weight", 1.0)
                        );
                        zones.add(zone);
                        plugin.getLogger().info("Loaded zone: " + zone);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load zone '" + key + "': " + e.getMessage());
                    }
                }
            }
        }

        // Load messages
        ConfigurationSection messages = config.getConfigurationSection("messages");
        if (messages != null) {
            messageSpawned = colorize(messages.getString("spawned", ""));
            messageFallback = colorize(messages.getString("fallback", ""));
        } else {
            messageSpawned = "";
            messageFallback = "";
        }

        if (zones.isEmpty()) {
            plugin.getLogger().warning("No spawn zones configured! Players will use default world spawn.");
        }
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void addZone(SpawnZone zone) {
        // Add to memory
        zones.add(zone);

        // Save to config
        FileConfiguration config = plugin.getConfig();
        String path = "zones." + zone.getName();
        config.set(path + ".world", zone.getWorldName());
        config.set(path + ".center-x", zone.getCenterX());
        config.set(path + ".center-z", zone.getCenterZ());
        config.set(path + ".inner-radius", zone.getInnerRadius());
        config.set(path + ".outer-radius", zone.getOuterRadius());
        config.set(path + ".weight", zone.getWeight());
        plugin.saveConfig();
    }

    public boolean removeZone(String name) {
        SpawnZone toRemove = null;
        for (SpawnZone zone : zones) {
            if (zone.getName().equalsIgnoreCase(name)) {
                toRemove = zone;
                break;
            }
        }

        if (toRemove != null) {
            zones.remove(toRemove);
            FileConfiguration config = plugin.getConfig();
            config.set("zones." + toRemove.getName(), null);
            plugin.saveConfig();
            return true;
        }
        return false;
    }

    public List<SpawnZone> getZones() {
        return zones;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public boolean isRequireSolidGround() {
        return requireSolidGround;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public Set<String> getBlockedBiomes() {
        return blockedBiomes;
    }

    public String getMessageSpawned() {
        return messageSpawned;
    }

    public String getMessageFallback() {
        return messageFallback;
    }
}
