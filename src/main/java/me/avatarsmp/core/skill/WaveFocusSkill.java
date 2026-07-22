package me.avatarsmp.core.skill;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.ISkill;
import me.avatarsmp.core.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class WaveFocusSkill implements ISkill {
    private static final Map<Player, ChargeTask> CHARGES = new HashMap<>();
    private static final Map<Player, Long> COOLDOWNS = new HashMap<>();
    private final AvatarSMP plugin;
    private final double damageMultiplier;

    public WaveFocusSkill(AvatarSMP plugin, double damageMultiplier) {
        this.plugin = plugin;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public boolean execute(Player player, PlayerData data) {
        long now = System.currentTimeMillis();
        if (COOLDOWNS.containsKey(player)) {
            long timeLeft = COOLDOWNS.get(player) - now;
            if (timeLeft > 0) {
                return false;
            }
        }
        if (CHARGES.containsKey(player)) {
            return false;
        }

        // USUNIĘTO WARUNEK player.isSneaking()!
        // Zdarzenie 'PlayerToggleSneakEvent' potierdziło już wciśnięcie SHIFT-a.

        ChargeTask task = new ChargeTask(player);
        CHARGES.put(player, task);
        task.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    public static boolean isHoldingSphere(Player player) {
        return CHARGES.containsKey(player);
    }

    public static void shootWaterSphere(Player player) {
        ChargeTask task = CHARGES.remove(player);
        if (task != null) {
            task.shoot();
        }
    }

    private class ChargeTask extends BukkitRunnable {
        private final Player player;
        private int ticks = 0;
        private boolean shot = false;
        private final Map<Location, BlockData> currentWaterBlocks = new HashMap<>();

        public ChargeTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            if (shot) return;

            // Kula po naładowaniu utrzymuje się do 100 ticków (5 sekund) lub do momentu strzału LPM
            if (!player.isOnline() || ticks >= 100) {
                cleanupBlocks();
                CHARGES.remove(player);
                cancel();
                return;
            }
            ticks = Math.min(ticks + 1, 60);

            Location back = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(-2.5)).add(0, 1.5, 0);
            double currentRadius = 0.4 + (ticks / 60.0);

            updateWaterSphereBlocks(back, currentRadius);
            spawnWaterParticles(back, currentRadius);

            if (ticks % 10 == 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.0f);
            }
        }

        private void updateWaterSphereBlocks(Location center, double r) {
            cleanupBlocks();
            int blockRadius = (int) Math.ceil(r);
            for (int x = -blockRadius; x <= blockRadius; x++) {
                for (int y = -blockRadius; y <= blockRadius; y++) {
                    for (int z = -blockRadius; z <= blockRadius; z++) {
                        if (x * x + y * y + z * z <= r * r) {
                            Location bLoc = center.clone().add(x, y, z);
                            if (bLoc.getBlock().getType().isAir()) {
                                currentWaterBlocks.put(bLoc.getBlock().getLocation(), bLoc.getBlock().getBlockData());
                                bLoc.getBlock().setType(Material.WATER, false);
                            }
                        }
                    }
                }
            }
        }

        private void spawnWaterParticles(Location center, double r) {
            for (int i = 0; i < 12; i++) {
                double theta = Math.random() * 2 * Math.PI;
                double phi = Math.acos(2 * Math.random() - 1);
                double x = r * Math.sin(phi) * Math.cos(theta);
                double y = r * Math.sin(phi) * Math.sin(theta);
                double z = r * Math.cos(phi);
                center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }

        private void cleanupBlocks() {
            for (Map.Entry<Location, BlockData> entry : currentWaterBlocks.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue(), false);
            }
            currentWaterBlocks.clear();
        }

        public void shoot() {
            shot = true;
            cleanupBlocks();
            cancel();

            COOLDOWNS.put(player, System.currentTimeMillis() + 1000L);

            player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Skupienie Fali!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.8f);
            double finalDamage = (4.0 + (ticks / 6.0)) * damageMultiplier;
            new BukkitRunnable() {
                private Location current = player.getEyeLocation();
                private final Vector direction = player.getEyeLocation().getDirection().normalize();
                private int life = 0;
                private final double radius = 0.4 + (ticks / 60.0);

                @Override
                public void run() {
                    if (life++ > 30) {
                        cancel();
                        return;
                    }
                    current.add(direction.clone().multiply(1.2));
                    current.getWorld().spawnParticle(Particle.SPLASH, current, 15, radius, radius, radius, 0.05);
                    RayTraceResult hit = current.getWorld().rayTraceEntities(current, direction, 1.2, radius, e -> e instanceof LivingEntity && e != player);
                    if (hit != null && hit.getHitEntity() instanceof LivingEntity target) {
                        target.damage(finalDamage, player);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

                        Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().setY(0.3).multiply(1.5);
                        target.setVelocity(push);

                        current.getWorld().spawnParticle(Particle.SPLASH, current, 60, radius + 1, radius + 1, radius + 1, 0.1);
                        cancel();
                        return;
                    }
                    if (!current.getBlock().isPassable()) {
                        current.getWorld().spawnParticle(Particle.SPLASH, current, 60, radius + 1, radius + 1, radius + 1, 0.1);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }
}