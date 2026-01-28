package com.haksndot.ffspawn;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnBlockListener implements Listener {

    private final FFSpawn plugin;
    private final SpawnBlockManager manager;

    public SpawnBlockListener(FFSpawn plugin, SpawnBlockManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isSpawnBlocksEnabled()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!manager.isSpawnBlockItem(item)) return;

        if (!player.hasPermission("ffspawn.place")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't have permission to place spawn blocks.");
            return;
        }

        Block block = event.getBlock();
        Location loc = block.getLocation();

        // Validate: solid ground below
        if (!block.getRelative(BlockFace.DOWN).getType().isSolid()) {
            player.sendMessage(ChatColor.RED + "Spawn blocks must be placed on solid ground!");
            event.setCancelled(true);
            return;
        }

        // Validate: 2 blocks of air above
        Block above1 = block.getRelative(BlockFace.UP);
        Block above2 = above1.getRelative(BlockFace.UP);
        if (above1.getType().isSolid() || above2.getType().isSolid()) {
            player.sendMessage(ChatColor.RED + "Spawn blocks need 2 blocks of air above!");
            event.setCancelled(true);
            return;
        }

        BlockFace direction = getCardinalDirection(player.getLocation().getYaw());

        SpawnBlock spawnBlock = new SpawnBlock(loc, direction, player.getUniqueId(),
                player.getName(), System.currentTimeMillis());
        manager.addSpawnBlock(spawnBlock);

        player.sendMessage(ChatColor.GREEN + "Spawn block placed! New players may spawn here.");
        plugin.getLogger().info(player.getName() + " placed spawn block at " +
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (!manager.isSpawnBlock(loc)) return;

        Player player = event.getPlayer();
        SpawnBlock removed = manager.removeSpawnBlock(loc);

        if (removed != null) {
            event.setDropItems(false);
            loc.getWorld().dropItemNaturally(loc, manager.createSpawnBlockItem());
            player.sendMessage(ChatColor.YELLOW + "Spawn block removed.");
            plugin.getLogger().info(player.getName() + " broke spawn block at " +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }

    private BlockFace getCardinalDirection(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }
}
