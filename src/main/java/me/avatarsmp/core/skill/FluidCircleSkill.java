package me.avatarsmp.core.skill;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.ISkill;
import me.avatarsmp.core.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FluidCircleSkill implements ISkill {

    private static final Set<Player> ACTIVE = new HashSet<>();
    private final AvatarSMP plugin;

    public FluidCircleSkill(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, PlayerData data) {
        if (ACTIVE.contains(player)) return false;
        if (!player.isSneaking()) return false;

        // Znajdź najbliższe źródło wody
        Block waterSource = findNearestWaterBlock(player.getLocation(), 8);
        if (waterSource == null) {
            player.sendActionBar(AvatarSMP.MM.deserialize("<red>Brak źródła wody w pobliżu (max 8 bloków)."));
            return false;
        }

        ACTIVE.add(player);
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Płynny Krąg!"));
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 0.8f);

        Location sourceLoc = waterSource.getLocation().add(0.5, 0.5, 0.5);

        new BukkitRunnable() {
            private final Map<Location, BlockData> originalBlocks = new HashMap<>();
            private int ticks = 0;
            private boolean sourceConnected = false;
            private double travelProgress = 0.0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking() || ticks > 120) {
                    release();
                    return;
                }

                // ETAP 1: Animacja przyciągania wody ze źródła do gracza
                if (!sourceConnected) {
                    travelProgress += 0.15; // Prędkość przepływu wody
                    Location playerLoc = player.getLocation().add(0, 1.0, 0);

                    // Pozycja cząsteczki pędzącej do gracza
                    Vector dir = playerLoc.toVector().subtract(sourceLoc.toVector());
                    double distance = dir.length();
                    dir.normalize();

                    Location currentParticleLoc = sourceLoc.clone().add(dir.clone().multiply(distance * travelProgress));
                    currentParticleLoc.getWorld().spawnParticle(Particle.SPLASH, currentParticleLoc, 8, 0.1, 0.1, 0.1, 0.05);
                    currentParticleLoc.getWorld().spawnParticle(Particle.DRIPPING_WATER, currentParticleLoc, 3, 0.05, 0.05, 0.05, 0.01);

                    if (travelProgress >= 1.0) {
                        sourceConnected = true; // Woda dotarła do gracza
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.2f);
                    }
                } else {
                    // ETAP 2: Formowanie i utrzymywanie kręgu wody wokół gracza
                    updateCircle();
                }

                ticks++;
            }

            private void updateCircle() {
                // Przywracanie starych bloków
                for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                    entry.getKey().getBlock().setBlockData(entry.getValue(), false);
                }
                originalBlocks.clear();

                Location center = player.getLocation();
                double radius = 3.0;
                int points = 16;
                double offsetAngle = ticks * 0.2;

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i / points) + offsetAngle;
                    Location loc = center.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius);
                    Block block = loc.getBlock();
                    if (block.getType().isAir()) {
                        originalBlocks.put(loc, block.getBlockData());
                        block.setType(Material.WATER, false);
                    }
                }
            }

            private void release() {
                for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                    entry.getKey().getBlock().setBlockData(entry.getValue(), false);
                }
                originalBlocks.clear();
                ACTIVE.remove(player);

                Location center = player.getLocation();

                // Wybuch wody na boki po puszczeniu Shifta
                for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, 6, 3, 6)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        Vector push = living.getLocation().toVector().subtract(center.toVector());
                        if (push.lengthSquared() == 0) {
                            push = new Vector(0, 0.1, 0);
                        } else {
                            push.normalize().setY(0.4).multiply(1.8);
                        }
                        living.setVelocity(push);
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        living.damage(5.0, player);
                    }
                }

                // Efekt fali eksplozji
                for (int deg = 0; deg < 360; deg += 12) {
                    double rad = Math.toRadians(deg);
                    for (double r = 1.0; r <= 6.0; r += 1.0) {
                        Location pLoc = center.clone().add(Math.cos(rad) * r, 0.5, Math.sin(rad) * r);
                        center.getWorld().spawnParticle(Particle.SPLASH, pLoc, 1, 0, 0, 0, 0);
                    }
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Block findNearestWaterBlock(Location center, int radius) {
        Block nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.WATER) {
                        double dist = center.distanceSquared(block.getLocation());
                        if (dist < minDistance) {
                            minDistance = dist;
                            nearest = block;
                        }
                    }
                }
            }
        }
        return nearest;
    }
}