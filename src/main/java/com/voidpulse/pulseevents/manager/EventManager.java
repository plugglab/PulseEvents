package com.voidpulse.pulseevents.manager;

import com.voidpulse.pulseevents.events.PulseEvent;
import com.voidpulse.pulseevents.events.ConfiguredPulseEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"deprecation", "unused"})
public class EventManager {

    private final JavaPlugin plugin;
    private final LiveUIManager liveUIManager;
    private final LanguageManager lang;
    private final List<PulseEvent> events = new ArrayList<>();
    private final Map<String, PulseEvent> eventsByKey = new LinkedHashMap<>();
    private final Queue<String> eventQueue = new LinkedList<>();
    private final Random random = new Random();
    private PulseEvent current;
    private AnnouncementManager announcementManager;
    private BukkitTask stopTask;
    private boolean eventsSystemEnabled;
    private final Map<UUID, Integer> survivalStreaks = new HashMap<>();
    private final Set<UUID> currentEventSurvivors = new HashSet<>();
    private final Map<UUID, String> playerVotes = new HashMap<>();
    private final Map<String, Integer> voteCounts = new HashMap<>();
    private final Map<String, Integer> eventStartCounts = new HashMap<>();
    private final Map<String, Integer> eventVoteTotals = new HashMap<>();
    private long currentEventEndTimeMillis;

    public EventManager(JavaPlugin plugin, LiveUIManager liveUIManager, LanguageManager lang) {
        this.plugin = plugin;
        this.liveUIManager = liveUIManager;
        this.lang = lang;
        this.eventsSystemEnabled = plugin.getConfig().getBoolean("events.enabled", true);
    }

    public void registerEvent(PulseEvent event) {
        events.add(event);
        eventsByKey.put(normalizeEventKey(event.getKey()), event);
    }

    public void clearRegisteredEvents() {
        events.clear();
        eventsByKey.clear();
        eventQueue.removeIf(eventKey -> !eventsByKey.containsKey(eventKey));
    }

    public void setAnnouncementManager(AnnouncementManager announcementManager) {
        this.announcementManager = announcementManager;
    }

    public boolean isEventRunning() {
        return current != null;
    }

    public boolean isEventsSystemEnabled() {
        return eventsSystemEnabled;
    }

    public void reloadState() {
        eventsSystemEnabled = plugin.getConfig().getBoolean("events.enabled", true);
    }

    public void setEventsSystemEnabled(boolean enabled) {
        eventsSystemEnabled = enabled;
        plugin.getConfig().set("events.enabled", enabled);
        plugin.saveConfig();

        if (!enabled) {
            if (current != null) {
                stopCurrent();
                return;
            }

            if (announcementManager != null) {
                announcementManager.stop();
            }
            return;
        }

        if (announcementManager != null) {
            announcementManager.refreshSchedules();
        }
    }

    public PulseEvent getCurrentEvent() {
        return current;
    }

    public boolean startRandomEvent() {
        if (!eventsSystemEnabled) {
            return false;
        }

        List<PulseEvent> availableEvents = getAvailableRandomEvents();

        if (availableEvents.isEmpty()) {
            Bukkit.broadcastMessage(lang.getWithPrefix("event.no-events"));
            return false;
        }

        if (current != null) {
            return false;
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.getLogger().fine("Skipping event start because no players are online.");
            return false;
        }

        PulseEvent selectedEvent = selectVotedEvent(availableEvents);
        if (selectedEvent == null) {
            selectedEvent = selectWeightedRandomEvent(availableEvents);
        }
        return selectedEvent != null && startEvent(selectedEvent);
    }

    public boolean isEventVotable(PulseEvent event) {
        if (event == null) {
            return false;
        }

        if (event instanceof ConfiguredPulseEvent configuredPulseEvent) {
            return configuredPulseEvent.isVoteEnabled();
        }

        return true;
    }

    public boolean enqueueEvent(String eventName) {
        if (!eventsSystemEnabled) {
            return false;
        }

        PulseEvent event = findEvent(eventName);
        if (event == null) {
            return false;
        }

        eventQueue.offer(normalizeEventKey(event.getKey()));

        if (announcementManager != null) {
            announcementManager.onQueueUpdated();
        }

        return true;
    }

    public boolean hasQueuedEvents() {
        return !eventQueue.isEmpty();
    }

    public List<String> getQueuedEventDisplayNames() {
        List<String> queueEntries = new ArrayList<>();

        for (String eventKey : eventQueue) {
            PulseEvent event = eventsByKey.get(eventKey);
            queueEntries.add(event == null ? eventKey : getDisplayName(event));
        }

        return queueEntries;
    }

    public boolean removeQueuedEvent(int index) {
        if (index < 0 || index >= eventQueue.size()) {
            return false;
        }

        List<String> snapshot = new ArrayList<>(eventQueue);
        snapshot.remove(index);
        eventQueue.clear();
        eventQueue.addAll(snapshot);

        if (announcementManager != null) {
            announcementManager.onQueueUpdated();
        }

        return true;
    }

    public void clearQueue() {
        eventQueue.clear();

        if (announcementManager != null) {
            announcementManager.refreshSchedules();
        }
    }

    public PulseEvent findEvent(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        return eventsByKey.get(normalizeEventKey(input));
    }

    public boolean tryStartNextQueuedEvent() {
        if (!eventsSystemEnabled || current != null) {
            return false;
        }

        while (!eventQueue.isEmpty()) {
            String nextKey = eventQueue.peek();
            PulseEvent nextEvent = eventsByKey.get(nextKey);

            if (nextEvent == null) {
                eventQueue.poll();
                plugin.getLogger().warning("Removed unknown event from queue: " + nextKey);
                continue;
            }

            if (Bukkit.getOnlinePlayers().isEmpty()) {
                plugin.getLogger().fine("Queue is waiting because no players are online.");
                return false;
            }

            eventQueue.poll();
            return startEvent(nextEvent);
        }

        return false;
    }

    public boolean stopCurrent() {
        return stopCurrent(false);
    }

    public boolean stopCurrentNaturally() {
        return stopCurrent(true);
    }

    public boolean stopCurrent(boolean rewardSurvivors) {
        if (current == null) {
            return false;
        }

        cancelStopTask();

        PulseEvent eventToStop = current;
        String eventName = getDisplayName(eventToStop);

        try {
            eventToStop.stop();
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to stop event " + eventToStop.getClass().getSimpleName() + ": " + exception.getMessage());
        }

        liveUIManager.stop();
        if (rewardSurvivors) {
            rewardCurrentEventSurvivors(eventToStop);
        } else {
            currentEventSurvivors.clear();
        }
        current = null;
        currentEventEndTimeMillis = 0L;

        if (eventToStop instanceof ConfiguredPulseEvent configuredPulseEvent
                && configuredPulseEvent.getEndMessage() != null
                && !configuredPulseEvent.getEndMessage().isBlank()) {
            Bukkit.broadcastMessage(lang.format(
                    configuredPulseEvent.getEndMessage(),
                    "%event%",
                    eventName
            ));
        } else {
            Bukkit.broadcastMessage(lang.getWithPrefix("event.end", "%event%", eventName));
        }

        if (announcementManager != null) {
            announcementManager.onEventStopped();
        }

        return true;
    }

    public boolean hasRegisteredEvents() {
        return !events.isEmpty();
    }

    public List<String> getRegisteredEventDisplayNames() {
        List<String> names = new ArrayList<>();

        for (PulseEvent event : events) {
            names.add(getDisplayName(event));
        }

        return names;
    }

    public List<String> getRegisteredEventInputNames() {
        List<String> names = new ArrayList<>();

        for (PulseEvent event : events) {
            names.add(event.getName());
        }

        return names;
    }

    public List<PulseEvent> getRegisteredEvents() {
        return new ArrayList<>(events);
    }

    public boolean voteForEvent(Player player, PulseEvent event) {
        return voteForEvent(player, event, false);
    }

    public boolean voteForEvent(Player player, PulseEvent event, boolean chargeCost) {
        if (player == null || event == null || !eventsSystemEnabled || current != null || !isEventVotable(event)) {
            return false;
        }

        String normalizedKey = normalizeEventKey(event.getKey());
        if (!eventsByKey.containsKey(normalizedKey)) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        String previousVote = playerVotes.get(playerId);
        if (normalizedKey.equals(previousVote)) {
            return false;
        }

        playerVotes.put(playerId, normalizedKey);
        if (previousVote != null) {
            decrementVote(previousVote);
        }

        voteCounts.merge(normalizedKey, 1, Integer::sum);
        eventVoteTotals.merge(normalizedKey, 1, Integer::sum);
        return true;
    }

    public boolean removePlayerVote(Player player) {
        if (player == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        String previousVote = playerVotes.remove(playerId);
        if (previousVote == null) {
            return false;
        }

        decrementVote(previousVote);
        return true;
    }

    public int getVoteCount(PulseEvent event) {
        return voteCounts.getOrDefault(normalizeEventKey(event.getKey()), 0);
    }

    public boolean hasActiveVote(Player player, PulseEvent event) {
        if (player == null || event == null) {
            return false;
        }

        return normalizeEventKey(event.getKey()).equals(playerVotes.get(player.getUniqueId()));
    }

    public boolean hasAnyActiveVote(Player player) {
        return player != null && playerVotes.containsKey(player.getUniqueId());
    }

    public String getConfigKey(PulseEvent event) {
        String className = event.getClass().getSimpleName();
        String baseName = className.endsWith("Event")
                ? className.substring(0, className.length() - "Event".length())
                : className;

        return event.getKey();
    }

    public int getEventChance(PulseEvent event) {
        if (event instanceof ConfiguredPulseEvent configuredPulseEvent) {
            return configuredPulseEvent.getChance();
        }

        return Math.max(0, plugin.getConfig().getInt(event.getChanceConfigPath(), 100));
    }

    public void setEventChance(PulseEvent event, int chance) {
        if (event instanceof ConfiguredPulseEvent configuredPulseEvent) {
            configuredPulseEvent.setChance(chance);
            if (plugin instanceof com.voidpulse.pulseevents.PulseEvents pulseEvents) {
                pulseEvents.getCustomEventManager().saveEventChance(configuredPulseEvent);
            }

            if (announcementManager != null) {
                announcementManager.refreshSchedules();
            }
            return;
        }

        plugin.getConfig().set(event.getChanceConfigPath(), Math.max(0, chance));
        plugin.saveConfig();

        if (announcementManager != null) {
            announcementManager.refreshSchedules();
        }
    }

    public String getCurrentEventDisplayName() {
        return current == null ? null : getDisplayName(current);
    }

    public long getCurrentEventRemainingSeconds() {
        if (current == null || currentEventEndTimeMillis <= 0L) {
            return 0L;
        }

        long remainingMillis = currentEventEndTimeMillis - System.currentTimeMillis();
        return Math.max(0L, (remainingMillis + 999L) / 1000L);
    }

    public String getLeadingEventDisplayName() {
        PulseEvent leadingEvent = getLeadingVotedEvent();
        return leadingEvent == null ? null : getDisplayName(leadingEvent);
    }

    public int getLeadingEventVoteCount() {
        PulseEvent leadingEvent = getLeadingVotedEvent();
        return leadingEvent == null ? 0 : getVoteCount(leadingEvent);
    }

    public Map<String, Integer> getVoteCountsByDisplayName() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (PulseEvent event : getRegisteredEvents()) {
            int votes = getVoteCount(event);
            if (votes > 0) {
                result.put(getDisplayName(event), votes);
            }
        }

        return result;
    }

    public Map<String, Integer> getEventStartCountsByDisplayName() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (PulseEvent event : getRegisteredEvents()) {
            int count = eventStartCounts.getOrDefault(normalizeEventKey(event.getKey()), 0);
            if (count > 0) {
                result.put(getDisplayName(event), count);
            }
        }

        return result;
    }

    public Map<String, Integer> getEventVoteTotalsByDisplayName() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (PulseEvent event : getRegisteredEvents()) {
            int count = eventVoteTotals.getOrDefault(normalizeEventKey(event.getKey()), 0);
            if (count > 0) {
                result.put(getDisplayName(event), count);
            }
        }

        return result;
    }

    public int getPlayerStreak(Player player) {
        if (player == null) {
            return 0;
        }

        return survivalStreaks.getOrDefault(player.getUniqueId(), 0);
    }

    public int getTopServerStreak() {
        int top = 0;
        for (int streak : survivalStreaks.values()) {
            top = Math.max(top, streak);
        }
        return top;
    }

    public int getNextStreakMilestone(int currentStreak) {
        List<Integer> milestones = new ArrayList<>(getSurvivalStreakMilestones().keySet());
        Collections.sort(milestones);
        for (int milestone : milestones) {
            if (milestone > currentStreak) {
                return milestone;
            }
        }
        return 0;
    }

    public void resetPlayerStreak(Player player) {
        if (player != null) {
            survivalStreaks.remove(player.getUniqueId());
        }
    }

    public void resetAllStreaks() {
        survivalStreaks.clear();
    }

    public void resetVotes() {
        clearVotes();
    }

    public String getDisplayName(PulseEvent event) {
        String translationKey = "events." + getConfigKey(event) + ".name";
        return lang.getOrDefault(translationKey, event.getName());
    }

    private boolean startEvent(PulseEvent event) {
        if (!eventsSystemEnabled || event == null || current != null) {
            return false;
        }

        if (event instanceof ConfiguredPulseEvent configuredPulseEvent
                && configuredPulseEvent.getEligiblePlayerCount() < configuredPulseEvent.getMinPlayers()) {
            plugin.getLogger().warning(
                    "Skipping custom event '" + configuredPulseEvent.getKey()
                            + "' because it requires at least "
                            + configuredPulseEvent.getMinPlayers()
                            + " eligible player(s)."
            );
            return false;
        }

        current = event;
        currentEventEndTimeMillis = System.currentTimeMillis() + (Math.max(0, event.getDuration()) * 1000L);
        eventStartCounts.merge(normalizeEventKey(event.getKey()), 1, Integer::sum);
        cancelStopTask();
        snapshotCurrentEventSurvivors(event);
        clearVotes();

        try {
            current.start();
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to start event " + event.getClass().getSimpleName() + ": " + exception.getMessage());
            currentEventSurvivors.clear();
            current = null;
            currentEventEndTimeMillis = 0L;

            if (announcementManager != null) {
                announcementManager.refreshSchedules();
            }

            return false;
        }

        String eventName = getDisplayName(event);
        if (event instanceof ConfiguredPulseEvent configuredPulseEvent) {
            if (configuredPulseEvent.getStartMessage() != null && !configuredPulseEvent.getStartMessage().isBlank()) {
                Bukkit.broadcastMessage(lang.format(
                        configuredPulseEvent.getStartMessage(),
                        "%event%",
                        eventName
                ));
            } else {
                Bukkit.broadcastMessage(lang.getWithPrefix("event.start", "%event%", eventName));
            }

            liveUIManager.start(eventName, Math.max(0, current.getDuration()), configuredPulseEvent.getBossBarTitle());
        } else {
            Bukkit.broadcastMessage(lang.getWithPrefix("event.start", "%event%", eventName));
            liveUIManager.start(eventName, Math.max(0, current.getDuration()));
        }

        if (announcementManager != null) {
            announcementManager.onEventStarted();
        }

        int duration = Math.max(0, current.getDuration());
        stopTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                this::stopCurrentNaturally,
                Math.max(1L, duration * 20L)
        );

        return true;
    }

    private PulseEvent getLeadingVotedEvent() {
        int highestVotes = 0;
        PulseEvent leading = null;

        for (PulseEvent event : events) {
            int votes = getVoteCount(event);
            if (votes > highestVotes) {
                highestVotes = votes;
                leading = event;
            }
        }

        return highestVotes > 0 ? leading : null;
    }

    private List<PulseEvent> getAvailableRandomEvents() {
        List<PulseEvent> available = new ArrayList<>();

        for (PulseEvent event : events) {
            if (getEventChance(event) > 0) {
                available.add(event);
            }
        }

        available.sort(Comparator.comparing(this::getDisplayName));
        return available;
    }

    private PulseEvent selectWeightedRandomEvent(List<PulseEvent> availableEvents) {
        int totalWeight = 0;

        for (PulseEvent event : availableEvents) {
            totalWeight += getEventChance(event);
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);

        for (PulseEvent event : availableEvents) {
            roll -= getEventChance(event);
            if (roll < 0) {
                return event;
            }
        }

        return availableEvents.get(availableEvents.size() - 1);
    }

    private PulseEvent selectVotedEvent(List<PulseEvent> availableEvents) {
        int highestVotes = 0;
        List<PulseEvent> candidates = new ArrayList<>();

        for (PulseEvent event : availableEvents) {
            int votes = getVoteCount(event);
            if (votes <= 0) {
                continue;
            }

            if (votes > highestVotes) {
                highestVotes = votes;
                candidates.clear();
                candidates.add(event);
                continue;
            }

            if (votes == highestVotes) {
                candidates.add(event);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        return selectWeightedRandomEvent(candidates);
    }

    private void cancelStopTask() {
        if (stopTask != null) {
            stopTask.cancel();
            stopTask = null;
        }
    }

    private void clearVotes() {
        playerVotes.clear();
        voteCounts.clear();
    }

    private void decrementVote(String eventKey) {
        int updated = voteCounts.getOrDefault(eventKey, 0) - 1;
        if (updated <= 0) {
            voteCounts.remove(eventKey);
        } else {
            voteCounts.put(eventKey, updated);
        }
    }

    public void markPlayerFailedCurrentEvent(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (currentEventSurvivors.remove(playerId)) {
            survivalStreaks.put(playerId, 0);
        }
    }

    private void snapshotCurrentEventSurvivors(PulseEvent event) {
        currentEventSurvivors.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEligibleForEvent(event, player)) {
                currentEventSurvivors.add(player.getUniqueId());
            }
        }
    }

    private boolean isEligibleForEvent(PulseEvent event, Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }

        List<String> globalAllowedWorlds = plugin.getConfig().getStringList("multiworld.allowed-worlds");
        if (plugin.getConfig().getBoolean("multiworld.enabled", true)
                && !globalAllowedWorlds.isEmpty()
                && !globalAllowedWorlds.contains(player.getWorld().getName())) {
            return false;
        }

        if (event instanceof ConfiguredPulseEvent configuredPulseEvent) {
            List<String> eventAllowedWorlds = configuredPulseEvent.getAllowedWorlds();
            return eventAllowedWorlds.isEmpty() || eventAllowedWorlds.contains(player.getWorld().getName());
        }

        return true;
    }

    private void rewardCurrentEventSurvivors(PulseEvent event) {
        Map<Integer, StreakReward> milestones = getSurvivalStreakMilestones();
        if (currentEventSurvivors.isEmpty()) {
            return;
        }

        for (UUID playerId : new ArrayList<>(currentEventSurvivors)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !isEligibleForEvent(event, player)) {
                survivalStreaks.put(playerId, 0);
                continue;
            }

            int streak = survivalStreaks.getOrDefault(playerId, 0) + 1;
            survivalStreaks.put(playerId, streak);

            StreakReward reward = milestones.get(streak);
            if (reward == null) {
                continue;
            }

            reward.apply(player, streak);
        }

        currentEventSurvivors.clear();
    }

    private Map<Integer, StreakReward> getSurvivalStreakMilestones() {
        Map<Integer, StreakReward> milestones = new HashMap<>();
        String path = "streak-rewards.milestones";
        if (!plugin.getConfig().isConfigurationSection(path)) {
            return milestones;
        }

        for (String key : plugin.getConfig().getConfigurationSection(path).getKeys(false)) {
            try {
                int streak = Integer.parseInt(key);
                if (streak > 0) {
                    StreakReward reward = parseStreakReward(path + "." + key);
                    if (reward != null) {
                        milestones.put(streak, reward);
                    }
                }
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Ignoring invalid streak reward milestone '" + key + "'.");
            }
        }

        return milestones;
    }

    private StreakReward parseStreakReward(String path) {
        String mode = plugin.getConfig().getString(path + ".mode", "").trim().toLowerCase(Locale.ROOT);
        if (mode.isEmpty()) {
            return null;
        }

        return switch (mode) {
            case "vault", "money" -> {
                double amount = plugin.getConfig().getDouble(path + ".value", 0.0D);
                yield amount > 0.0D ? createVaultReward(amount) : null;
            }
            case "item" -> createSimpleItemReward(plugin.getConfig().getString(path + ".value", ""));
            case "custom-item", "customitem" -> createCustomItemReward(path + ".value");
            case "command" -> createCommandReward(plugin.getConfig().getString(path + ".value", ""));
            default -> {
                plugin.getLogger().warning("Unsupported streak reward mode '" + mode + "' at " + path + ".");
                yield null;
            }
        };
    }

    private StreakReward createVaultReward(double amount) {
        if (!(plugin instanceof com.voidpulse.pulseevents.PulseEvents pulseEvents)) {
            return null;
        }

        return (player, streak) -> {
            if (!pulseEvents.getEconomyManager().isAvailable()) {
                return;
            }

            if (pulseEvents.getEconomyManager().deposit(player, amount)) {
                player.sendMessage(lang.getWithPrefix(
                        "event.streak.reward-received",
                        "%streak%",
                        String.valueOf(streak),
                        "%reward%",
                        pulseEvents.getEconomyManager().format(amount)
                ));
            }
        };
    }

    private StreakReward createSimpleItemReward(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.trim().split("\\s+");
        Material material;
        try {
            material = Material.valueOf(parts[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid streak reward item material '" + parts[0] + "'.");
            return null;
        }

        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1]));
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Invalid streak reward item amount in '" + value + "'.");
            }
        }

        ItemStack item = new ItemStack(material, amount);
        return createItemReward(item, amount + "x " + material.name());
    }

    private StreakReward createCustomItemReward(String path) {
        String materialName = plugin.getConfig().getString(path + ".material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid custom streak reward material '" + materialName + "'.");
            return null;
        }

        int amount = Math.max(1, plugin.getConfig().getInt(path + ".amount", 1));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString(path + ".name");
            if (name != null && !name.isBlank()) {
                meta.setDisplayName(lang.format(name));
            }

            List<String> lore = plugin.getConfig().getStringList(path + ".lore");
            if (!lore.isEmpty()) {
                List<String> formattedLore = new ArrayList<>();
                for (String line : lore) {
                    formattedLore.add(lang.format(line));
                }
                meta.setLore(formattedLore);
            }

            if (plugin.getConfig().contains(path + ".custom-model-data")) {
                meta.setCustomModelData(plugin.getConfig().getInt(path + ".custom-model-data"));
            }
            item.setItemMeta(meta);
        }

        String description = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : amount + "x " + material.name();
        return createItemReward(item, description);
    }

    private StreakReward createItemReward(ItemStack item, String description) {
        return (player, streak) -> {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            player.sendMessage(lang.getWithPrefix(
                    "event.streak.reward-received",
                    "%streak%",
                    String.valueOf(streak),
                    "%reward%",
                    description
            ));
        };
    }

    private StreakReward createCommandReward(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }

        return (player, streak) -> {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command
                            .replace("%player%", player.getName())
                            .replace("%streak%", String.valueOf(streak))
            );
            player.sendMessage(lang.getWithPrefix(
                    "event.streak.reward-received",
                    "%streak%",
                    String.valueOf(streak),
                    "%reward%",
                    "command"
            ));
        };
    }

    private interface StreakReward {
        void apply(Player player, int streak);
    }

    private String normalizeEventKey(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }
}
