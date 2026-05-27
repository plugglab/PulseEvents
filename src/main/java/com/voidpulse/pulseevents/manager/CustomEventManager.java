package com.voidpulse.pulseevents.manager;

import com.voidpulse.pulseevents.events.ConfiguredPulseEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomEventManager {

    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final EconomyManager economyManager;
    private final WorldCheck worldCheck;

    public CustomEventManager(JavaPlugin plugin, LanguageManager lang, EconomyManager economyManager, WorldCheck worldCheck) {
        this.plugin = plugin;
        this.lang = lang;
        this.economyManager = economyManager;
        this.worldCheck = worldCheck;
    }

    public List<ConfiguredPulseEvent> loadCustomEvents() {
        List<ConfiguredPulseEvent> events = new ArrayList<>();
        File eventsFolder = new File(plugin.getDataFolder(), "events");
        ensureExampleEvents(eventsFolder);

        File[] eventFiles = eventsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (eventFiles == null || eventFiles.length == 0) {
            return events;
        }

        for (File eventFile : eventFiles) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(eventFile);
            ConfigurationSection section = configuration.getConfigurationSection("event");
            if (section == null) {
                plugin.getLogger().warning("Custom event file '" + eventFile.getName() + "' is missing the top-level 'event' section.");
                continue;
            }

            String key = section.getString("key", stripExtension(eventFile.getName()));
            if (!section.getBoolean("enabled", true)) {
                continue;
            }

            List<String> problems = validateEventSection(key, section);
            if (!problems.isEmpty()) {
                for (String problem : problems) {
                    plugin.getLogger().warning(problem);
                }
                continue;
            }

            ConfiguredPulseEvent event = ConfiguredPulseEvent.fromConfig(
                    plugin,
                    lang,
                    economyManager,
                    worldCheck,
                    eventFile,
                    key,
                    section
            );

            if (event == null) {
                plugin.getLogger().warning("Custom event '" + key + "' was skipped because it has no valid actions.");
                continue;
            }

            events.add(event);
        }

        plugin.getLogger().info("Loaded " + events.size() + " custom PulseEvents event(s).");
        return events;
    }

    public void saveEventChance(ConfiguredPulseEvent event) {
        File sourceFile = event.getSourceFile();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(sourceFile);
        configuration.set("event.chance", event.getChance());

        try {
            configuration.save(sourceFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save chance for custom event '" + event.getKey() + "': " + exception.getMessage());
        }
    }

    private void ensureExampleEvents(File eventsFolder) {
        if (!eventsFolder.exists() && !eventsFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create custom events folder at " + eventsFolder.getAbsolutePath());
            return;
        }

        saveExampleEvent("meteor-shower.yml");
        saveExampleEvent("gravity-well.yml");
        saveExampleEvent("hot-potato.yml");
    }

    private void saveExampleEvent(String fileName) {
        File targetFile = new File(new File(plugin.getDataFolder(), "events"), fileName);
        if (!targetFile.exists()) {
            plugin.saveResource("events/" + fileName, false);
        }
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private List<String> validateEventSection(String key, ConfigurationSection section) {
        List<String> problems = new ArrayList<>();

        if (section.getInt("duration", 0) <= 0) {
            problems.add("Custom event '" + key + "' has invalid duration. Use a value greater than 0.");
        }

        String icon = section.getString("icon", "NETHER_STAR");
        try {
            Material.valueOf(icon.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            problems.add("Custom event '" + key + "' uses unknown icon material '" + icon + "'.");
        }

        ConfigurationSection actionsSection = section.getConfigurationSection("actions");
        if (actionsSection == null || actionsSection.getKeys(false).isEmpty()) {
            problems.add("Custom event '" + key + "' has no actions configured.");
            return problems;
        }

        for (String actionKey : actionsSection.getKeys(false)) {
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionKey);
            if (actionSection == null) {
                problems.add("Custom event '" + key + "' action '" + actionKey + "' is missing a section.");
                continue;
            }

            validateAction(key, actionKey, actionSection, problems);
        }

        return problems;
    }

    private void validateAction(String eventKey, String actionKey, ConfigurationSection actionSection, List<String> problems) {
        String type = actionSection.getString("type", "").toLowerCase(Locale.ROOT);
        if (type.isBlank()) {
            problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' is missing a type.");
            return;
        }

        switch (type) {
            case "message", "teleport", "strike-lightning", "velocity", "ignite" -> {
            }
            case "title" -> {
            }
            case "sound" -> {
                String sound = actionSection.getString("sound", "");
                try {
                    Sound.valueOf(sound.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' uses unknown sound '" + sound + "'.");
                }
            }
            case "potion" -> {
                String effect = actionSection.getString("effect", "");
                if (PotionEffectType.getByName(effect.toUpperCase(Locale.ROOT)) == null) {
                    problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' uses unknown potion effect '" + effect + "'.");
                }
            }
            case "spawn-mob" -> {
                String entity = actionSection.getString("entity", "ZOMBIE");
                try {
                    EntityType.valueOf(entity.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' uses unknown entity '" + entity + "'.");
                }
            }
            case "spawn-tnt" -> {
            }
            case "console-command", "player-command" -> {
                if (actionSection.getString("command", "").isBlank()) {
                    problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' requires a command.");
                }
            }
            case "economy-reward" -> {
                if (!economyManager.isAvailable()) {
                    problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' requires Vault economy, but no provider is available.");
                }
            }
            default -> problems.add("Custom event '" + eventKey + "' action '" + actionKey + "' uses unsupported type '" + type + "'.");
        }
    }
}
