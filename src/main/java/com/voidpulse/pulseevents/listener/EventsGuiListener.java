package com.voidpulse.pulseevents.listener;

import com.voidpulse.pulseevents.PulseEvents;
import com.voidpulse.pulseevents.events.ConfiguredPulseEvent;
import com.voidpulse.pulseevents.events.PulseEvent;
import com.voidpulse.pulseevents.manager.EventManager;
import com.voidpulse.pulseevents.manager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventsGuiListener implements Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int SUMMARY_SLOT = 26;

    private final PulseEvents plugin;
    private final EventManager eventManager;
    private final LanguageManager lang;

    public EventsGuiListener(PulseEvents plugin, EventManager eventManager, LanguageManager lang) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.lang = lang;
    }

    public boolean openFor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (!canManageEvents(player)) {
            return false;
        }

        player.openInventory(createAdminInventory(player));
        return true;
    }

    public boolean openVotingFor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        player.openInventory(createVotingInventory(player));
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof EventsMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();
        List<PulseEvent> events = getSortedEvents();
        if (slot < 0 || slot >= events.size()) {
            return;
        }

        PulseEvent pulseEvent = events.get(slot);
        if (holder.mode == MenuMode.VOTING) {
            handleVote(player, pulseEvent);
            player.openInventory(createVotingInventory(player));
            return;
        }

        if (isQueueClick(event.getClick())) {
            handleQueueAdd(player, pulseEvent);
            player.openInventory(createAdminInventory(player));
            return;
        }

        int change = resolveChange(event.getClick());
        if (change == 0) {
            return;
        }

        int updatedChance = Math.max(0, eventManager.getEventChance(pulseEvent) + change);
        eventManager.setEventChance(pulseEvent, updatedChance);

        player.sendMessage(lang.getWithPrefix(
                "gui.events.updated",
                "%event%",
                eventManager.getDisplayName(pulseEvent),
                "%chance%",
                String.valueOf(updatedChance)
        ));
        player.openInventory(createAdminInventory(player));
    }

    private Inventory createAdminInventory(Player viewer) {
        MenuMode mode = canManageEvents(viewer) ? MenuMode.ADMIN : MenuMode.VOTING;
        Inventory inventory = Bukkit.createInventory(new EventsMenuHolder(mode), INVENTORY_SIZE, lang.get(mode.titleKey));
        List<PulseEvent> events = getSortedEvents();

        for (int i = 0; i < Math.min(events.size(), INVENTORY_SIZE); i++) {
            if (i == SUMMARY_SLOT) {
                break;
            }
            inventory.setItem(i, createEventItem(viewer, events.get(i), mode));
        }

        inventory.setItem(SUMMARY_SLOT, mode == MenuMode.ADMIN ? createQueueSummaryItem() : createVotingSummaryItem());
        return inventory;
    }

    private Inventory createVotingInventory(Player viewer) {
        Inventory inventory = Bukkit.createInventory(new EventsMenuHolder(MenuMode.VOTING), INVENTORY_SIZE, lang.get(MenuMode.VOTING.titleKey));
        List<PulseEvent> events = getVotableEvents();

        for (int i = 0; i < Math.min(events.size(), INVENTORY_SIZE); i++) {
            if (i == SUMMARY_SLOT) {
                break;
            }
            inventory.setItem(i, createEventItem(viewer, events.get(i), MenuMode.VOTING));
        }

        inventory.setItem(SUMMARY_SLOT, createVotingSummaryItem());
        return inventory;
    }

    private ItemStack createEventItem(Player viewer, PulseEvent pulseEvent, MenuMode mode) {
        ItemStack item = new ItemStack(pulseEvent.getMenuMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(lang.get("gui.events.item-name", "%event%", eventManager.getDisplayName(pulseEvent)));

        List<String> lore = new ArrayList<>();
        lore.add(lang.get("gui.events.item-chance", "%chance%", String.valueOf(eventManager.getEventChance(pulseEvent))));
        if (mode == MenuMode.ADMIN) {
            lore.add(lang.get("gui.events.item-left"));
            lore.add(lang.get("gui.events.item-right"));
            lore.add(lang.get("gui.events.item-shift-left"));
            lore.add(lang.get("gui.events.item-shift-right"));
            lore.add(lang.get("gui.events.item-middle"));
            lore.add(lang.get("gui.events.item-queued", "%amount%", String.valueOf(countQueuedCopies(pulseEvent))));
        } else {
            lore.add(lang.get("gui.voting.item-votes", "%amount%", String.valueOf(eventManager.getVoteCount(pulseEvent))));
            lore.add(lang.get("gui.voting.item-cost", "%amount%", getVoteCostDisplay(pulseEvent)));
            if (pulseEvent instanceof ConfiguredPulseEvent configuredPulseEvent) {
                if (!configuredPulseEvent.getCategory().isBlank()) {
                    lore.add(lang.get("gui.events.item-category", "%category%", configuredPulseEvent.getCategory()));
                }
                if (configuredPulseEvent.getVoteCostOverride() > 0.0D) {
                    lore.add(lang.get("gui.voting.item-cost-override", "%amount%", plugin.getEconomyManager().format(configuredPulseEvent.getVoteCostOverride())));
                }
            }
            lore.add(lang.get("gui.voting.item-click"));
            if (eventManager.hasActiveVote(viewer, pulseEvent)) {
                lore.add(lang.get("gui.voting.item-selected"));
            }
        }

        if (pulseEvent instanceof ConfiguredPulseEvent configuredPulseEvent) {
            lore.add(lang.get("gui.events.item-duration", "%seconds%", String.valueOf(configuredPulseEvent.getDuration())));
            lore.add(lang.get("gui.events.item-min-players", "%amount%", String.valueOf(configuredPulseEvent.getMinPlayers())));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createQueueSummaryItem() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(lang.get("gui.events.queue-name"));
        List<String> lore = new ArrayList<>();
        lore.add(lang.get("gui.events.queue-size", "%amount%", String.valueOf(eventManager.getQueuedEventDisplayNames().size())));
        lore.add(lang.get("gui.events.queue-hint"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createVotingSummaryItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(lang.get("gui.voting.summary-name"));
        List<String> lore = new ArrayList<>();
        lore.add(lang.get("gui.voting.summary-cost", "%amount%", getVoteCostDisplay(null)));
        lore.add(lang.get("gui.voting.summary-note"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<PulseEvent> getSortedEvents() {
        List<PulseEvent> events = eventManager.getRegisteredEvents();
        events.sort(Comparator.comparing(eventManager::getDisplayName));
        return events;
    }

    private List<PulseEvent> getVotableEvents() {
        List<PulseEvent> events = new ArrayList<>();
        for (PulseEvent event : eventManager.getRegisteredEvents()) {
            if (eventManager.isEventVotable(event)) {
                events.add(event);
            }
        }
        events.sort(Comparator.comparing(eventManager::getDisplayName));
        return events;
    }

    private int resolveChange(ClickType clickType) {
        return switch (clickType) {
            case LEFT -> 5;
            case SHIFT_LEFT -> 20;
            case RIGHT -> -5;
            case SHIFT_RIGHT -> -20;
            default -> 0;
        };
    }

    private boolean isQueueClick(ClickType clickType) {
        return clickType == ClickType.MIDDLE
                || clickType == ClickType.DROP
                || clickType == ClickType.CONTROL_DROP;
    }

    private void handleQueueAdd(Player player, PulseEvent pulseEvent) {
        if (!player.hasPermission("pulseevents.queue.add") && !player.isOp()) {
            player.sendMessage(lang.getWithPrefix("command.no-permission"));
            return;
        }

        if (!eventManager.isEventsSystemEnabled()) {
            player.sendMessage(lang.getWithPrefix("command.system-disabled"));
            return;
        }

        if (!eventManager.enqueueEvent(pulseEvent.getName())) {
            player.sendMessage(lang.getWithPrefix("command.queue.invalid-event", "%event%", eventManager.getDisplayName(pulseEvent)));
            return;
        }

        player.sendMessage(lang.getWithPrefix(
                "gui.events.queued",
                "%event%",
                eventManager.getDisplayName(pulseEvent),
                "%amount%",
                String.valueOf(countQueuedCopies(pulseEvent))
        ));
    }

    private void handleVote(Player player, PulseEvent pulseEvent) {
        if (!eventManager.isEventsSystemEnabled()) {
            player.sendMessage(lang.getWithPrefix("command.system-disabled"));
            return;
        }

        if (eventManager.isEventRunning()) {
            player.sendMessage(lang.getWithPrefix("gui.voting.event-running"));
            return;
        }

        double cost = resolveVoteCost(pulseEvent);
        if (cost > 0.0D && plugin.getEconomyManager().isAvailable()) {
            if (!plugin.getEconomyManager().has(player, cost)) {
                player.sendMessage(lang.getWithPrefix(
                        "gui.voting.not-enough-money",
                        "%amount%",
                        plugin.getEconomyManager().format(cost)
                ));
                return;
            }

            if (!plugin.getEconomyManager().withdraw(player, cost)) {
                player.sendMessage(lang.getWithPrefix("gui.voting.payment-failed"));
                return;
            }
        }

        if (!eventManager.voteForEvent(player, pulseEvent)) {
            player.sendMessage(lang.getWithPrefix("gui.voting.vote-failed"));
            return;
        }

        player.sendMessage(lang.getWithPrefix(
                "gui.voting.vote-cast",
                "%event%",
                eventManager.getDisplayName(pulseEvent),
                "%votes%",
                String.valueOf(eventManager.getVoteCount(pulseEvent))
        ));
    }

    private boolean canManageEvents(Player player) {
        return player.isOp() || player.hasPermission("pulseevents.admin");
    }

    private String getVoteCostDisplay(PulseEvent pulseEvent) {
        double cost = resolveVoteCost(pulseEvent);
        if (!plugin.getEconomyManager().isAvailable() || cost <= 0.0D) {
            return lang.get("gui.voting.free");
        }

        return plugin.getEconomyManager().format(cost);
    }

    private double resolveVoteCost(PulseEvent pulseEvent) {
        double baseCost = plugin.getConfig().getDouble("voting.cost", 0.0D);
        if (pulseEvent instanceof ConfiguredPulseEvent configuredPulseEvent && configuredPulseEvent.getVoteCostOverride() > 0.0D) {
            return configuredPulseEvent.getVoteCostOverride();
        }
        return baseCost;
    }

    private int countQueuedCopies(PulseEvent pulseEvent) {
        String displayName = eventManager.getDisplayName(pulseEvent);
        int count = 0;

        for (String queuedEvent : eventManager.getQueuedEventDisplayNames()) {
            if (queuedEvent.equals(displayName)) {
                count++;
            }
        }

        return count;
    }

    private enum MenuMode {
        ADMIN("gui.events.title"),
        VOTING("gui.voting.title");

        private final String titleKey;

        MenuMode(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    private static final class EventsMenuHolder implements org.bukkit.inventory.InventoryHolder {
        private final MenuMode mode;

        private EventsMenuHolder(MenuMode mode) {
            this.mode = mode;
        }

        @Override
        public Inventory getInventory() {
            return Bukkit.createInventory(this, INVENTORY_SIZE);
        }
    }
}
