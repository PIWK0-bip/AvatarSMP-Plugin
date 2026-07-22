package me.avatarsmp.core.skill.water;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.BendingManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Particle;

import java.util.HashSet;
import java.util.Set;

public class IceShardsSkill {

    public static void execute(AvatarSMP plugin, Player player, double waterMultiplier, BendingManager manager) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Lodowe Sople!"));

        Vector baseDir = player.getEyeLocation().getDirection().normalize();
        double[] angleOffsets = {-0.15, 0.0, 0.15};

        // Wypuszcza 3 sople lecące jako osobne obiekty w czasie
        for (double offset : angleOffsets) {
            Vector dir = baseDir.clone().add(new Vector(offset, 0, offset)).normalize();
            
            new BukkitRunnable() {
                Vector currentPos = player.getEyeLocation().toVector();
                final Set<LivingEntity> hitTargets = new HashSet<>();
                int step = 0;

                @Override
                public void run() {
                    // Maksymalny zasięg 18 bloków (36 kroków po 0.5 bloku)
                    if (step >= 36) {
                        cancel();
                        return;
                    }

                    currentPos.add(dir.clone().multiply(0.5));
                    var loc = currentPos.toLocation(player.getWorld());

                    // Kolizja z blokami
                    if (loc.getBlock().getType().isSolid()) {
                        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 10, 0.2, 0.2, 0.2, Bukkit.createBlockData(Material.ICE));
                        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                        cancel();
                        return;
                    }

                    // Płynny ślad cząsteczek sopla lodu
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.02, 0.02, 0.02, 0.01);
                    loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, loc, 1, 0, 0, 0, 0);

                    // Kolizja z bytami
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity living && e != player && !hitTargets.contains(living)) {
                            hitTargets.add(living);
                            manager.damage(player, living, 5 * waterMultiplier);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                            
                            loc.getWorld().spawnParticle(Particle.BLOCK, loc, 15, 0.2, 0.2, 0.2, Bukkit.createBlockData(Material.ICE));
                            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
                            cancel();
                            return;
                        }
                    }
                    step++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }
}