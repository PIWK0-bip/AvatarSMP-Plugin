package me.avatarsmp.core.skill.task;

import me.avatarsmp.core.AvatarSMP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class WaterSphereTask extends BukkitRunnable {
    private final Player player;
    private final AvatarSMP plugin;
    private int ticks = 0;
    private final int MAX_TICKS = 160; // 8 sekund (20 ticks/sec * 8)
    private final Map<Location, BlockData> placedWater = new HashMap<>();
    private boolean launched = false;

    public WaterSphereTask(Player player, AvatarSMP plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void startCharging() {
        this.runTaskTimer(plugin, 0L, 1L); // Animacja co 1 tick
    }

    @Override
    public void run() {
        if (!player.isOnline() || player.isDead()) {
            cleanup();
            return;
        }

        ticks++;

        // --- 1. AURA SPOWOLNIENIA WOKÓŁ GRACZA (BEZ OBRAŻEŃ) ---
        double radius = 2.5 + (ticks / 40.0); // Promień rośnie od 2.5 do 6.5 bloku
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity living && entity != player) {
                // Nakładamy spowolnienie 2 (brak jakichkolwiek obrażeń!)
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false, true));
            }
        }

        // --- 2. ETAPY FORMY KULI ---
        
        // ETAP 1 (0.0s - 2.5s / 0-50 ticks): Tylko cząsteczki
        if (ticks < 50) {
            spawnParticleStage(0.8 + (ticks * 0.02));
        } 
        // ETAP 2 (2.5s - 5.5s / 50-110 ticks): Średnia kula wody
        else if (ticks < 110) {
            spawnParticleStage(1.6);
            updateWaterSphere(1.2);
        } 
        // ETAP 3 (5.5s - 8.0s / 110-160 ticks): Największa forma kuli
        else {
            spawnParticleStage(2.4);
            updateWaterSphere(2.0);
        }

        // Efekt dźwiękowy narastania energii co 10 ticków (0.5s)
        if (ticks % 10 == 0) {
            float pitch = 0.5f + ((float) ticks / MAX_TICKS) * 1.0f;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.6f, pitch);
        }

        // Powiadomienie o pełnym naładowaniu
        if (ticks == MAX_TICKS) {
            player.sendActionBar(AvatarSMP.MM.deserialize("<gradient:#00d2ff:#3a7bd5><bold>KULA WODY W PEŁNI NAŁADOWANA!</gradient>"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.5f);
        }
    }

    private void spawnParticleStage(double rad) {
        Location center = player.getLocation().add(0, 1.2, 0);
        int points = 10;
        double angleOffset = ticks * 0.2;

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i / points) + angleOffset;
            double x = Math.cos(angle) * rad;
            double z = Math.sin(angle) * rad;

            Location pLoc = center.clone().add(x, Math.sin(ticks * 0.1 + i) * 0.4, z);
            player.getWorld().spawnParticle(Particle.SPLASH, pLoc, 1, 0, 0, 0, 0);
            player.getWorld().spawnParticle(Particle.DRIPPING_WATER, pLoc, 1, 0, 0, 0, 0);
        }
    }

    private void updateWaterSphere(double rad) {
        restoreWaterBlocks(); // Czyszczenie starych bloków z poprzedniej klatki

        Location center = player.getLocation().add(0, 1.2, 0);
        int points = 14;

        for (int i = 0; i < points; i++) {
            double u = Math.random() * Math.PI * 2;
            double v = Math.random() * Math.PI;
            double x = rad * Math.sin(v) * Math.cos(u);
            double y = rad * Math.cos(v);
            double z = rad * Math.sin(v) * Math.sin(u);

            Location bLoc = center.clone().add(x, y, z);
            Block b = bLoc.getBlock();
            if (b.getType().isAir()) {
                placedWater.put(bLoc, b.getBlockData());
                b.setType(Material.WATER, false);
            }
        }
    }

    public void launch() {
        if (launched) return;
        launched = true;

        restoreWaterBlocks();
        cancel();

        // Skalowanie siły i obrażeń w zależności od czasu ładowania (od 0.0 do 1.0)
        double chargeRatio = Math.min(1.0, ticks / (double) MAX_TICKS);
        
        // Spamowanie (szybkie kliknięcie) daje znikome obrażenia (np. 2 HP). 
        // Max naładowanie (8 sek) daje aż 16 HP (8 serc) + duży odrzut.
        double damage = 2.0 + (chargeRatio * 14.0);
        double speed = 1.0 + (chargeRatio * 1.5);

        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection().normalize();

        player.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.6f + ((float) chargeRatio * 0.8f));

        // Pocisk po wypuszczeniu
        new BukkitRunnable() {
            private final Location currentLoc = loc.clone();
            private int distance = 0;

            @Override
            public void run() {
                currentLoc.add(dir.clone().multiply(speed));
                currentLoc.getWorld().spawnParticle(Particle.DRIPPING_WATER, currentLoc, 10, 0.2, 0.2, 0.2, 0.05);
                currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 15, 0.3, 0.3, 0.3, 0.05);

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity living && e != player) {
                        living.damage(damage, player);
                        living.setVelocity(dir.clone().multiply(1.8 * chargeRatio).setY(0.4));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (40 + chargeRatio * 80), 2));

                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
                        currentLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, currentLoc, 1);
                        cancel();
                        return;
                    }
                }

                if (currentLoc.getBlock().getType().isSolid() || distance++ > 35) {
                    currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 25, 0.4, 0.4, 0.4, 0.1);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void cleanup() {
        restoreWaterBlocks();
        cancel();
    }

    private void restoreWaterBlocks() {
        for (Map.Entry<Location, BlockData> entry : placedWater.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue(), false);
        }
        placedWater.clear();
    }
}