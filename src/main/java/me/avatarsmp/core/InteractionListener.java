package me.avatarsmp.core;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player; // Added missing import
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionListener implements Listener {
    private record WaterStream(UUID owner, List<Location> points, long expiry) {
    }

    private final AvatarSMP plugin;
    private final BendingManager bendingManager; // Declared missing variable
    private final Map<UUID, Long> wetUntil = new ConcurrentHashMap<>();
    private final List<WaterStream> activeStreams = new ArrayList<>();

    public InteractionListener(AvatarSMP plugin, BendingManager bendingManager) {
        this.plugin = plugin;
        this.bendingManager = bendingManager; // Initialized bendingManager
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    public void addWaterStream(UUID owner, List<Location> points) {
        this.activeStreams.add(new WaterStream(owner, points, System.currentTimeMillis() + 3000L));
    }

    public void markWet(LivingEntity entity, int seconds) {
        this.wetUntil.put(entity.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean isWet(LivingEntity entity) {
        Long expiry = this.wetUntil.get(entity.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        this.activeStreams.removeIf(stream -> stream.expiry() < now);
        if (this.activeStreams.isEmpty()) return;

        for (WaterStream stream : this.activeStreams) {
            for (Location point : stream.points()) {
                if (point.getWorld() == null) continue;
                for (Entity entity : point.getWorld().getNearbyEntities(point, 1.5, 1.5, 1.5)) {
                    if (entity instanceof SmallFireball) {
                        point.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                        entity.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onLightningConduction(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (!isWet(victim)) {
            return;
        }

        Location loc = victim.getLocation();
        for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (nearby instanceof LivingEntity living && nearby != victim) {
                living.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, living.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
                living.setHealth(Math.max(0.0, living.getHealth() - 6.0));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.wetUntil.remove(event.getPlayer().getUniqueId());
        this.activeStreams.removeIf(stream -> stream.owner().equals(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        int currentSlot = player.getInventory().getHeldItemSlot();
        if (currentSlot == 3) {
            boolean executed = bendingManager.activateSkill(player, 3, "ATTACK", target);
            if (executed) {
                event.setDamage(1.0);
            }
        }
    }
}