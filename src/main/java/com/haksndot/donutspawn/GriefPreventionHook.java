package com.haksndot.donutspawn;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Hooks into GriefPrevention to check if spawn locations are inside claims.
 * Gracefully handles GriefPrevention not being installed.
 */
public class GriefPreventionHook {

    private final DonutSpawn plugin;
    private boolean enabled;
    private GriefPrevention griefPrevention;

    public GriefPreventionHook(DonutSpawn plugin) {
        this.plugin = plugin;
        this.enabled = false;

        try {
            Plugin gpPlugin = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
            if (gpPlugin != null && gpPlugin.isEnabled()) {
                griefPrevention = (GriefPrevention) gpPlugin;
                enabled = true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into GriefPrevention: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * Check if GriefPrevention integration is active.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if a location is inside a claim that doesn't belong to the player.
     *
     * @param location The location to check
     * @param player   The player who would spawn here (null to check any claim)
     * @return true if the location is in another player's claim
     */
    public boolean isInClaim(Location location, Player player) {
        if (!enabled || griefPrevention == null) {
            return false;
        }

        try {
            Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);

            if (claim == null) {
                // No claim at this location - safe to spawn
                return false;
            }

            // There is a claim here
            if (player == null) {
                // No player context - consider it blocked
                return true;
            }

            // Check if the player owns this claim or is trusted
            // ownerID is null for admin claims
            if (claim.ownerID == null) {
                // Admin claim - don't spawn here
                return true;
            }

            if (claim.ownerID.equals(player.getUniqueId())) {
                // Player owns this claim - OK to spawn
                return false;
            }

            // Check if player has trust
            // allowAccess returns null if player has access, error message otherwise
            String accessDenied = claim.allowAccess(player);
            if (accessDenied == null) {
                // Player has access - OK to spawn
                return false;
            }

            // Player doesn't have access to this claim
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking GriefPrevention claim: " + e.getMessage());
            // On error, assume it's safe (don't block spawning)
            return false;
        }
    }

    /**
     * Check if a location is inside any claim at all.
     */
    public boolean isInAnyClaim(Location location) {
        if (!enabled || griefPrevention == null) {
            return false;
        }

        try {
            Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
            return claim != null;
        } catch (Exception e) {
            return false;
        }
    }
}
