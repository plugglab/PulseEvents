package com.voidpulse.pulseevents.listener;

import com.voidpulse.pulseevents.manager.EventManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSurvivalListener implements Listener {

    private final EventManager eventManager;

    public PlayerSurvivalListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        eventManager.markPlayerFailedCurrentEvent(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        eventManager.markPlayerFailedCurrentEvent(event.getPlayer());
    }
}
