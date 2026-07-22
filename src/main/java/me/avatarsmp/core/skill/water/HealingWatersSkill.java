package me.avatarsmp.core.skill.water;

import me.avatarsmp.core.AvatarSMP;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class HealingWatersSkill {

    public static void execute(AvatarSMP plugin, Player player) {
        Location loc = player.getLocation();
        player.playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.0f);
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Krąg Uzdrawiania!"));

        // Płynna animacja wznoszącej się i obracającej spali wodnej przez 2 sekundy (40 ticków)
        new BukkitRunnable() {
            int ticks = 0;
            final double radius = 2.0;

            @Override
            public void run() {
                if (ticks >= 40 || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                Location currentLoc = player.getLocation();
                
                // Matematyczne obliczenie pozycji cząsteczek w kształcie spirali
                double angle = ticks * 0.3;
                double yOffset = (ticks % 20) * 0.1; // Fala wznosi się i opada
                
                double x1 = Math.cos(angle) * radius;
                double z1 = Math.sin(angle) * radius;
                double x2 = Math.cos(angle + Math.PI) * radius; // Druga strona dla symetrii
                double z2 = Math.sin(angle + Math.PI) * radius;

                Location p1 = currentLoc.clone().add(x1, yOffset, z1);
                Location p2 = currentLoc.clone().add(x2, yOffset, z2);

                currentLoc.getWorld().spawnParticle(Particle.FISHING, p1, 2, 0.05, 0.05, 0.05, 0);
                currentLoc.getWorld().spawnParticle(Particle.SPLASH, p2, 2, 0.05, 0.05, 0.05, 0);

                // Aplikowanie leczenia i zdejmoawnie ognia co 10 ticków (0.5s)
                if (ticks % 10 == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 1));
                    player.setFireTicks(0);

                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 5, 3, 5)) {
                        if (entity instanceof Player ally) {
                            ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 1));
                            ally.setFireTicks(0);
                        }
                    }
                    currentLoc.getWorld().spawnParticle(Particle.HEART, currentLoc.clone().add(0, 2, 0), 2, 0.4, 0.2, 0.4, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}