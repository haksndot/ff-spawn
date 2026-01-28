package com.haksndot.ffspawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class SpawnBlock {

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final BlockFace direction;
    private final UUID ownerUuid;
    private final String ownerName;
    private final long placedAt;

    public SpawnBlock(Location location, BlockFace direction, UUID ownerUuid, String ownerName, long placedAt) {
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.direction = direction;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.placedAt = placedAt;
    }

    public SpawnBlock(String worldName, int x, int y, int z, BlockFace direction,
                      UUID ownerUuid, String ownerName, long placedAt) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.direction = direction;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.placedAt = placedAt;
    }

    public Location getSpawnLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
        loc.setYaw(directionToYaw(direction));
        loc.setPitch(0);
        return loc;
    }

    public Location getBlockLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    private float directionToYaw(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }

    public String getLocationKey() {
        return worldName + "," + x + "," + y + "," + z;
    }

    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public BlockFace getDirection() { return direction; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public long getPlacedAt() { return placedAt; }
}
