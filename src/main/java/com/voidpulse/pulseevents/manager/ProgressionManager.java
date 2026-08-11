package com.voidpulse.pulseevents.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Tracks persistent Pulse Points and levels earned by players for taking part in events.
 * Data is stored in playerdata.yml and kept in memory while the server runs.
 */
public class ProgressionManager {

    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final Map<UUID, Integer> points = new HashMap<>();
    private final File dataFile;
    private BukkitTask autosaveTask;
    private boolean dirty;

    public ProgressionManager(JavaPlugin plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("progression.enabled", true);
    }

    public void startAutosave() {
        stopAutosave();

        int intervalMinutes = Math.max(1, plugin.getConfig().getInt("progression.autosave-interval-minutes", 5));
        long intervalTicks = intervalMinutes * 60L * 20L;

        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (dirty) {
                saveAll();
            }
        }, intervalTicks, intervalTicks);
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public void addPoints(Player player, int amount, String reasonKey) {
        if (player == null || amount <= 0 || !isEnabled()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int oldPoints = points.getOrDefault(playerId, 0);
        int oldLevel = getLevel(oldPoints);

        int newPoints = oldPoints + amount;
        points.put(playerId, newPoints);
        dirty = true;

        int newLevel = getLevel(newPoints);
        if (newLevel > oldLevel) {
            for (int level = oldLevel + 1; level <= newLevel; level++) {
                onLevelUp(player, level);
            }
        }
    }

    public int getPoints(Player player) {
        return player == null ? 0 : points.getOrDefault(player.getUniqueId(), 0);
    }

    public int getPoints(UUID playerId) {
        return playerId == null ? 0 : points.getOrDefault(playerId, 0);
    }

    public int getLevel(Player player) {
        return getLevel(getPoints(player));
    }

    public int getLevel(int currentPoints) {
        TreeMap<Integer, Integer> thresholds = getLevelThresholds();
        int level = 1;

        for (Map.Entry<Integer, Integer> entry : thresholds.entrySet()) {
            if (currentPoints >= entry.getValue()) {
                level = entry.getKey();
            }
        }

        return level;
    }

    public int getPointsForNextLevel(Player player) {
        return getPointsForNextLevel(getLevel(player));
    }

    public int getPointsForNextLevel(int currentLevel) {
        TreeMap<Integer, Integer> thresholds = getLevelThresholds();
        Integer nextThreshold = thresholds.get(currentLevel + 1);
        return nextThreshold == null ? -1 : nextThreshold;
    }

    public List<Map.Entry<UUID, Integer>> getTopPointsEntries(int limit) {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(points.entrySet());
        sorted.sort(Comparator.<Map.Entry<UUID, Integer>>comparingInt(Map.Entry::getValue).reversed());

        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public String resolveName(UUID playerId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        String name = offlinePlayer.getName();
        return name == null ? playerId.toString() : name;
    }

    public void load() {
        points.clear();

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.isConfigurationSection("players")) {
            return;
        }

        for (String key : data.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                int value = data.getInt("players." + key + ".points", 0);
                points.put(playerId, value);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid player UUID in playerdata.yml: " + key);
            }
        }
    }

    public void saveAll() {
        YamlConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
            data.set("players." + entry.getKey() + ".points", entry.getValue());
        }

        try {
            data.save(dataFile);
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save playerdata.yml: " + exception.getMessage());
        }
    }

    private void onLevelUp(Player player, int level) {
        player.sendMessage(lang.getWithPrefix(
                "progression.level-up",
                "%level%",
                String.valueOf(level)
        ));

        LevelReward reward = parseLevelReward(level);
        if (reward != null) {
            reward.apply(player, level);
        }
    }

    private TreeMap<Integer, Integer> getLevelThresholds() {
        TreeMap<Integer, Integer> thresholds = new TreeMap<>();
        thresholds.put(1, 0);

        String path = "progression.levels";
        if (!plugin.getConfig().isConfigurationSection(path)) {
            return thresholds;
        }

        for (String key : plugin.getConfig().getConfigurationSection(path).getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                int required = Math.max(0, plugin.getConfig().getInt(path + "." + key + ".points", 0));
                thresholds.put(level, required);
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Ignoring invalid progression level '" + key + "'.");
            }
        }

        return thresholds;
    }

    private LevelReward parseLevelReward(int level) {
        String path = "progression.levels." + level + ".reward";
        if (!plugin.getConfig().isConfigurationSection(path)) {
            return null;
        }

        String mode = plugin.getConfig().getString(path + ".mode", "").trim().toLowerCase(Locale.ROOT);
        if (mode.isEmpty()) {
            return null;
        }

        return switch (mode) {
            case "vault", "money" -> {
                double amount = plugin.getConfig().getDouble(path + ".value", 0.0D);
                yield amount > 0.0D ? createVaultReward(amount) : null;
            }
            case "item" -> createItemReward(plugin.getConfig().getString(path + ".value", ""));
            case "command" -> createCommandReward(plugin.getConfig().getString(path + ".value", ""));
            default -> {
                plugin.getLogger().warning("Unsupported progression reward mode '" + mode + "' at " + path + ".");
                yield null;
            }
        };
    }

    private LevelReward createVaultReward(double amount) {
        if (!(plugin instanceof com.voidpulse.pulseevents.PulseEvents pulseEvents)) {
            return null;
        }

        return (player, level) -> {
            if (!pulseEvents.getEconomyManager().isAvailable()) {
                return;
            }

            if (pulseEvents.getEconomyManager().deposit(player, amount)) {
                player.sendMessage(lang.getWithPrefix(
                        "progression.reward-received",
                        "%level%",
                        String.valueOf(level),
                        "%reward%",
                        pulseEvents.getEconomyManager().format(amount)
                ));
            }
        };
    }

    private LevelReward createItemReward(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.trim().split("\\s+");
        Material material;
        try {
            material = Material.valueOf(parts[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid progression reward item material '" + parts[0] + "'.");
            return null;
        }

        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1]));
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Invalid progression reward item amount in '" + value + "'.");
            }
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        String description = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : amount + "x " + material.name();

        return (player, level) -> {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            player.sendMessage(lang.getWithPrefix(
                    "progression.reward-received",
                    "%level%",
                    String.valueOf(level),
                    "%reward%",
                    description
            ));
        };
    }

    private LevelReward createCommandReward(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }

        return (player, level) -> {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command
                            .replace("%player%", player.getName())
                            .replace("%level%", String.valueOf(level))
            );
            player.sendMessage(lang.getWithPrefix(
                    "progression.reward-received",
                    "%level%",
                    String.valueOf(level),
                    "%reward%",
                    "command"
            ));
        };
    }

    private interface LevelReward {
        void apply(Player player, int level);
    }
}
