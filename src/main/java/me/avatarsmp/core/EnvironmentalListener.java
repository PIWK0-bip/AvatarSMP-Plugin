package me.avatarsmp.core;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.Set;

public class EnvironmentalListener implements Listener {

    private static final Set<Material> NATURAL_GROUND = EnumSet.of(Material.GRASS_BLOCK, Material.DIRT, Material.STONE);

    private final AvatarSMP plugin;
    private final DataManager dataManager;

    public EnvironmentalListener(AvatarSMP plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        new BukkitRunnable() {
            @Override
            public void run() {
                applyPassives();
            }
        }.runTaskTimer(plugin, 20L, 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                spawnTrails();
            }
        }.runTaskTimer(plugin, 5L, 4L);
    }

    public double getFireDamageMultiplier(Player player) {
        World world = player.getWorld();
        double multiplier = 1.0;
        boolean isDay = world.getTime() >= 0 && world.getTime() < 12300;
        if (world.getEnvironment() == World.Environment.NETHER || isDay) {
            multiplier += 0.20;
        }
        if (world.hasStorm() && world.getEnvironment() == World.Environment.NORMAL) {
            multiplier -= 0.20;
        }
        return Math.max(0.1, multiplier);
    }

    public double getWaterDamageMultiplier(Player player) {
        World world = player.getWorld();
        boolean rain = world.hasStorm() && world.getEnvironment() == World.Environment.NORMAL;
        boolean inWater = player.isInWater();
        return (rain || inWater) ? 1.2 : 1.0;
    }

    public void applyPassives() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data == null) {
                continue;
            }
            World world = player.getWorld();
            switch (data.getElement()) {
                case WATER -> {
                    boolean night = world.getTime() >= 13000 && world.getTime() <= 23000;
                    boolean inWater = player.getLocation().getBlock().getType() == Material.WATER;
                    if (world.hasStorm() || inWater || night) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false));
                    }
                }
                case EARTH -> {
                    Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                    if (NATURAL_GROUND.contains(below.getType())) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false));
                    }
                }
                case AIR -> {
                    player.setAllowFlight(true);
                    double y = player.getLocation().getY();
                    if (y > 120) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, true, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 1, true, false));
                    } else if (y > 100) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
                    }
                }
                case WARRIOR -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0, true, false));
                }
                default -> {
                }
            }
        }
    }

    public void spawnTrails() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isSprinting()) {
                continue;
            }
            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data == null) {
                continue;
            }
            Location loc = player.getLocation();
            switch (data.getElement()) {
                case FIRE -> loc.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.2, 0.05, 0.2, 0.01);
                case EARTH -> loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 3, 0.2, 0.05, 0.2, 0,
                        Material.DIRT.createBlockData());
                case WATER -> loc.getWorld().spawnParticle(Particle.SPLASH, loc, 3, 0.2, 0.05, 0.2, 0.01);
                case AIR -> loc.getWorld().spawnParticle(Particle.CLOUD, loc, 3, 0.2, 0.05, 0.2, 0.01);
                default -> {
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && plugin.getBendingManager().isInAvatarState(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLowHealthCheck(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.isCancelled()) {
            return;
        }
        PlayerData data = this.dataManager.getData(player.getUniqueId());
        if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
            return;
        }
        double predictedHealth = player.getHealth() - event.getFinalDamage();
        if (predictedHealth > 0 && predictedHealth <= 4.0) {
            this.plugin.getBendingManager().triggerLowHealthAvatarState(player);
        }
    }

    @EventHandler
    public void onFireDamageModifier(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        PlayerData data = this.dataManager.getData(damager.getUniqueId());
        if (data == null || data.getElement() != Element.FIRE) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.setDamage(event.getDamage() * getFireDamageMultiplier(damager));
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        PlayerData data = this.dataManager.getData(player.getUniqueId());
        if (data == null || data.getElement() != Element.AIR) {
            return;
        }
        event.setCancelled(true);
        player.setFlying(false);
        if (!data.isDoubleJumpAvailable() || player.isOnGround()) {
            return;
        }
        data.setDoubleJumpAvailable(false);
        Vector boost = player.getLocation().getDirection().setY(0).normalize().multiply(0.6).setY(0.9);
        player.setVelocity(boost);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOnGround()) {
            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data != null) {
                data.setDoubleJumpAvailable(true);
            }
        }
    }
}