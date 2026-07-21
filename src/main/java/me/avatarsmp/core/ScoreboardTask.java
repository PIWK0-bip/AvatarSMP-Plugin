package me.avatarsmp.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardTask extends BukkitRunnable implements Listener {

    private final AvatarSMP plugin;
    private final ScoreboardManager scoreboardManager;
    private boolean started;

    public ScoreboardTask(AvatarSMP plugin, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }

    public void start() {
        if (this.started) {
            return;
        }
        this.started = true;
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        runTaskTimer(this.plugin, 20L, 20L); // Odświeżanie co 1 sekundę zamiast 10 razy na sekundę
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.scoreboardManager.update(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> this.scoreboardManager.update(event.getPlayer()));
    }
}