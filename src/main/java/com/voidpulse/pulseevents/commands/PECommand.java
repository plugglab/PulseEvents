package com.voidpulse.pulseevents.commands;

import com.voidpulse.pulseevents.PulseEvents;
import com.voidpulse.pulseevents.manager.EventManager;
import com.voidpulse.pulseevents.manager.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PECommand implements CommandExecutor, TabCompleter {

    private final EventManager eventManager;
    private final LanguageManager lang;
    private final Map<String, PESubCommand> subCommands = new LinkedHashMap<>();

    public PECommand(PulseEvents plugin) {
        this.eventManager = plugin.getEventManager();
        this.lang = plugin.getLang();

        register(new HelpSubCommand(plugin));
        register(new StartSubCommand(plugin));
        register(new StopSubCommand(plugin));
        register(new StatusSubCommand(plugin));
        register(new QueueSubCommand(plugin));
        register(new VoteSubCommand(plugin));
        register(new VotesSubCommand(plugin));
        register(new StreakSubCommand(plugin));
        register(new StatsSubCommand(plugin));
        register(new LeaderboardSubCommand(plugin));
        register(new ProfileSubCommand(plugin));
        register(new ActionbarSubCommand(plugin));
        register(new ValidateSubCommand(plugin));
        register(new EventsSubCommand(plugin));
        register(new ToggleSubCommand(plugin));
        register(new ReloadSubCommand(plugin));
        register(new UpdateSubCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, new String[0]);
            return true;
        }

        PESubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sender.sendMessage(lang.getWithPrefix("command.unknown"));
            subCommands.get("help").execute(sender, new String[0]);
            return true;
        }

        String permission = subCommand.getPermission();
        if (permission != null && !sender.hasPermission(permission) && !sender.isOp()) {
            sender.sendMessage(lang.getWithPrefix("command.no-permission"));
            return true;
        }

        String[] subArgs = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        }

        subCommand.execute(sender, subArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterMatches(args[0], getAvailableSubCommands(sender));
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            if ("queue".equals(sub) && canUseQueue(sender)) {
                return filterMatches(args[1], List.of("add", "list", "remove", "clear"));
            }

            if ("toggle".equals(sub) && canUseSubCommand(sender, "toggle")) {
                return filterMatches(args[1], List.of("on", "off"));
            }

            if ("vote".equals(sub)) {
                List<String> voteTargets = new ArrayList<>();
                for (String eventName : eventManager.getRegisteredEventInputNames()) {
                    if (eventManager.isEventVotable(eventManager.findEvent(eventName))) {
                        voteTargets.add(eventName);
                    }
                }
                return filterMatches(args[1], voteTargets);
            }

            if ("votes".equals(sub)) {
                return filterMatches(args[1], List.of("list", "reset"));
            }

            if ("streak".equals(sub)) {
                return filterMatches(args[1], List.of("check", "reset"));
            }

            if ("stats".equals(sub)) {
                return Collections.emptyList();
            }

            if ("leaderboard".equals(sub)) {
                return filterMatches(args[1], List.of("streak", "points"));
            }

            if ("validate".equals(sub)) {
                return Collections.emptyList();
            }
        }

        if (args.length >= 3 && "queue".equalsIgnoreCase(args[0]) && "add".equalsIgnoreCase(args[1]) && canUseQueue(sender)) {
            String query = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)).toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();

            for (String eventName : eventManager.getRegisteredEventInputNames()) {
                if (eventName.toLowerCase(Locale.ROOT).startsWith(query)) {
                    matches.add(eventName);
                }
            }

            return matches;
        }

        if (args.length == 3 && "queue".equalsIgnoreCase(args[0]) && "remove".equalsIgnoreCase(args[1]) && canUseQueue(sender)) {
            List<String> indexes = new ArrayList<>();
            int size = eventManager.getQueuedEventDisplayNames().size();

            for (int i = 1; i <= size; i++) {
                indexes.add(String.valueOf(i));
            }

            return filterMatches(args[2], indexes);
        }

        return Collections.emptyList();
    }

    private List<String> getAvailableSubCommands(CommandSender sender) {
        List<String> result = new ArrayList<>();

        for (PESubCommand subCommand : subCommands.values()) {
            String permission = subCommand.getPermission();
            if (permission == null || sender.hasPermission(permission) || sender.isOp()) {
                result.add(subCommand.getName());
            }
        }

        return result;
    }

    private boolean canUseSubCommand(CommandSender sender, String subCommandName) {
        PESubCommand subCommand = subCommands.get(subCommandName);
        if (subCommand == null) {
            return false;
        }

        String permission = subCommand.getPermission();
        return permission == null || sender.hasPermission(permission) || sender.isOp();
    }

    private boolean canUseQueue(CommandSender sender) {
        return sender.hasPermission("pulseevents.queue.add")
                || sender.hasPermission("pulseevents.queue.list")
                || sender.hasPermission("pulseevents.queue.remove")
                || sender.hasPermission("pulseevents.queue.clear")
                || sender.isOp();
    }

    private List<String> filterMatches(String input, List<String> options) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(normalizedInput)) {
                matches.add(option);
            }
        }

        return matches;
    }

    private void register(PESubCommand subCommand) {
        subCommands.put(subCommand.getName(), subCommand);
    }
}
