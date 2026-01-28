package com.haksndot.ffspawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a donut-shaped spawn zone.
 * Players spawn between innerRadius and outerRadius from the center.
 */
public class SpawnZone {

    private final String name;
    private final String worldName;
    private final double centerX;
    private final double centerZ;
    private final double innerRadius;
    private final double outerRadius;
    private final double weight;

    public SpawnZone(String name, String worldName, double centerX, double centerZ,
                     double innerRadius, double outerRadius, double weight) {
        this.name = name;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.weight = weight;
    }

    /**
     * Generate a random point within the donut (between inner and outer radius).
     * Uses uniform distribution across the donut area.
     */
    public Location getRandomLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // For uniform distribution in a donut, we need to account for the area
        // The probability should be proportional to the area at each radius
        // Using the formula: r = sqrt(random * (R2^2 - R1^2) + R1^2)
        double r1Squared = innerRadius * innerRadius;
        double r2Squared = outerRadius * outerRadius;
        double radius = Math.sqrt(random.nextDouble() * (r2Squared - r1Squared) + r1Squared);

        // Random angle
        double angle = random.nextDouble() * 2 * Math.PI;

        // Calculate coordinates
        double x = centerX + radius * Math.cos(angle);
        double z = centerZ + radius * Math.sin(angle);

        // Y will be determined by the SpawnManager (find surface)
        return new Location(world, x, 0, z);
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public double getWeight() {
        return weight;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    @Override
    public String toString() {
        return String.format("SpawnZone{name='%s', world='%s', center=(%.0f, %.0f), inner=%.0f, outer=%.0f, weight=%.2f}",
                name, worldName, centerX, centerZ, innerRadius, outerRadius, weight);
    }
}
