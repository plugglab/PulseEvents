package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import com.voidpulse.pulseevents.events.PulseEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteSubCommand extends BaseSubCommand {

    public VoteSubCommand(PulseEvents plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "vote";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.getWithPrefix("command.vote.players-only"));
            return;
        }

        if (args.length == 0) {
            if (!plugin.getEventsGuiListener().openVotingFor(player)) {
                sender.sendMessage(lang.getWithPrefix("command.vote.no-voting-gui"));
            }
            return;
        }

        if (!eventManager.isEventsSystemEnabled()) {
            sender.sendMessage(lang.getWithPrefix("command.system-disabled"));
            return;
        }

        if (eventManager.isEventRunning()) {
            sender.sendMessage(lang.getWithPrefix("gui.voting.event-running"));
            return;
        }

        String eventName = String.join(" ", args);
        PulseEvent pulseEvent = eventManager.findEvent(eventName);
        if (pulseEvent == null || !eventManager.isEventVotable(pulseEvent)) {
            sender.sendMessage(lang.getWithPrefix("command.vote.invalid-event", "%event%", eventName));
            return;
        }

        if (!chargeVoteCost(player, pulseEvent)) {
            return;
        }

        if (!eventManager.voteForEvent(player, pulseEvent)) {
            sender.sendMessage(lang.getWithPrefix("gui.voting.vote-failed"));
            return;
        }

        sender.sendMessage(lang.getWithPrefix(
                "gui.voting.vote-cast",
                "%event%",
                eventManager.getDisplayName(pulseEvent),
                "%votes%",
                String.valueOf(eventManager.getVoteCount(pulseEvent))
        ));
    }

    private boolean chargeVoteCost(Player player, PulseEvent pulseEvent) {
        double cost = pulseEvent instanceof com.voidpulse.pulseevents.events.ConfiguredPulseEvent configuredPulseEvent
                && configuredPulseEvent.getVoteCostOverride() > 0.0D
                ? configuredPulseEvent.getVoteCostOverride()
                : plugin.getConfig().getDouble("voting.cost", 0.0D);

        if (cost <= 0.0D || !plugin.getEconomyManager().isAvailable()) {
            return true;
        }

        if (!plugin.getEconomyManager().has(player, cost)) {
            senderMessage(player, lang.getWithPrefix("gui.voting.not-enough-money", "%amount%", plugin.getEconomyManager().format(cost)));
            return false;
        }

        if (!plugin.getEconomyManager().withdraw(player, cost)) {
            senderMessage(player, lang.getWithPrefix("gui.voting.payment-failed"));
            return false;
        }

        return true;
    }

    private void senderMessage(Player player, String message) {
        player.sendMessage(message);
    }
}
