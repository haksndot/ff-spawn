package com.haksndot.donutspawn;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {

    private final DonutSpawn plugin;
    private final ConfigManager config;
    private final GriefPreventionHook gpHook;

    public SpawnManager(DonutSpawn plugin, ConfigManager config, GriefPreventionHook gpHook) {
        this.plugin = plugin;
        this.config = config;
        this.gpHook = gpHook;
    }

    /**
     * Find a valid random spawn location for a player.
     * Returns null if no valid location found after max attempts.
     */
    public Location findSpawnLocation(Player player) {
        List<SpawnZone> zones = config.getZones();
        if (zones.isEmpty()) {
            return null;
        }

        int maxAttempts = config.getMaxAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Select a random zone (weighted)
            SpawnZone zone = selectRandomZone(zones);
            if (zone == null || zone.getWorld() == null) {
                continue;
            }

            // Get random location in the donut
            Location loc = zone.getRandomLocation();
            if (loc == null) {
                continue;
            }

            // Find safe Y coordinate
            loc = findSafeY(loc);
            if (loc == null) {
                continue;
            }

            // Check biome
            if (isBiomeBlocked(loc)) {
                continue;
            }

            // Check GriefPrevention claims
            if (gpHook.isEnabled() && gpHook.isInClaim(loc, player)) {
                continue;
            }

            // Check if spawn location is safe
            if (config.isRequireSolidGround() && !isSafeLocation(loc)) {
                continue;
            }

            // Found a valid location!
            // Center on block and add small offset for player placement
            loc.setX(loc.getBlockX() + 0.5);
            loc.setZ(loc.getBlockZ() + 0.5);
            return loc;
        }

        // Failed to find valid location
        plugin.getLogger().warning("Failed to find valid spawn for " + player.getName() +
                " after " + maxAttempts + " attempts. Using world spawn.");
        return null;
    }

    /**
     * Select a random zone based on weights.
     */
    private SpawnZone selectRandomZone(List<SpawnZone> zones) {
        if (zones.size() == 1) {
            return zones.get(0);
        }

        // Calculate total weight
        double totalWeight = 0;
        for (SpawnZone zone : zones) {
            totalWeight += zone.getWeight();
        }

        // Select random based on weight
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;

        for (SpawnZone zone : zones) {
            cumulative += zone.getWeight();
            if (random < cumulative) {
                return zone;
            }
        }

        return zones.get(zones.size() - 1);
    }

    /**
     * Find a safe Y coordinate at the given X/Z location.
     */
    private Location findSafeY(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }

        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // Use heightmap to find the surface
        int surfaceY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);

        // Respect Y limits from config
        int minY = config.getMinY();
        int maxY = config.getMaxY();

        if (surfaceY < minY) {
            // Surface is below minimum - this might be a cave or ocean floor
            // Try to find a safe spot above
            for (int y = minY; y <= Math.min(maxY, world.getMaxHeight() - 2); y++) {
                Block block = world.getBlockAt(x, y, z);
                Block above = world.getBlockAt(x, y + 1, z);
                Block below = world.getBlockAt(x, y - 1, z);

                if (below.getType().isSolid() &&
                        !block.getType().isSolid() && block.getType() != Material.LAVA &&
                        !above.getType().isSolid()) {
                    loc.setY(y);
                    return loc;
                }
            }
            return null;
        }

        if (maxY < 255 && surfaceY > maxY) {
            // Surface is above maximum (like a mountain)
            return null;
        }

        // Use the surface
        loc.setY(surfaceY + 1);
        return loc;
    }

    /**
     * Check if the biome at this location is blocked.
     */
    private boolean isBiomeBlocked(Location loc) {
        if (config.getBlockedBiomes().isEmpty()) {
            return false;
        }

        String biomeName = loc.getWorld().getBiome(loc).getKey().value().toUpperCase();
        return config.getBlockedBiomes().contains(biomeName);
    }

    /**
     * Check if the location is safe for spawning (solid ground, no hazards).
     */
    private boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        // Check ground is solid
        if (!ground.getType().isSolid()) {
            return false;
        }

        // Check feet and head positions are not solid or dangerous
        if (feet.getType().isSolid() || head.getType().isSolid()) {
            return false;
        }

        // Check for hazards
        if (isDangerous(feet.getType()) || isDangerous(head.getType()) || isDangerous(ground.getType())) {
            return false;
        }

        return true;
    }

    /**
     * Check if a material is dangerous to spawn in/on.
     */
    private boolean isDangerous(Material material) {
        return switch (material) {
            case LAVA, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE, MAGMA_BLOCK,
                    CACTUS, SWEET_BERRY_BUSH, WITHER_ROSE, POWDER_SNOW -> true;
            default -> false;
        };
    }
}
