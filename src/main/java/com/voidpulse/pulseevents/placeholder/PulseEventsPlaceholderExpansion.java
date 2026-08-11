package com.voidpulse.pulseevents.placeholder;

import com.voidpulse.pulseevents.PulseEvents;
import com.voidpulse.pulseevents.manager.EventManager;
import com.voidpulse.pulseevents.manager.ProgressionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PulseEventsPlaceholderExpansion extends PlaceholderExpansion {

    private final PulseEvents plugin;

    public PulseEventsPlaceholderExpansion(PulseEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pulseevents";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        EventManager eventManager = plugin.getEventManager();
        ProgressionManager progressionManager = plugin.getProgressionManager();

        return switch (params.toLowerCase()) {
            case "current_event" -> {
                String currentEvent = eventManager.getCurrentEventDisplayName();
                yield currentEvent == null ? "none" : currentEvent;
            }
            case "current_event_time_remaining" -> String.valueOf(eventManager.getCurrentEventRemainingSeconds());
            case "leading_event" -> {
                String leading = eventManager.getLeadingEventDisplayName();
                yield leading == null ? "none" : leading;
            }
            case "leading_event_votes" -> String.valueOf(eventManager.getLeadingEventVoteCount());
            case "event_active" -> String.valueOf(eventManager.isEventRunning());
            case "events_enabled" -> String.valueOf(eventManager.isEventsSystemEnabled());
            case "queue_size" -> String.valueOf(eventManager.getQueuedEventDisplayNames().size());
            case "registered_events" -> String.valueOf(eventManager.getRegisteredEvents().size());
            case "player_streak" -> String.valueOf(eventManager.getPlayerStreak(player == null ? null : player.getPlayer()));
            case "top_streak" -> String.valueOf(eventManager.getTopServerStreak());
            case "next_streak_milestone" -> String.valueOf(eventManager.getNextStreakMilestone(eventManager.getPlayerStreak(player == null ? null : player.getPlayer())));
            case "points" -> String.valueOf(player == null ? 0 : progressionManager.getPoints(player.getUniqueId()));
            case "level" -> {
                int points = player == null ? 0 : progressionManager.getPoints(player.getUniqueId());
                yield String.valueOf(progressionManager.getLevel(points));
            }
            case "points_next_level" -> {
                int points = player == null ? 0 : progressionManager.getPoints(player.getUniqueId());
                int nextThreshold = progressionManager.getPointsForNextLevel(progressionManager.getLevel(points));
                yield nextThreshold < 0 ? "-" : String.valueOf(nextThreshold);
            }
            case "points_to_next_level" -> {
                int points = player == null ? 0 : progressionManager.getPoints(player.getUniqueId());
                int nextThreshold = progressionManager.getPointsForNextLevel(progressionManager.getLevel(points));
                yield nextThreshold < 0 ? "-" : String.valueOf(Math.max(0, nextThreshold - points));
            }
            default -> null;
        };
    }
}
