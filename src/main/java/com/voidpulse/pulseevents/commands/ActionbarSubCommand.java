package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ActionbarSubCommand extends BaseSubCommand {

    public ActionbarSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "actionbar";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.getWithPrefix("command.vote.players-only"));
            return;
        }

        boolean nowEnabled = plugin.getLiveUIManager().toggleActionBar(player);
        sender.sendMessage(lang.getWithPrefix(nowEnabled ? "command.actionbar.enabled" : "command.actionbar.disabled"));
    }
}
