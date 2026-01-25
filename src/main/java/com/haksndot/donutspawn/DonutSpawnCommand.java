package com.haksndot.donutspawn;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DonutSpawnCommand implements CommandExecutor, TabCompleter {

    private final DonutSpawn plugin;

    public DonutSpawnCommand(DonutSpawn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutspawn.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "addzone" -> handleAddZone(sender, args);
            case "removezone" -> handleRemoveZone(sender, args);
            case "test" -> handleTest(sender, args);
            case "info" -> handleInfo(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== DonutSpawn Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ds reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/ds list" + ChatColor.GRAY + " - List all spawn zones");
        sender.sendMessage(ChatColor.YELLOW + "/ds addzone <name> <inner> <outer> [weight]" + ChatColor.GRAY + " - Add zone at your location");
        sender.sendMessage(ChatColor.YELLOW + "/ds removezone <name>" + ChatColor.GRAY + " - Remove a spawn zone");
        sender.sendMessage(ChatColor.YELLOW + "/ds test [player]" + ChatColor.GRAY + " - Teleport yourself or a player to a random spawn");
        sender.sendMessage(ChatColor.YELLOW + "/ds info" + ChatColor.GRAY + " - Show plugin status");
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "DonutSpawn configuration reloaded! " +
                plugin.getConfigManager().getZones().size() + " zone(s) loaded.");
    }

    private void handleList(CommandSender sender) {
        List<SpawnZone> zones = plugin.getConfigManager().getZones();
        if (zones.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No spawn zones configured.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Spawn Zones (" + zones.size() + ") ===");
        for (SpawnZone zone : zones) {
            sender.sendMessage(ChatColor.YELLOW + zone.getName() + ChatColor.GRAY + ": " +
                    "world=" + zone.getWorldName() + ", " +
                    "center=(" + (int) zone.getCenterX() + ", " + (int) zone.getCenterZ() + "), " +
                    "inner=" + (int) zone.getInnerRadius() + ", " +
                    "outer=" + (int) zone.getOuterRadius() + ", " +
                    "weight=" + zone.getWeight());
        }
    }

    private void handleAddZone(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command must be run by a player.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /ds addzone <name> <inner-radius> <outer-radius> [weight]");
            sender.sendMessage(ChatColor.GRAY + "Example: /ds addzone spawn1 100 500 1.0");
            return;
        }

        String name = args[1];
        double innerRadius;
        double outerRadius;
        double weight = 1.0;

        try {
            innerRadius = Double.parseDouble(args[2]);
            outerRadius = Double.parseDouble(args[3]);
            if (args.length >= 5) {
                weight = Double.parseDouble(args[4]);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format. Radii and weight must be numbers.");
            return;
        }

        if (innerRadius < 0 || outerRadius <= 0) {
            sender.sendMessage(ChatColor.RED + "Radii must be positive numbers.");
            return;
        }

        if (innerRadius >= outerRadius) {
            sender.sendMessage(ChatColor.RED + "Inner radius must be smaller than outer radius.");
            return;
        }

        // Check for duplicate name
        for (SpawnZone z : plugin.getConfigManager().getZones()) {
            if (z.getName().equalsIgnoreCase(name)) {
                sender.sendMessage(ChatColor.RED + "A zone named '" + name + "' already exists.");
                return;
            }
        }

        Location loc = player.getLocation();
        SpawnZone zone = new SpawnZone(
                name,
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockZ(),
                innerRadius,
                outerRadius,
                weight
        );

        plugin.getConfigManager().addZone(zone);
        sender.sendMessage(ChatColor.GREEN + "Created spawn zone '" + name + "' centered at your location!");
        sender.sendMessage(ChatColor.GRAY + "Players will spawn between " + (int) innerRadius +
                " and " + (int) outerRadius + " blocks from here.");
    }

    private void handleRemoveZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ds removezone <name>");
            return;
        }

        String name = args[1];
        if (plugin.getConfigManager().removeZone(name)) {
            sender.sendMessage(ChatColor.GREEN + "Removed spawn zone '" + name + "'.");
        } else {
            sender.sendMessage(ChatColor.RED + "No zone found with name '" + name + "'.");
        }
    }

    private void handleTest(CommandSender sender, String[] args) {
        Player target;

        if (args.length >= 2) {
            // Teleport another player
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found or not online.");
                return;
            }
        } else {
            // Teleport self
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /ds test <player>");
                return;
            }
            target = (Player) sender;
        }

        sender.sendMessage(ChatColor.YELLOW + "Finding random spawn location...");

        Location loc = plugin.getSpawnManager().findSpawnLocation(target);
        if (loc != null) {
            target.teleport(loc);

            String coords = String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());

            if (target.equals(sender)) {
                sender.sendMessage(ChatColor.GREEN + "Teleported to random spawn: " + coords);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to: " + coords);
                target.sendMessage(ChatColor.GREEN + "You were teleported to a random spawn location.");
            }

            // Show claim info to admin
            if (plugin.getGPHook().isEnabled()) {
                boolean inClaim = plugin.getGPHook().isInAnyClaim(loc);
                sender.sendMessage(ChatColor.GRAY + "In claim: " + (inClaim ? "yes" : "no"));
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to find valid spawn location after " +
                    plugin.getConfigManager().getMaxAttempts() + " attempts.");
        }
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== DonutSpawn Info ===");
        sender.sendMessage(ChatColor.YELLOW + "Zones: " + ChatColor.WHITE +
                plugin.getConfigManager().getZones().size());
        sender.sendMessage(ChatColor.YELLOW + "Max attempts: " + ChatColor.WHITE +
                plugin.getConfigManager().getMaxAttempts());
        sender.sendMessage(ChatColor.YELLOW + "GriefPrevention: " + ChatColor.WHITE +
                (plugin.getGPHook().isEnabled() ? "enabled" : "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Require solid ground: " + ChatColor.WHITE +
                plugin.getConfigManager().isRequireSolidGround());
        sender.sendMessage(ChatColor.YELLOW + "Y range: " + ChatColor.WHITE +
                plugin.getConfigManager().getMinY() + " - " + plugin.getConfigManager().getMaxY());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("donutspawn.admin")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "list", "addzone", "removezone", "test", "info");
            String partial = args[0].toLowerCase();
            for (String cmd : subcommands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("removezone")) {
            String partial = args[1].toLowerCase();
            for (SpawnZone zone : plugin.getConfigManager().getZones()) {
                if (zone.getName().toLowerCase().startsWith(partial)) {
                    completions.add(zone.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("addzone")) {
            completions.add("<name>");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("addzone")) {
            completions.add("<inner-radius>");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("addzone")) {
            completions.add("<outer-radius>");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("addzone")) {
            completions.add("1.0");
        }

        return completions;
    }
}
