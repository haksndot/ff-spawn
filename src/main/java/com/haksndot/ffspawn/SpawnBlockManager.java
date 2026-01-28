package com.haksndot.ffspawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnBlockManager {

    public static final int CUSTOM_MODEL_DATA = 1002;

    private final FFSpawn plugin;
    private final ConfigManager configManager;
    private final NamespacedKey spawnBlockKey;
    private final NamespacedKey recipeKey;
    private final Map<String, SpawnBlock> spawnBlocks = new HashMap<>();
    private File spawnBlocksFile;

    public SpawnBlockManager(FFSpawn plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.spawnBlockKey = new NamespacedKey(plugin, "spawn_block");
        this.recipeKey = new NamespacedKey(plugin, "spawn_block_recipe");

        if (configManager.isSpawnBlocksEnabled() && configManager.isSpawnBlockRecipeEnabled()) {
            registerRecipe();
        }
    }

    public void loadSpawnBlocks() {
        spawnBlocks.clear();
        spawnBlocksFile = new File(plugin.getDataFolder(), "spawn-blocks.yml");
        if (!spawnBlocksFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(spawnBlocksFile);
        ConfigurationSection blocksSection = yaml.getConfigurationSection("blocks");
        if (blocksSection == null) return;

        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection bs = blocksSection.getConfigurationSection(key);
            if (bs == null) continue;

            try {
                String worldName = bs.getString("world");
                int x = bs.getInt("x");
                int y = bs.getInt("y");
                int z = bs.getInt("z");
                BlockFace direction = BlockFace.valueOf(bs.getString("direction", "SOUTH").toUpperCase());
                String uuidStr = bs.getString("owner-uuid");
                UUID ownerUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
                String ownerName = bs.getString("owner-name", "Unknown");
                long placedAt = bs.getLong("placed-at", System.currentTimeMillis());

                SpawnBlock block = new SpawnBlock(worldName, x, y, z, direction, ownerUuid, ownerName, placedAt);
                spawnBlocks.put(block.getLocationKey(), block);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load spawn block '" + key + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + spawnBlocks.size() + " spawn block(s).");
    }

    public void saveSpawnBlocks() {
        if (spawnBlocksFile == null) {
            spawnBlocksFile = new File(plugin.getDataFolder(), "spawn-blocks.yml");
        }

        YamlConfiguration yaml = new YamlConfiguration();
        int i = 0;
        for (SpawnBlock block : spawnBlocks.values()) {
            String path = "blocks.block" + i++;
            yaml.set(path + ".world", block.getWorldName());
            yaml.set(path + ".x", block.getX());
            yaml.set(path + ".y", block.getY());
            yaml.set(path + ".z", block.getZ());
            yaml.set(path + ".direction", block.getDirection().name());
            if (block.getOwnerUuid() != null) {
                yaml.set(path + ".owner-uuid", block.getOwnerUuid().toString());
            }
            yaml.set(path + ".owner-name", block.getOwnerName());
            yaml.set(path + ".placed-at", block.getPlacedAt());
        }

        try {
            yaml.save(spawnBlocksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save spawn blocks: " + e.getMessage());
        }
    }

    private void registerRecipe() {
        ItemStack item = createSpawnBlockItem();
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, item);

        List<String> shape = configManager.getSpawnBlockRecipeShape();
        if (shape.size() != 3) {
            shape = List.of("GIG", "ILI", "RNR");
        }
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        Map<Character, Material> ingredients = configManager.getSpawnBlockRecipeIngredients();
        if (ingredients.isEmpty()) {
            ingredients = Map.of('G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK,
                    'L', Material.LODESTONE, 'N', Material.NETHER_STAR, 'R', Material.REDSTONE_BLOCK);
        }

        for (Map.Entry<Character, Material> e : ingredients.entrySet()) {
            recipe.setIngredient(e.getKey(), e.getValue());
        }

        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("Registered spawn block crafting recipe");
    }

    public ItemStack createSpawnBlockItem() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Spawn Block")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
                Component.empty(),
                Component.text("Place to create a spawn point")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("for new players.")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Also works as a lodestone!")
                        .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(spawnBlockKey, PersistentDataType.BYTE, (byte) 1);
        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSpawnBlockItem(ItemStack item) {
        if (item == null || item.getType() != Material.LODESTONE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(spawnBlockKey, PersistentDataType.BYTE);
    }

    public void addSpawnBlock(SpawnBlock block) {
        spawnBlocks.put(block.getLocationKey(), block);
        saveSpawnBlocks();
    }

    public SpawnBlock removeSpawnBlock(Location loc) {
        String key = locationKey(loc);
        SpawnBlock removed = spawnBlocks.remove(key);
        if (removed != null) saveSpawnBlocks();
        return removed;
    }

    public boolean isSpawnBlock(Location loc) {
        return spawnBlocks.containsKey(locationKey(loc));
    }

    public SpawnBlock getSpawnBlock(Location loc) {
        return spawnBlocks.get(locationKey(loc));
    }

    public Collection<SpawnBlock> getAllSpawnBlocks() {
        return Collections.unmodifiableCollection(spawnBlocks.values());
    }

    public int getSpawnBlockCount() {
        return spawnBlocks.size();
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public NamespacedKey getSpawnBlockKey() { return spawnBlockKey; }
}
