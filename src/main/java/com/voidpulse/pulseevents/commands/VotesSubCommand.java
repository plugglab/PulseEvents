package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class VotesSubCommand extends BaseSubCommand {

    public VotesSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "votes";
    }

    @Override
    public String getPermission() {
        return "pulseevents.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            sender.sendMessage(lang.getWithPrefix("command.votes.header"));
            Map<String, Integer> votes = eventManager.getVoteCountsByDisplayName();
            if (votes.isEmpty()) {
                sender.sendMessage(lang.getWithPrefix("command.votes.empty"));
                return;
            }

            for (var entry : votes.entrySet()) {
                sender.sendMessage(lang.get(
                        "command.votes.line",
                        "%event%",
                        entry.getKey(),
                        "%votes%",
                        String.valueOf(entry.getValue())
                ));
            }
            return;
        }

        if ("reset".equalsIgnoreCase(args[0])) {
            eventManager.resetVotes();
            sender.sendMessage(lang.getWithPrefix("command.votes.reset"));
            return;
        }

        sender.sendMessage(lang.getWithPrefix("command.votes.usage"));
    }
}
