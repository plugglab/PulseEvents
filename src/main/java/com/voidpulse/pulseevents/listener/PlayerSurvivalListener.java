package com.voidpulse.pulseevents.listener;

import com.voidpulse.pulseevents.manager.EventManager;
import com.voidpulse.pulseevents.manager.ProgressionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSurvivalListener implements Listener {

    private final EventManager eventManager;
    private final ProgressionManager progressionManager;

    public PlayerSurvivalListener(EventManager eventManager, ProgressionManager progressionManager) {
        this.eventManager = eventManager;
        this.progressionManager = progressionManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        eventManager.markPlayerFailedCurrentEvent(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        eventManager.markPlayerFailedCurrentEvent(event.getPlayer());

        if (progressionManager != null) {
            progressionManager.saveAll();
        }
    }
}
