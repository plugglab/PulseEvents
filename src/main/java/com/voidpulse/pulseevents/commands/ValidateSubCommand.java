package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ValidateSubCommand extends BaseSubCommand {

    public ValidateSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getPermission() {
        return "pulseevents.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> problems = plugin.getCustomEventManager().validateCustomEvents();

        sender.sendMessage(lang.getWithPrefix("command.validate.header"));
        if (problems.isEmpty()) {
            sender.sendMessage(lang.getWithPrefix("command.validate.clean"));
            return;
        }

        for (String problem : problems) {
            sender.sendMessage(lang.get("command.validate.line", "%message%", problem));
        }
    }
}
