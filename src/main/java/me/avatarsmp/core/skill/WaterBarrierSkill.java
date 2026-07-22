package me.avatarsmp.core.skill;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.ISkill;
import me.avatarsmp.core.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class WaterBarrierSkill implements ISkill {
    private static final Set<Player> ACTIVE = new HashSet<>();
    private final AvatarSMP plugin;

    public WaterBarrierSkill(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, PlayerData data) {
        if (ACTIVE.contains(player)) return false;
        if (!player.isSneaking()) return false;

        ACTIVE.add(player);
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Wodna Bariera!"));

        new BukkitRunnable() {
            private final int radius = 6; // Zwiększone z 4 do 6 bloków
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking() || ticks > 140) {
                    ACTIVE.remove(player);
                    cancel();
                    return;
                }
                drawParticleDome();
                repelEntities();
                ticks++;
            }

            private void drawParticleDome() {
                Location center = player.getLocation().add(0, 0.5, 0);
                // Matematyczne generowanie punktów na półsferze (kopule)
                for (int i = 0; i < 60; i++) {
                    double theta = Math.random() * Math.PI * 2;
                    double phi = Math.acos(Math.random()); // Tylko górna półsfera (od 0 do PI/2)
                    
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.cos(phi);
                    double z = radius * Math.sin(phi) * Math.sin(theta);
                    
                    Location pLoc = center.clone().add(x, y, z);
                    pLoc.getWorld().spawnParticle(Particle.SPLASH, pLoc, 1, 0, 0, 0, 0);
                }
            }

            private void repelEntities() {
                Location center = player.getLocation();
                for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, radius + 0.5, radius, radius + 0.5)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        Vector push = living.getLocation().toVector().subtract(center.toVector()).normalize().setY(0.2).multiply(1.4);
                        living.setVelocity(push);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Uruchamiane co tick dla idealnej płynności wizualnej

        return true;
    }
}