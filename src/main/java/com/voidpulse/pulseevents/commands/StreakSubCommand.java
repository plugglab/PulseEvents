package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StreakSubCommand extends BaseSubCommand {

    public StreakSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "streak";
    }

    @Override
    public String getPermission() {
        return "pulseevents.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "check".equalsIgnoreCase(args[0])) {
            Player target = resolveTarget(sender, args, 1);
            if (target == null) {
                sender.sendMessage(lang.getWithPrefix("command.streak.usage"));
                return;
            }

            int streak = eventManager.getPlayerStreak(target);
            int next = eventManager.getNextStreakMilestone(streak);
            sender.sendMessage(lang.getWithPrefix(
                    "command.streak.check",
                    "%player%",
                    target.getName(),
                    "%streak%",
                    String.valueOf(streak),
                    "%next%",
                    next <= 0 ? "-" : String.valueOf(next)
            ));
            return;
        }

        if ("reset".equalsIgnoreCase(args[0])) {
            if (args.length == 1 || "all".equalsIgnoreCase(args[1])) {
                eventManager.resetAllStreaks();
                sender.sendMessage(lang.getWithPrefix("command.streak.reset-all"));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(lang.getWithPrefix("command.streak.player-not-found", "%player%", args[1]));
                return;
            }

            eventManager.resetPlayerStreak(target);
            sender.sendMessage(lang.getWithPrefix("command.streak.reset-player", "%player%", target.getName()));
            return;
        }

        sender.sendMessage(lang.getWithPrefix("command.streak.usage"));
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player target = Bukkit.getPlayerExact(args[index]);
            if (target != null) {
                return target;
            }
            sender.sendMessage(lang.getWithPrefix("command.streak.player-not-found", "%player%", args[index]));
            return null;
        }

        return sender instanceof Player player ? player : null;
    }
}
