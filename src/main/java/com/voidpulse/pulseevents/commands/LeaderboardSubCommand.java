package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardSubCommand extends BaseSubCommand {

    public LeaderboardSubCommand(PulseEvents plugin) {
        super(plugin);
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
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparingInt((Player player) -> eventManager.getPlayerStreak(player)).reversed());

        sender.sendMessage(lang.getWithPrefix("command.leaderboard.header"));
        if (players.isEmpty()) {
            sender.sendMessage(lang.getWithPrefix("command.leaderboard.empty"));
            return;
        }

        int limit = Math.min(players.size(), 10);
        for (int i = 0; i < limit; i++) {
            Player player = players.get(i);
            int streak = eventManager.getPlayerStreak(player);
            if (streak <= 0) {
                continue;
            }

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
    }
}
