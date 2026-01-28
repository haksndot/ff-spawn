package com.haksndot.ffspawn;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class RespawnListener implements Listener {

    private final FFSpawn plugin;

    public RespawnListener(FFSpawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Check bypass permission
        if (player.hasPermission("ffspawn.bypass")) {
            return;
        }

        // Check if player has a bed/respawn anchor spawn
        // If isBedSpawn() or isAnchorSpawn() is true, they have a valid respawn point
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            // Player has a bed or respawn anchor - use normal behavior
            return;
        }

        // Player is "unbedded" - find them a random spawn location
        Location spawnLoc = plugin.getSpawnManager().findSpawnLocation(player);

        if (spawnLoc != null) {
            event.setRespawnLocation(spawnLoc);

            // Send message if configured
            String message = plugin.getConfigManager().getMessageSpawned();
            if (!message.isEmpty()) {
                // Delay message slightly so player sees it after respawning
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(message);
                    }
                }, 20L); // 1 second delay
            }

            plugin.getLogger().info("Random spawned " + player.getName() + " at " +
                    String.format("%.0f, %.0f, %.0f", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
        } else {
            // Failed to find valid spawn - use world spawn (default behavior)
            String message = plugin.getConfigManager().getMessageFallback();
            if (!message.isEmpty()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(message);
                    }
                }, 20L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Only handle End portal exits
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return;
        }

        // Only handle teleports FROM the End to overworld
        if (event.getFrom().getWorld() == null ||
                !event.getFrom().getWorld().getEnvironment().equals(org.bukkit.World.Environment.THE_END)) {
            return;
        }

        Player player = event.getPlayer();

        // Check bypass permission
        if (player.hasPermission("ffspawn.bypass")) {
            return;
        }

        // Check if player has a bed spawn set
        Location bedSpawn = player.getBedSpawnLocation();
        if (bedSpawn != null) {
            // Player has a bed - let them go there
            event.setTo(bedSpawn);
            return;
        }

        // Player is unbedded - find random spawn
        Location spawnLoc = plugin.getSpawnManager().findSpawnLocation(player);

        if (spawnLoc != null) {
            event.setTo(spawnLoc);

            // Send message if configured
            String message = plugin.getConfigManager().getMessageSpawned();
            if (!message.isEmpty()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(message);
                    }
                }, 20L);
            }

            plugin.getLogger().info("Random spawned " + player.getName() + " (End return) at " +
                    String.format("%.0f, %.0f, %.0f", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
        }
        // If no valid spawn found, default behavior will send them to world spawn
    }
}
