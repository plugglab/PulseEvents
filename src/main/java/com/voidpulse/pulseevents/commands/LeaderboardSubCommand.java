package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import com.voidpulse.pulseevents.manager.ProgressionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardSubCommand extends BaseSubCommand {

    private final ProgressionManager progressionManager;

    public LeaderboardSubCommand(PulseEvents plugin) {
        super(plugin);
        this.progressionManager = plugin.getProgressionManager();
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getPermission() {
        return "pulseevents.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean pointsMode = args.length > 0 && "points".equalsIgnoreCase(args[0]);

        if (pointsMode) {
            showPointsLeaderboard(sender);
        } else {
            showStreakLeaderboard(sender);
        }
    }

    private void showStreakLeaderboard(CommandSender sender) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparingInt((Player player) -> eventManager.getPlayerStreak(player)).reversed());

        sender.sendMessage(lang.getWithPrefix("command.leaderboard.header"));
        if (players.isEmpty()) {
            sender.sendMessage(lang.getWithPrefix("command.leaderboard.empty"));
            return;
        }

        int limit = Math.min(players.size(), 10);
        boolean any = false;
        for (int i = 0; i < limit; i++) {
            Player player = players.get(i);
            int streak = eventManager.getPlayerStreak(player);
            if (streak <= 0) {
                continue;
            }

            any = true;
            sender.sendMessage(lang.get(
                    "command.leaderboard.line",
                    "%index%",
                    String.valueOf(i + 1),
                    "%player%",
                    player.getName(),
                    "%streak%",
                    String.valueOf(streak)
            ));
        }

        if (!any) {
            sender.sendMessage(lang.getWithPrefix("command.leaderboard.empty"));
        }
    }

    private void showPointsLeaderboard(CommandSender sender) {
        List<Map.Entry<UUID, Integer>> top = progressionManager.getTopPointsEntries(10);

        sender.sendMessage(lang.getWithPrefix("command.leaderboard.points-header"));
        if (top.isEmpty()) {
            sender.sendMessage(lang.getWithPrefix("command.leaderboard.empty"));
            return;
        }

        int index = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            if (entry.getValue() <= 0) {
                continue;
            }

            sender.sendMessage(lang.get(
                    "command.leaderboard.points-line",
                    "%index%",
                    String.valueOf(index),
                    "%player%",
                    progressionManager.resolveName(entry.getKey()),
                    "%points%",
                    String.valueOf(entry.getValue()),
                    "%level%",
                    String.valueOf(progressionManager.getLevel(entry.getValue()))
            ));
            index++;
        }
    }
}
