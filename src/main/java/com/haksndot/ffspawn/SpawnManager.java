package com.haksndot.ffspawn;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {

    private final FFSpawn plugin;
    private final ConfigManager config;
    private final GriefPreventionHook gpHook;
    private SpawnBlockManager spawnBlockManager;

    public SpawnManager(FFSpawn plugin, ConfigManager config, GriefPreventionHook gpHook) {
        this.plugin = plugin;
        this.config = config;
        this.gpHook = gpHook;
    }

    public void setSpawnBlockManager(SpawnBlockManager manager) {
        this.spawnBlockManager = manager;
    }

    /**
     * Find a valid random spawn location for a player.
     * Returns null if no valid location found after max attempts.
     */
    public Location findSpawnLocation(Player player) {
        List<SpawnZone> zones = config.getZones();
        Collection<SpawnBlock> spawnBlocks = (config.isSpawnBlocksEnabled() && spawnBlockManager != null)
                ? spawnBlockManager.getAllSpawnBlocks() : List.of();

        if (zones.isEmpty() && spawnBlocks.isEmpty()) {
            return null;
        }

        int maxAttempts = config.getMaxAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Select either a zone or spawn block based on weights
            Object selected = selectWeightedSpawnPoint(zones, spawnBlocks);

            if (selected instanceof SpawnZone zone) {
                Location loc = attemptZoneSpawn(zone, player);
                if (loc != null) return loc;
            } else if (selected instanceof SpawnBlock block) {
                Location loc = attemptSpawnBlockSpawn(block);
                if (loc != null) return loc;
            }
        }

        // Failed to find valid location
        plugin.getLogger().warning("Failed to find valid spawn for " + player.getName() +
                " after " + maxAttempts + " attempts. Using world spawn.");
        return null;
    }

    private Location attemptZoneSpawn(SpawnZone zone, Player player) {
        if (zone.getWorld() == null) return null;

        Location loc = zone.getRandomLocation();
        if (loc == null) return null;

        loc = findSafeY(loc);
        if (loc == null) return null;

        if (isBiomeBlocked(loc)) return null;
        if (gpHook.isEnabled() && gpHook.isInClaim(loc, player)) return null;
        if (config.isRequireSolidGround() && !isSafeLocation(loc)) return null;

        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        return loc;
    }

    private Location attemptSpawnBlockSpawn(SpawnBlock block) {
        Location loc = block.getSpawnLocation();
        if (loc == null || loc.getWorld() == null) return null;

        // Verify block still exists
        Block worldBlock = loc.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ());
        if (worldBlock.getType() != Material.LODESTONE) {
            plugin.getLogger().warning("Spawn block at " + block.getLocationKey() + " no longer exists");
            spawnBlockManager.removeSpawnBlock(worldBlock.getLocation());
            return null;
        }

        // Check safety at spawn location
        if (!isSpawnBlockLocationSafe(loc)) return null;

        return loc;
    }

    private Object selectWeightedSpawnPoint(List<SpawnZone> zones, Collection<SpawnBlock> spawnBlocks) {
        double sbWeight = config.getSpawnBlockWeight();
        List<WeightedEntry> entries = new ArrayList<>();

        for (SpawnZone zone : zones) {
            entries.add(new WeightedEntry(zone, zone.getWeight()));
        }
        for (SpawnBlock block : spawnBlocks) {
            entries.add(new WeightedEntry(block, sbWeight));
        }

        if (entries.isEmpty()) return null;
        if (entries.size() == 1) return entries.get(0).object;

        double total = entries.stream().mapToDouble(e -> e.weight).sum();
        double rand = ThreadLocalRandom.current().nextDouble() * total;
        double cumulative = 0;

        for (WeightedEntry entry : entries) {
            cumulative += entry.weight;
            if (rand < cumulative) return entry.object;
        }
        return entries.get(entries.size() - 1).object;
    }

    private boolean isSpawnBlockLocationSafe(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);

        if (feet.getType().isSolid() || head.getType().isSolid()) return false;
        if (isDangerous(feet.getType()) || isDangerous(head.getType())) return false;

        // Check for lava nearby
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (world.getBlockAt(x + dx, y, z + dz).getType() == Material.LAVA) return false;
            }
        }
        return true;
    }

    private static class WeightedEntry {
        final Object object;
        final double weight;
        WeightedEntry(Object o, double w) { this.object = o; this.weight = w; }
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
