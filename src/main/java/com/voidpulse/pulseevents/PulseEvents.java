package com.voidpulse.pulseevents;

import com.voidpulse.pulseevents.commands.PECommand;
import com.voidpulse.pulseevents.events.BlackHoleEvent;
import com.voidpulse.pulseevents.events.CoinRainEvent;
import com.voidpulse.pulseevents.events.FireFeetEvent;
import com.voidpulse.pulseevents.events.FreezeEvent;
import com.voidpulse.pulseevents.events.LightningStormEvent;
import com.voidpulse.pulseevents.events.MobSwarmEvent;
import com.voidpulse.pulseevents.events.RandomEffectsEvent;
import com.voidpulse.pulseevents.events.RandomTeleportEvent;
import com.voidpulse.pulseevents.events.SpinEvent;
import com.voidpulse.pulseevents.events.TNTRainEvent;
import com.voidpulse.pulseevents.events.TargetPlayerEvent;
import com.voidpulse.pulseevents.listener.CoinRainListener;
import com.voidpulse.pulseevents.listener.EventsGuiListener;
import com.voidpulse.pulseevents.listener.JoinListener;
import com.voidpulse.pulseevents.listener.MilkBlockListener;
import com.voidpulse.pulseevents.listener.PlayerSurvivalListener;
import com.voidpulse.pulseevents.manager.EconomyManager;
import com.voidpulse.pulseevents.manager.EventManager;
import com.voidpulse.pulseevents.manager.LanguageManager;
import com.voidpulse.pulseevents.manager.LiveUIManager;
import com.voidpulse.pulseevents.manager.AnnouncementManager;
import com.voidpulse.pulseevents.manager.CooldownManager;
import com.voidpulse.pulseevents.manager.CustomEventManager;
import com.voidpulse.pulseevents.manager.ProgressionManager;
import com.voidpulse.pulseevents.manager.WorldCheck;
import com.voidpulse.pulseevents.placeholder.PulseEventsPlaceholderExpansion;
import com.voidpulse.pulseevents.update.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

@SuppressWarnings("unused")
public final class PulseEvents extends JavaPlugin {

    private EventManager eventManager;
    private LiveUIManager liveUIManager;
    private AnnouncementManager announcementManager;
    private LanguageManager lang;
    private WorldCheck worldCheck;
    private UpdateChecker updateChecker;
    private EconomyManager economyManager;
    private CooldownManager cooldownManager;
    private CustomEventManager customEventManager;
    private ProgressionManager progressionManager;
    private EventsGuiListener eventsGuiListener;
    private PulseEventsPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("events/meteor-shower.yml", false);
        saveResource("events/gravity-well.yml", false);
        saveResource("events/hot-potato.yml", false);
        getConfig().options().copyDefaults(true);
        saveConfig();

        initManagers();
        registerEvents();
        registerCommands();
        registerGameEvents();
        registerPlaceholderExpansion();
        startSystems();

        getLogger().info("PulseEvents enabled.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.stopCurrent();
        }

        if (announcementManager != null) {
            announcementManager.stop();
        }

        if (liveUIManager != null) {
            liveUIManager.stop();
        }

        if (progressionManager != null) {
            progressionManager.stopAutosave();
            progressionManager.saveAll();
        }
    }

    private void initManagers() {
        lang = new LanguageManager(this);
        liveUIManager = new LiveUIManager(this, lang);
        cooldownManager = new CooldownManager(this);
        eventManager = new EventManager(this, liveUIManager, lang);
        announcementManager = new AnnouncementManager(this, eventManager, lang);
        eventManager.setAnnouncementManager(announcementManager);
        worldCheck = new WorldCheck(this);
        updateChecker = new UpdateChecker(this, lang, "Ryvox0/PulseEvents");
        economyManager = new EconomyManager(this);
        customEventManager = new CustomEventManager(this, lang, economyManager, worldCheck);
        progressionManager = new ProgressionManager(this, lang);
        eventManager.setProgressionManager(progressionManager);
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(
                new MilkBlockListener(eventManager, worldCheck, lang),
                this
        );

        getServer().getPluginManager().registerEvents(
                new JoinListener(this, updateChecker, eventManager, liveUIManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new PlayerSurvivalListener(eventManager, progressionManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CoinRainListener(economyManager, lang),
                this
        );

        eventsGuiListener = new EventsGuiListener(this, eventManager, lang);
        getServer().getPluginManager().registerEvents(eventsGuiListener, this);
    }

    private void registerCommands() {
        PECommand peCommand = new PECommand(this);
        Objects.requireNonNull(getCommand("pe")).setExecutor(peCommand);
        Objects.requireNonNull(getCommand("pe")).setTabCompleter(peCommand);
    }

    private void registerGameEvents() {
        eventManager.clearRegisteredEvents();

        if (economyManager.isAvailable()) {
            eventManager.registerEvent(new CoinRainEvent(this));
        } else {
            getLogger().warning("Coin Rain event was not registered because Vault Economy is unavailable.");
        }

        eventManager.registerEvent(new LightningStormEvent(this));
        eventManager.registerEvent(new TNTRainEvent(this));
        eventManager.registerEvent(new MobSwarmEvent(this));
        eventManager.registerEvent(new RandomTeleportEvent(this));
        eventManager.registerEvent(new FireFeetEvent(this));
        eventManager.registerEvent(new FreezeEvent(this));
        eventManager.registerEvent(new BlackHoleEvent(this));
        eventManager.registerEvent(new RandomEffectsEvent(this));
        eventManager.registerEvent(new TargetPlayerEvent(this));
        eventManager.registerEvent(new SpinEvent(this));

        for (var customEvent : customEventManager.loadCustomEvents()) {
            eventManager.registerEvent(customEvent);
        }
    }

    private void startSystems() {
        announcementManager.start();
        progressionManager.startAutosave();

        if (getConfig().getBoolean("update-check.enabled", true)
                && getConfig().getBoolean("update-check.check-on-startup", true)) {
            updateChecker.check();
        }
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        placeholderExpansion = new PulseEventsPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("Registered PlaceholderAPI expansion.");
    }

    public void reloadPlugin() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        lang.load();
        cooldownManager.reload();
        eventManager.reloadState();
        registerGameEvents();

        if (announcementManager != null) {
            announcementManager.refreshSchedules();
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public LiveUIManager getLiveUIManager() {
        return liveUIManager;
    }

    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    public LanguageManager getLang() {
        return lang;
    }

    public WorldCheck getWorldCheck() {
        return worldCheck;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public EventsGuiListener getEventsGuiListener() {
        return eventsGuiListener;
    }

    public CustomEventManager getCustomEventManager() {
        return customEventManager;
    }

    public ProgressionManager getProgressionManager() {
        return progressionManager;
    }
}
