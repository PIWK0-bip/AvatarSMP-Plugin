package me.avatarsmp.core;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import me.avatarsmp.core.data.PlayerData;

public class CooldownManager {

    private final AvatarSMP plugin;
    private final Map<UUID, Map<Integer, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, BossBar> ultimateBars = new HashMap<>();

    public CooldownManager(AvatarSMP plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                checkReady();
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    public void setCooldown(UUID uuid, int slot, int seconds) {
        this.cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(slot, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean isOnCooldown(UUID uuid, int slot) {
        Map<Integer, Long> playerCooldowns = this.cooldowns.get(uuid);
        if (playerCooldowns == null) {
            return false;
        }
        Long expiry = playerCooldowns.get(slot);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public long getRemainingMillis(UUID uuid, int slot) {
        Map<Integer, Long> playerCooldowns = this.cooldowns.get(uuid);
        if (playerCooldowns == null) {
            return 0L;
        }
        Long expiry = playerCooldowns.get(slot);
        if (expiry == null) {
            return 0L;
        }
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    public void sendCooldownActionbar(Player player, int slot, int totalSeconds) {
        long remainingMillis = getRemainingMillis(player.getUniqueId(), slot);
        double remainingSeconds = remainingMillis / 1000.0;
        int totalBars = 20;
        double ratio = totalSeconds <= 0 ? 0 : 1.0 - (remainingMillis / (double) (totalSeconds * 1000L));
        ratio = Math.max(0, Math.min(1, ratio));
        int filled = (int) Math.round(totalBars * ratio);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                bar.append("<green>■</green>");
            } else {
                bar.append("<gray>■</gray>");
            }
        }
        Component component = AvatarSMP.MM.deserialize(
                "<yellow>Odnowienie [" + slot + "]: " + bar + " <white>" + String.format("%.1fs", remainingSeconds));
        player.sendActionBar(component);
    }

    public void checkReady() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<Integer, Long>> entry : this.cooldowns.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            Iterator<Map.Entry<Integer, Long>> it = entry.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Long> slotEntry = it.next();
                long expiry = slotEntry.getValue();
                if (expiry <= now) {
                    if (player != null && player.isOnline()) {
                        player.sendActionBar(AvatarSMP.MM.deserialize("<green><bold>⚡ Skill [" + slotEntry.getKey() + "] gotowa!"));
                        if (slotEntry.getKey() == 7) {
                            removeUltimateBossBar(player);
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                        }
                    }
                    it.remove();
                } else if (slotEntry.getKey() == 7 && player != null && player.isOnline()) {
                    double remainingSeconds = (expiry - now) / 1000.0;
                    updateUltimateBossBar(player, remainingSeconds, 60);
                }
            }
        }
    }

    private void updateUltimateBossBar(Player player, double remainingSeconds, int totalSeconds) {
        PlayerData data = this.plugin.getDataManager().getData(player.getUniqueId());
        BossBar.Color color = data == null ? BossBar.Color.WHITE : switch (data.getElement()) {
            case FIRE -> BossBar.Color.RED;
            case WATER -> BossBar.Color.BLUE;
            case EARTH -> BossBar.Color.GREEN;
            case AIR -> BossBar.Color.WHITE;
            default -> BossBar.Color.PURPLE;
        };
        float progress = (float) Math.max(0.0, Math.min(1.0, 1.0 - (remainingSeconds / totalSeconds)));
        Component title = AvatarSMP.MM.deserialize("<bold>ŁADOWANIE ULTIMATE: " + String.format("%.1fs", remainingSeconds));
        BossBar bar = this.ultimateBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
            this.ultimateBars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
            bar.progress(progress);
            bar.color(color);
        }
    }

    private void removeUltimateBossBar(Player player) {
        BossBar bar = this.ultimateBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void clearPlayer(UUID uuid) {
        this.cooldowns.remove(uuid);
        BossBar bar = this.ultimateBars.remove(uuid);
        if (bar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
    }
}