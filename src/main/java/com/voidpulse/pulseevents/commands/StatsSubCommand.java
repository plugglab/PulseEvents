package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.Map;

public class StatsSubCommand extends BaseSubCommand {

    public StatsSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getPermission() {
        return "pulseevents.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(lang.getWithPrefix("command.stats.header"));

        Map<String, Integer> starts = eventManager.getEventStartCountsByDisplayName();
        Map<String, Integer> votes = eventManager.getEventVoteTotalsByDisplayName();

        sender.sendMessage(lang.get("command.stats.running", "%event%", eventManager.getCurrentEventDisplayName() == null ? "none" : eventManager.getCurrentEventDisplayName()));
        sender.sendMessage(lang.get("command.stats.queue", "%amount%", String.valueOf(eventManager.getQueuedEventDisplayNames().size())));
        sender.sendMessage(lang.get("command.stats.top-streak", "%amount%", String.valueOf(eventManager.getTopServerStreak())));
        sender.sendMessage(lang.get("command.stats.top-voted", "%event%", getTopEntry(votes)));
        sender.sendMessage(lang.get("command.stats.top-started", "%event%", getTopEntry(starts)));
    }

    private String getTopEntry(Map<String, Integer> values) {
        return values.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .orElse("none");
    }
}
