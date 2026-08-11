package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import com.voidpulse.pulseevents.manager.ProgressionManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class ProfileSubCommand extends BaseSubCommand {

    private final ProgressionManager progressionManager;

    public ProfileSubCommand(PulseEvents plugin) {
        super(plugin);
        this.progressionManager = plugin.getProgressionManager();
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        OfflinePlayer target;

        if (args.length > 0) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(lang.getWithPrefix("command.profile.usage"));
            return;
        }

        if (target.getName() == null) {
            sender.sendMessage(lang.getWithPrefix("command.streak.player-not-found", "%player%", args.length > 0 ? args[0] : "?"));
            return;
        }

        int points = progressionManager.getPoints(target.getUniqueId());
        int level = progressionManager.getLevel(points);
        int nextThreshold = progressionManager.getPointsForNextLevel(level);
        int streak = target instanceof Player onlinePlayer ? eventManager.getPlayerStreak(onlinePlayer) : 0;

        sender.sendMessage(lang.getWithPrefix("command.profile.header", "%player%", target.getName()));
        sender.sendMessage(lang.get("command.profile.points", "%points%", String.valueOf(points)));
        sender.sendMessage(lang.get("command.profile.level", "%level%", String.valueOf(level)));
        sender.sendMessage(lang.get(
                "command.profile.next-level",
                "%points%",
                nextThreshold < 0 ? "-" : String.valueOf(nextThreshold)
        ));
        sender.sendMessage(lang.get("command.profile.streak", "%streak%", String.valueOf(streak)));
    }
}
