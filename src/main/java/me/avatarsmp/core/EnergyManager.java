package me.avatarsmp.core;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnergyManager {

    private final AvatarSMP plugin;
    private final Map<UUID, Double> energy = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private double maxEnergy;
    private double regenPerSecond;
    private BukkitRunnable task;

    public EnergyManager(AvatarSMP plugin) {
        this.plugin = plugin;
        reload();
        start();
    }

    public void reload() {
        this.maxEnergy = this.plugin.getConfig().getDouble("energy.max", 100.0);
        this.regenPerSecond = this.plugin.getConfig().getDouble("energy.regen-per-second", 4.0);
    }

    private void start() {
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        this.task.runTaskTimer(this.plugin, 20L, 20L);
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = this.plugin.getDataManager().getData(player.getUniqueId());
            if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
                removeBossBar(player.getUniqueId());
                continue;
            }
            UUID uuid = player.getUniqueId();
            double current = this.energy.computeIfAbsent(uuid, k -> this.maxEnergy);
            current = Math.min(this.maxEnergy, current + this.regenPerSecond);
            this.energy.put(uuid, current);
            updateBossBar(player, current);
        }
    }

    private void updateBossBar(Player player, double current) {
        BossBar bar = this.bossBars.computeIfAbsent(player.getUniqueId(), k -> {
            BossBar newBar = BossBar.bossBar(AvatarSMP.MM.deserialize("<gradient:#00c3ff:#8e2de2>⚡ Chakra</gradient>"),
                    1.0f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
            player.showBossBar(newBar);
            return newBar;
        });
        float progress = (float) Math.max(0.0, Math.min(1.0, current / this.maxEnergy));
        bar.progress(progress);
        bar.color(progress < 0.3f ? BossBar.Color.RED : progress < 0.6f ? BossBar.Color.YELLOW : BossBar.Color.BLUE);
    }

    public boolean consume(UUID uuid, double amount) {
        double current = this.energy.getOrDefault(uuid, this.maxEnergy);
        if (current < amount) {
            return false;
        }
        this.energy.put(uuid, Math.min(this.maxEnergy, current - amount));
        return true;
    }

    public double getEnergy(UUID uuid) {
        return this.energy.getOrDefault(uuid, this.maxEnergy);
    }

    public double getMaxEnergy() {
        return this.maxEnergy;
    }

    public void removeBossBar(UUID uuid) {
        BossBar bar = this.bossBars.remove(uuid);
        if (bar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
        this.energy.remove(uuid);
    }

    public void shutdown() {
        if (this.task != null) {
            this.task.cancel();
        }
        for (Map.Entry<UUID, BossBar> entry : this.bossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        this.bossBars.clear();
        this.energy.clear();
    }
}