package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.command.CommandSender;

public class HelpSubCommand extends BaseSubCommand {

    public HelpSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(lang.getWithPrefix("command.help.header"));
        sender.sendMessage(lang.get("command.help.line-help"));
        sender.sendMessage(lang.get("command.help.line-start"));
        sender.sendMessage(lang.get("command.help.line-stop"));
        sender.sendMessage(lang.get("command.help.line-status"));
        sender.sendMessage(lang.get("command.help.line-events"));
        sender.sendMessage(lang.get("command.help.line-vote"));
        sender.sendMessage(lang.get("command.help.line-votes"));
        sender.sendMessage(lang.get("command.help.line-streak"));
        sender.sendMessage(lang.get("command.help.line-stats"));
        sender.sendMessage(lang.get("command.help.line-leaderboard"));
        sender.sendMessage(lang.get("command.help.line-profile"));
        sender.sendMessage(lang.get("command.help.line-actionbar"));
        sender.sendMessage(lang.get("command.help.line-validate"));
        sender.sendMessage(lang.get("command.help.line-queue"));
        sender.sendMessage(lang.get("command.help.line-toggle"));
        sender.sendMessage(lang.get("command.help.line-reload"));
        sender.sendMessage(lang.get("command.help.line-update"));
    }
}
