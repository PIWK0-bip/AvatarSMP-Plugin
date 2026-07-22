package me.avatarsmp.core.skill.water;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.BendingManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WaterSpoutSkill {

    public static void execute(AvatarSMP plugin, Player player, double waterMultiplier, BendingManager manager) {
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Wodna Fala Ślizgowa!"));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 80 || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                Vector dir = loc.getDirection().setY(0).normalize();

                // Płynna zmiana prędkości gracza
                double lift = player.isSneaking() ? -0.15 : 0.35;
                player.setVelocity(dir.multiply(0.65).setY(lift));

                // Tworzenie wirującej trąby wodnej pod nogami (animowana podwójna helisa)
                for (int h = 0; h < 3; h++) {
                    double angle = (ticks * 0.4) + (h * 0.8);
                    double radius = 0.7 - (h * 0.15); // Zwężanie w dół

                    double x1 = Math.cos(angle) * radius;
                    double z1 = Math.sin(angle) * radius;
                    double x2 = Math.cos(angle + Math.PI) * radius;
                    double z2 = Math.sin(angle + Math.PI) * radius;

                    Location p1 = loc.clone().subtract(0, h * 0.6, 0).add(x1, 0, z1);
                    Location p2 = loc.clone().subtract(0, h * 0.6, 0).add(x2, 0, z2);

                    loc.getWorld().spawnParticle(Particle.SPLASH, p1, 3, 0.02, 0.02, 0.02, 0.02);
                    loc.getWorld().spawnParticle(Particle.FISHING, p2, 2, 0.02, 0.02, 0.02, 0.02);
                }

                // Odpychanie i obrażenia dla pobliskich celów
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.2, 2.0, 2.2)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        Vector push = living.getLocation().toVector().subtract(loc.toVector()).normalize().setY(0.3).multiply(1.1);
                        living.setVelocity(push);
                        manager.damage(player, living, 2 * waterMultiplier);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}