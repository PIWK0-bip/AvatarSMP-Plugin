package me.avatarsmp.core;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExpBarManager {

    private final AvatarSMP plugin;
    private final Map<UUID, Integer> savedLevel = new ConcurrentHashMap<>();
    private final Map<UUID, Float> savedExp = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasReady = new ConcurrentHashMap<>();
    private BukkitRunnable task;

    public ExpBarManager(AvatarSMP plugin) {
        this.plugin = plugin;
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        this.task.runTaskTimer(this.plugin, 5L, 2L);
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = this.plugin.getDataManager().getData(player.getUniqueId());
            if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
                restore(player);
                continue;
            }
            UUID uuid = player.getUniqueId();
            int heldSlot = player.getInventory().getHeldItemSlot();
            int abilityIndex = data.getAbilityForSlot(heldSlot);
            if (abilityIndex == -1) {
                capture(player);
                player.setExp(1.0f);
                player.setLevel(0);
                this.wasReady.put(uuid, true);
                continue;
            }
            capture(player);
            boolean onCooldown = this.plugin.getCooldownManager().isOnCooldown(uuid, abilityIndex);
            if (onCooldown) {
                long remaining = this.plugin.getCooldownManager().getRemainingMillis(uuid, abilityIndex);
                int total = this.plugin.getBendingManager().cooldownSecondsFor(abilityIndex);
                float progress = total <= 0 ? 1.0f : (float) Math.max(0.0, Math.min(1.0, 1.0 - (remaining / (double) (total * 1000L))));
                player.setExp(progress);
                player.setLevel(abilityIndex + 1);
                this.wasReady.put(uuid, false);
            } else {
                player.setExp(1.0f);
                player.setLevel(abilityIndex + 1);
                Boolean previouslyReady = this.wasReady.get(uuid);
                if (previouslyReady == null || !previouslyReady) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
                }
                this.wasReady.put(uuid, true);
            }
        }
    }

    private void capture(Player player) {
        UUID uuid = player.getUniqueId();
        if (!this.savedLevel.containsKey(uuid)) {
            this.savedLevel.put(uuid, player.getLevel());
            this.savedExp.put(uuid, player.getExp());
        }
    }

    public void restore(Player player) {
        UUID uuid = player.getUniqueId();
        Integer level = this.savedLevel.remove(uuid);
        Float exp = this.savedExp.remove(uuid);
        this.wasReady.remove(uuid);
        if (level != null) {
            player.setLevel(level);
        }
        if (exp != null) {
            player.setExp(exp);
        }
    }

    public void shutdown() {
        if (this.task != null) {
            this.task.cancel();
        }
        for (UUID uuid : this.savedLevel.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                restore(player);
            }
        }
        this.savedLevel.clear();
        this.savedExp.clear();
        this.wasReady.clear();
    }
}