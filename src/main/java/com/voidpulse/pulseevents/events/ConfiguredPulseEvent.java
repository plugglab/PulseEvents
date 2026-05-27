package com.voidpulse.pulseevents.events;

import com.voidpulse.pulseevents.manager.EconomyManager;
import com.voidpulse.pulseevents.manager.LanguageManager;
import com.voidpulse.pulseevents.manager.WorldCheck;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ConfiguredPulseEvent implements PulseEvent {

    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final EconomyManager economyManager;
    private final WorldCheck worldCheck;
    private final File sourceFile;
    private final String key;
    private final String displayName;
    private int chance;
    private final int durationSeconds;
    private final Material menuMaterial;
    private final int minPlayers;
    private final List<String> allowedWorlds;
    private final String startMessage;
    private final String endMessage;
    private final String bossBarTitle;
    private final List<EventAction> actions;
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final Random random = new Random();

    public ConfiguredPulseEvent(
            JavaPlugin plugin,
            LanguageManager lang,
            EconomyManager economyManager,
            WorldCheck worldCheck,
            File sourceFile,
            String key,
            String displayName,
            int chance,
            int durationSeconds,
            Material menuMaterial,
            int minPlayers,
            List<String> allowedWorlds,
            String startMessage,
            String endMessage,
            String bossBarTitle,
            List<EventAction> actions
    ) {
        this.plugin = plugin;
        this.lang = lang;
        this.economyManager = economyManager;
        this.worldCheck = worldCheck;
        this.sourceFile = sourceFile;
        this.key = key;
        this.displayName = displayName;
        this.chance = chance;
        this.durationSeconds = durationSeconds;
        this.menuMaterial = menuMaterial;
        this.minPlayers = minPlayers;
        this.allowedWorlds = new ArrayList<>(allowedWorlds);
        this.startMessage = startMessage;
        this.endMessage = endMessage;
        this.bossBarTitle = bossBarTitle;
        this.actions = new ArrayList<>(actions);
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getChanceConfigPath() {
        return "events/" + sourceFile.getName() + "#event.chance";
    }

    @Override
    public Material getMenuMaterial() {
        return menuMaterial;
    }

    @Override
    public void start() {
        stop();

        if (getEligiblePlayers().size() < minPlayers) {
            plugin.getLogger().warning("Custom event '" + key + "' started without enough eligible players.");
            return;
        }

        for (EventAction action : actions) {
            if (action.repeatEverySeconds > 0 && action.repeatTimes > 1) {
                long delayTicks = action.delaySeconds * 20L;
                long periodTicks = action.repeatEverySeconds * 20L;
                final int[] remainingRuns = {action.repeatTimes};
                final BukkitTask[] taskRef = new BukkitTask[1];
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (remainingRuns[0] <= 0) {
                        if (taskRef[0] != null) {
                            taskRef[0].cancel();
                        }
                        return;
                    }

                    executeAction(action);
                    remainingRuns[0]--;
                    if (remainingRuns[0] <= 0 && taskRef[0] != null) {
                        taskRef[0].cancel();
                    }
                }, delayTicks, Math.max(1L, periodTicks));
                taskRef[0] = task;
                activeTasks.add(task);
                continue;
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> executeAction(action), action.delaySeconds * 20L);
            activeTasks.add(task);
        }
    }

    @Override
    public void stop() {
        for (BukkitTask activeTask : activeTasks) {
            activeTask.cancel();
        }
        activeTasks.clear();
    }

    @Override
    public int getDuration() {
        return durationSeconds;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = Math.max(0, chance);
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public List<String> getAllowedWorlds() {
        return Collections.unmodifiableList(allowedWorlds);
    }

    public int getEligiblePlayerCount() {
        return getEligiblePlayers().size();
    }

    public String getStartMessage() {
        return startMessage;
    }

    public String getEndMessage() {
        return endMessage;
    }

    public String getBossBarTitle() {
        return bossBarTitle;
    }

    private void executeAction(EventAction action) {
        List<Player> targets = resolveTargets(action);
        if (targets.isEmpty()) {
            return;
        }

        if (action.chance < 100 && random.nextInt(100) >= action.chance) {
            return;
        }

        switch (action.type) {
            case "message" -> {
                for (Player target : targets) {
                    target.sendMessage(formatText(action.message));
                }
            }
            case "sound" -> {
                for (Player target : targets) {
                    target.playSound(target.getLocation(), action.sound, action.volume, action.pitch);
                }
            }
            case "potion" -> {
                for (Player target : targets) {
                    target.addPotionEffect(new PotionEffect(action.potionEffectType, action.potionDurationSeconds * 20, action.potionAmplifier));
                }
            }
            case "teleport" -> {
                for (Player target : targets) {
                    Location base = target.getLocation();
                    Location destination = resolveTeleport(base, action);
                    target.teleport(destination);
                }
            }
            case "spawn-mob" -> {
                for (Player target : targets) {
                    Location location = offset(target.getLocation(), action.offsetX, action.offsetY, action.offsetZ);
                    for (int i = 0; i < action.amount; i++) {
                        location.getWorld().spawnEntity(location, action.entityType);
                    }
                }
            }
            case "strike-lightning" -> {
                for (Player target : targets) {
                    Location location = offset(target.getLocation(), action.offsetX, action.offsetY, action.offsetZ);
                    if (action.effectOnly) {
                        location.getWorld().strikeLightningEffect(location);
                    } else {
                        location.getWorld().strikeLightning(location);
                    }
                }
            }
            case "spawn-tnt" -> {
                for (Player target : targets) {
                    Location location = offset(target.getLocation(), action.offsetX, action.offsetY, action.offsetZ);
                    for (int i = 0; i < action.amount; i++) {
                        var tnt = location.getWorld().spawn(location, org.bukkit.entity.TNTPrimed.class);
                        tnt.setFuseTicks(action.fuseTicks);
                        tnt.setYield((float) action.explosionPower);
                    }
                }
            }
            case "velocity" -> {
                for (Player target : targets) {
                    target.setVelocity(new Vector(action.velocityX, action.velocityY, action.velocityZ));
                }
            }
            case "ignite" -> {
                for (Player target : targets) {
                    target.setFireTicks(action.fireTicks);
                }
            }
            case "economy-reward" -> {
                if (!economyManager.isAvailable()) {
                    return;
                }

                for (Player target : targets) {
                    double reward = action.moneyMin >= action.moneyMax
                            ? action.moneyMin
                            : action.moneyMin + (random.nextDouble() * (action.moneyMax - action.moneyMin));
                    if (economyManager.deposit(target, reward)) {
                        target.sendMessage(lang.getWithPrefix("event.coin-rain.reward-received", "%amount%", economyManager.format(reward)));
                    } else {
                        target.sendMessage(lang.getWithPrefix("event.coin-rain.deposit-failed"));
                    }
                }
            }
            case "title" -> {
                for (Player target : targets) {
                    target.sendTitle(
                            formatText(action.title),
                            formatText(action.subtitle),
                            action.fadeInTicks,
                            action.stayTicks,
                            action.fadeOutTicks
                    );
                }
            }
            case "console-command" -> {
                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                for (Player target : targets) {
                    Bukkit.dispatchCommand(console, formatCommand(action.command, target));
                }
            }
            case "player-command" -> {
                for (Player target : targets) {
                    Bukkit.dispatchCommand(target, formatCommand(action.command, target));
                }
            }
            default -> plugin.getLogger().warning("Unknown custom event action type '" + action.type + "' in event '" + key + "'.");
        }
    }

    private List<Player> resolveTargets(EventAction action) {
        String targetMode = action.target;
        List<Player> eligiblePlayers = getEligiblePlayers();
        if (eligiblePlayers.isEmpty()) {
            return Collections.emptyList();
        }

        return switch (targetMode) {
            case "random-player" -> List.of(eligiblePlayers.get(random.nextInt(eligiblePlayers.size())));
            case "random-players" -> selectRandomPlayers(eligiblePlayers, action.targetCount);
            case "all-players", "all" -> eligiblePlayers;
            default -> eligiblePlayers;
        };
    }

    private List<Player> selectRandomPlayers(List<Player> eligiblePlayers, int targetCount) {
        List<Player> pool = new ArrayList<>(eligiblePlayers);
        Collections.shuffle(pool, random);
        int count = Math.max(1, Math.min(pool.size(), targetCount));
        return new ArrayList<>(pool.subList(0, count));
    }

    private List<Player> getEligiblePlayers() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        List<Player> eligiblePlayers = new ArrayList<>();

        for (Player player : onlinePlayers) {
            if (!worldCheck.isAllowed(player.getWorld())) {
                continue;
            }

            if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                continue;
            }

            eligiblePlayers.add(player);
        }

        return eligiblePlayers;
    }

    private Location resolveTeleport(Location base, EventAction action) {
        World world = base.getWorld();
        double x = base.getX() + randomBetween(action.randomXMin, action.randomXMax);
        double y = Math.max(world.getMinHeight() + 1, base.getY() + randomBetween(action.randomYMin, action.randomYMax));
        double z = base.getZ() + randomBetween(action.randomZMin, action.randomZMax);
        return new Location(world, x, y, z, base.getYaw(), base.getPitch());
    }

    private double randomBetween(double min, double max) {
        if (max <= min) {
            return min;
        }

        return min + (random.nextDouble() * (max - min));
    }

    private Location offset(Location location, double x, double y, double z) {
        return location.clone().add(x, y, z);
    }

    public static ConfiguredPulseEvent fromConfig(
            JavaPlugin plugin,
            LanguageManager lang,
            EconomyManager economyManager,
            WorldCheck worldCheck,
            File sourceFile,
            String key,
            ConfigurationSection section
    ) {
        String displayName = section.getString("name", toDisplayName(key));
        int chance = Math.max(0, section.getInt("chance", 100));
        int durationSeconds = Math.max(5, section.getInt("duration", 30));
        Material menuMaterial = parseMaterial(section.getString("icon", "NETHER_STAR"));
        int minPlayers = Math.max(1, section.getInt("min-players", 1));
        List<String> allowedWorlds = section.getStringList("allowed-worlds");
        String startMessage = section.getString("start-message");
        String endMessage = section.getString("end-message");
        String bossBarTitle = section.getString("bossbar-title");
        List<EventAction> actions = new ArrayList<>();

        for (MaplessSection actionSection : getActionSections(section.getConfigurationSection("actions"))) {
            EventAction action = EventAction.fromConfig(actionSection.key, actionSection.section);
            if (action != null) {
                actions.add(action);
            }
        }

        if (actions.isEmpty()) {
            return null;
        }

        return new ConfiguredPulseEvent(
                plugin,
                lang,
                economyManager,
                worldCheck,
                sourceFile,
                key,
                displayName,
                chance,
                durationSeconds,
                menuMaterial,
                minPlayers,
                allowedWorlds,
                startMessage,
                endMessage,
                bossBarTitle,
                actions
        );
    }

    private String formatText(String input) {
        return lang.format(
                input == null ? "" : input,
                "%event%",
                displayName
        );
    }

    private String formatCommand(String command, Player target) {
        return formatText(command)
                .replace("%player%", target.getName())
                .replace("%world%", target.getWorld().getName());
    }

    private static List<MaplessSection> getActionSections(ConfigurationSection actionsSection) {
        if (actionsSection == null) {
            return Collections.emptyList();
        }

        List<MaplessSection> sections = new ArrayList<>();
        for (String childKey : actionsSection.getKeys(false)) {
            ConfigurationSection childSection = actionsSection.getConfigurationSection(childKey);
            if (childSection != null) {
                sections.add(new MaplessSection(childKey, childSection));
            }
        }
        return sections;
    }

    private static Material parseMaterial(String value) {
        try {
            return Material.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Material.NETHER_STAR;
        }
    }

    private static String toDisplayName(String key) {
        String[] parts = key.split("-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private record MaplessSection(String key, ConfigurationSection section) {
    }

    private static final class EventAction {
        private final String type;
        private final String target;
        private final int targetCount;
        private final int chance;
        private final int delaySeconds;
        private final int repeatEverySeconds;
        private final int repeatTimes;
        private final String message;
        private final String title;
        private final String subtitle;
        private final String command;
        private final Sound sound;
        private final float volume;
        private final float pitch;
        private final PotionEffectType potionEffectType;
        private final int potionDurationSeconds;
        private final int potionAmplifier;
        private final EntityType entityType;
        private final int amount;
        private final double offsetX;
        private final double offsetY;
        private final double offsetZ;
        private final boolean effectOnly;
        private final int fuseTicks;
        private final double explosionPower;
        private final double velocityX;
        private final double velocityY;
        private final double velocityZ;
        private final int fireTicks;
        private final double moneyMin;
        private final double moneyMax;
        private final double randomXMin;
        private final double randomXMax;
        private final double randomYMin;
        private final double randomYMax;
        private final double randomZMin;
        private final double randomZMax;
        private final int fadeInTicks;
        private final int stayTicks;
        private final int fadeOutTicks;

        private EventAction(
                String type,
                String target,
                int targetCount,
                int chance,
                int delaySeconds,
                int repeatEverySeconds,
                int repeatTimes,
                String message,
                String title,
                String subtitle,
                String command,
                Sound sound,
                float volume,
                float pitch,
                PotionEffectType potionEffectType,
                int potionDurationSeconds,
                int potionAmplifier,
                EntityType entityType,
                int amount,
                double offsetX,
                double offsetY,
                double offsetZ,
                boolean effectOnly,
                int fuseTicks,
                double explosionPower,
                double velocityX,
                double velocityY,
                double velocityZ,
                int fireTicks,
                double moneyMin,
                double moneyMax,
                double randomXMin,
                double randomXMax,
                double randomYMin,
                double randomYMax,
                double randomZMin,
                double randomZMax,
                int fadeInTicks,
                int stayTicks,
                int fadeOutTicks
        ) {
            this.type = type;
            this.target = target;
            this.targetCount = targetCount;
            this.chance = chance;
            this.delaySeconds = delaySeconds;
            this.repeatEverySeconds = repeatEverySeconds;
            this.repeatTimes = repeatTimes;
            this.message = message;
            this.title = title;
            this.subtitle = subtitle;
            this.command = command;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.potionEffectType = potionEffectType;
            this.potionDurationSeconds = potionDurationSeconds;
            this.potionAmplifier = potionAmplifier;
            this.entityType = entityType;
            this.amount = amount;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.effectOnly = effectOnly;
            this.fuseTicks = fuseTicks;
            this.explosionPower = explosionPower;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.fireTicks = fireTicks;
            this.moneyMin = moneyMin;
            this.moneyMax = moneyMax;
            this.randomXMin = randomXMin;
            this.randomXMax = randomXMax;
            this.randomYMin = randomYMin;
            this.randomYMax = randomYMax;
            this.randomZMin = randomZMin;
            this.randomZMax = randomZMax;
            this.fadeInTicks = fadeInTicks;
            this.stayTicks = stayTicks;
            this.fadeOutTicks = fadeOutTicks;
        }

        private static EventAction fromConfig(String key, ConfigurationSection section) {
            String type = section.getString("type", "").toLowerCase(Locale.ROOT);
            if (type.isEmpty()) {
                return null;
            }

            Sound sound = null;
            String soundName = section.getString("sound");
            if (soundName != null && !soundName.isBlank()) {
                try {
                    sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    sound = null;
                }
            }

            PotionEffectType effectType = null;
            String effectName = section.getString("effect");
            if (effectName != null && !effectName.isBlank()) {
                effectType = PotionEffectType.getByName(effectName.toUpperCase(Locale.ROOT));
            }

            EntityType entityType = EntityType.ZOMBIE;
            String entityName = section.getString("entity", "ZOMBIE");
            try {
                entityType = EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                entityType = EntityType.ZOMBIE;
            }

            return new EventAction(
                    type,
                    section.getString("target", "all-players").toLowerCase(Locale.ROOT),
                    Math.max(1, section.getInt("target-count", 1)),
                    Math.max(0, Math.min(100, section.getInt("chance", 100))),
                    Math.max(0, section.getInt("delay-seconds", 0)),
                    Math.max(0, section.getInt("repeat-every-seconds", 0)),
                    Math.max(1, section.getInt("repeat-times", 1)),
                    section.getString("message", "&e%event% is active."),
                    section.getString("title", ""),
                    section.getString("subtitle", ""),
                    section.getString("command", ""),
                    sound,
                    (float) section.getDouble("volume", 1.0),
                    (float) section.getDouble("pitch", 1.0),
                    effectType == null ? PotionEffectType.SPEED : effectType,
                    Math.max(1, section.getInt("effect-duration-seconds", 5)),
                    Math.max(0, section.getInt("amplifier", 0)),
                    entityType,
                    Math.max(1, section.getInt("amount", 1)),
                    section.getDouble("offset.x", 0.0),
                    section.getDouble("offset.y", 0.0),
                    section.getDouble("offset.z", 0.0),
                    section.getBoolean("effect-only", false),
                    Math.max(0, section.getInt("fuse-ticks", 40)),
                    Math.max(0.0, section.getDouble("explosion-power", 4.0)),
                    section.getDouble("velocity.x", 0.0),
                    section.getDouble("velocity.y", 0.0),
                    section.getDouble("velocity.z", 0.0),
                    Math.max(0, section.getInt("fire-ticks", 60)),
                    section.getDouble("money.min", 0.0),
                    section.getDouble("money.max", 0.0),
                    section.getDouble("random-offset.x.min", 0.0),
                    section.getDouble("random-offset.x.max", 0.0),
                    section.getDouble("random-offset.y.min", 0.0),
                    section.getDouble("random-offset.y.max", 0.0),
                    section.getDouble("random-offset.z.min", 0.0),
                    section.getDouble("random-offset.z.max", 0.0),
                    Math.max(0, section.getInt("fade-in-ticks", 10)),
                    Math.max(0, section.getInt("stay-ticks", 40)),
                    Math.max(0, section.getInt("fade-out-ticks", 10))
            );
        }
    }
}
