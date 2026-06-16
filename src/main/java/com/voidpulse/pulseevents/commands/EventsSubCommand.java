package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import org.bukkit.command.CommandSender;

import java.util.List;

public class EventsSubCommand extends BaseSubCommand {

    public EventsSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "events";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (plugin.getEventsGuiListener().openFor(sender)) {
            return;
        }

        List<String> events = eventManager.getRegisteredEventDisplayNames();
        sender.sendMessage(lang.getWithPrefix("command.events.header"));
        sender.sendMessage(lang.getWithPrefix("command.events.admin-only"));

        for (int i = 0; i < events.size(); i++) {
            sender.sendMessage(lang.get(
                    "command.events.line",
                    "%index%",
                    String.valueOf(i + 1),
                    "%event%",
                    events.get(i)
            ));
        }
    }
}
