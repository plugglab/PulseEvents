package com.voidpulse.pulseevents.manager;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.voidpulse.pulseevents.PulseEvents;

import java.util.Locale;

@SuppressWarnings("deprecation")
public class LiveUIManager {

    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private BossBar bossBar;
    private BukkitTask progressTask;

    public LiveUIManager(JavaPlugin plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void start(String eventName, int durationSeconds) {
        start(eventName, durationSeconds, null);
    }

    public void start(String eventName, int durationSeconds, String customTitle) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }

        stop();

        bossBar = Bukkit.createBossBar(
                resolveTitle(eventName, durationSeconds, customTitle),
                getBarColor(),
                getBarStyle()
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        if (durationSeconds <= 0) {
            return;
        }

        final int totalTicks = durationSeconds * 20;
        final int[] elapsedTicks = {0};

        progressTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (bossBar == null) {
                return;
            }

            elapsedTicks[0]++;
            double progress = Math.max(0.0, 1.0 - ((double) elapsedTicks[0] / totalTicks));
            bossBar.setProgress(progress);
            bossBar.setTitle(resolveTitle(eventName, Math.max(0, durationSeconds - (elapsedTicks[0] / 20)), customTitle));
        }, 1L, 1L);
    }

    public void addPlayer(Player player) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public void stop() {
        if (progressTask != null) {
            progressTask.cancel();
            progressTask = null;
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    private BarColor getBarColor() {
        String configured = plugin.getConfig().getString("bossbar.color", "BLUE");

        try {
            return BarColor.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BarColor.BLUE;
        }
    }

    private BarStyle getBarStyle() {
        String configured = plugin.getConfig().getString("bossbar.style", "SOLID");

        try {
            return BarStyle.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BarStyle.SOLID;
        }
    }

    private String resolveTitle(String eventName, int remainingSeconds, String customTitle) {
        String source = customTitle == null || customTitle.isBlank()
                ? plugin.getConfig().getString("bossbar.title", "&9Pulse Event: &f%event%")
                : customTitle;

        return lang.format(
                source,
                "%event%",
                eventName,
                "%time%",
                String.valueOf(Math.max(0, remainingSeconds)),
                "%leading%",
                plugin instanceof PulseEvents pulseEvents && pulseEvents.getEventManager() != null
                        ? String.valueOf(pulseEvents.getEventManager().getLeadingEventDisplayName())
                        : "none",
                "%votes%",
                plugin instanceof PulseEvents pulseEvents && pulseEvents.getEventManager() != null
                        ? String.valueOf(pulseEvents.getEventManager().getLeadingEventVoteCount())
                        : "0"
        );
    }
}
