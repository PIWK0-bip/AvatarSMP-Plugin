package me.avatarsmp.core.skill.water;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.ISkill;
import me.avatarsmp.core.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TsunamiSkill implements ISkill {

    private static final Map<UUID, TsunamiChargeTask> CHARGING_PLAYERS = new ConcurrentHashMap<>();
    private final AvatarSMP plugin;

    public TsunamiSkill(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    public static boolean isCharging(Player player) {
        return CHARGING_PLAYERS.containsKey(player.getUniqueId());
    }

    public static void release(Player player) {
        TsunamiChargeTask task = CHARGING_PLAYERS.remove(player.getUniqueId());
        if (task != null) {
            task.launch();
        }
    }

    @Override
    public boolean execute(Player player, PlayerData data) {
        UUID uuid = player.getUniqueId();

        if (CHARGING_PLAYERS.containsKey(uuid)) {
            return false;
        }

        // 1. Sprawdzenie źródła wody w promieniu 10 bloków
        if (!isWaterSourceNearby(player.getLocation(), 10)) {
            player.sendActionBar(AvatarSMP.MM.deserialize("<red>Musisz znajdować się max 10 bloków od źródła wody!"));
            return false;
        }

        // 2. Rozpoczęcie ładowania
        TsunamiChargeTask chargeTask = new TsunamiChargeTask(player, plugin);
        CHARGING_PLAYERS.put(uuid, chargeTask);
        chargeTask.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Formujesz Tsunami... Trzymaj SHIFT!"));
        return true;
    }

    private boolean isWaterSourceNearby(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return false;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        Block b = world.getBlockAt(cX + x, cY + y, cZ + z);
                        if (b.getType() == Material.WATER) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // =========================================================================
    // TASK ŁADOWANIA FALI (PRZY TRZYMANIU SHIFT)
    // =========================================================================
    private static class TsunamiChargeTask extends BukkitRunnable {
        private final Player player;
        private final AvatarSMP plugin;
        private final Map<Block, BlockData> activeBlocks = new HashMap<>();
        
        private int ticks = 0;
        private final int MAX_CHARGE_TICKS = 60; // 3 sekundy do pełnego rozrostu (15x10)
        private boolean launched = false;

        public TsunamiChargeTask(Player player, AvatarSMP plugin) {
            this.player = player;
            this.plugin = plugin;
        }

        @Override
        public void run() {
            if (launched) return;

            if (!player.isOnline() || player.isDead() || !player.isSneaking()) {
                cleanup();
                CHARGING_PLAYERS.remove(player.getUniqueId());
                cancel();
                return;
            }

            ticks = Math.min(ticks + 1, MAX_CHARGE_TICKS);
            double progress = (double) ticks / MAX_CHARGE_TICKS;

            // Ładowane wymiary: od dołu do góry i od środka na boki
            double currentWidth = Math.max(1.0, 15.0 * progress);
            double currentHeight = Math.max(1.0, 10.0 * progress);

            // Rysowanie fali w świecie
            renderWaveShape(player.getLocation(), player.getEyeLocation().getDirection(), currentWidth, currentHeight);

            if (ticks % 5 == 0) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.8f, 0.6f + (float) progress * 0.4f);
            }
        }

        private void renderWaveShape(Location origin, Vector direction, double width, double height) {
            restoreBlocks();

            Vector forward = direction.clone().setY(0).normalize();
            Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

            Location startCenter = origin.clone().add(forward.clone().multiply(3)); // 3 bloki przed graczem

            int halfWidth = (int) Math.ceil(width / 2.0);
            int maxHeight = (int) Math.ceil(height);

            for (int h = 0; h < maxHeight; h++) {
                // Profil połówki serca / zagiętej grzywy
                double hRatio = (double) h / maxHeight;
                double forwardCurl = Math.sin(hRatio * Math.PI * 0.8) * 1.5 + (hRatio > 0.6 ? Math.pow(hRatio - 0.6, 2) * 4.0 : 0.0);

                for (int w = -halfWidth; w <= halfWidth; w++) {
                    double widthRatio = Math.abs((double) w / halfWidth);
                    if (widthRatio > 1.0) continue;

                    Location bLoc = startCenter.clone()
                            .add(right.clone().multiply(w))
                            .add(0, h, 0)
                            .add(forward.clone().multiply(forwardCurl));

                    Block block = bLoc.getBlock();
                    if (block.getType().isAir() || block.getType() == Material.WATER) {
                        if (!activeBlocks.containsKey(block)) {
                            activeBlocks.put(block, block.getBlockData().clone());
                        }
                        block.setType(Material.WATER, false);
                    }
                }
            }
        }

        public void launch() {
            if (launched) return;
            launched = true;

            restoreBlocks();
            cancel();

            double finalProgress = Math.max(0.2, (double) ticks / MAX_CHARGE_TICKS);
            double initialWidth = 15.0 * finalProgress;
            double initialHeight = 10.0 * finalProgress;

            Location startLoc = player.getLocation().clone();
            Vector direction = player.getEyeLocation().getDirection().clone().setY(0).normalize();

            player.getWorld().playSound(startLoc, Sound.ITEM_BUCKET_EMPTY, 1.0f, 0.5f);
            player.getWorld().playSound(startLoc, Sound.ENTITY_GENERIC_SPLASH, 1.2f, 0.6f);

            // Uruchomienie wolnego lotu tsunami
            new TsunamiTravelTask(plugin, player, startLoc, direction, initialWidth, initialHeight).runTaskTimer(plugin, 0L, 1L);
        }

        public void cleanup() {
            restoreBlocks();
        }

        private void restoreBlocks() {
            for (Map.Entry<Block, BlockData> entry : activeBlocks.entrySet()) {
                entry.getKey().setBlockData(entry.getValue(), false);
            }
            activeBlocks.clear();
        }
    }

    // =========================================================================
    // TASK LOTU TSUNAMI (PRZEMIESZCZANIE, ZNIEKSZTAŁCANIE I PRZYSPIESZANIE)
    // =========================================================================
    private static class TsunamiTravelTask extends BukkitRunnable {
        private final AvatarSMP plugin;
        private final Player caster;
        private final Location currentCenter;
        private final Vector forward;
        private final Vector right;

        private final double maxDistance = 50.0;
        private double traveledDistance = 0.0;

        private final double initialWidth;
        private final double initialHeight;

        private final Map<Block, BlockData> activeBlocks = new HashMap<>();

        public TsunamiTravelTask(AvatarSMP plugin, Player caster, Location startLoc, Vector direction, double width, double height) {
            this.plugin = plugin;
            this.caster = caster;
            this.forward = direction.clone().setY(0).normalize();
            this.right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
            this.currentCenter = startLoc.clone().add(forward.clone().multiply(3));
            this.initialWidth = width;
            this.initialHeight = height;
        }

        @Override
        public void run() {
            restoreBlocks();

            if (traveledDistance >= maxDistance) {
                cancel();
                return;
            }

            double distanceRatio = traveledDistance / maxDistance;

            // Animacja znikania od góry do dołu
            double currentHeight = initialHeight * (1.0 - distanceRatio);
            double currentWidth = initialWidth * (1.0 - (distanceRatio * 0.7));

            if (currentHeight < 0.8) {
                cancel();
                return;
            }

            // Bardzo powolny start (0.12 bloku/tick = ~2.4 bloku/s) do łagodnego przyspieszenia (0.40 bloku/tick = ~8 bloków/s)
            double currentSpeed = 0.12 + (distanceRatio * 0.28); 
            
            traveledDistance += currentSpeed;
            currentCenter.add(forward.clone().multiply(currentSpeed));

            int halfWidth = (int) Math.ceil(currentWidth / 2.0);
            int maxHeight = (int) Math.ceil(currentHeight);

            for (int h = 0; h < maxHeight; h++) {
                double hRatio = (double) h / maxHeight;
                double forwardCurl = Math.sin(hRatio * Math.PI * 0.8) * 1.5 + (hRatio > 0.6 ? Math.pow(hRatio - 0.6, 2) * 3.5 : 0.0);

                for (int w = -halfWidth; w <= halfWidth; w++) {
                    Location bLoc = currentCenter.clone()
                            .add(right.clone().multiply(w))
                            .add(0, h, 0)
                            .add(forward.clone().multiply(forwardCurl));

                    Block block = bLoc.getBlock();
                    if (block.getType().isAir() || block.getType() == Material.WATER) {
                        if (!activeBlocks.containsKey(block)) {
                            activeBlocks.put(block, block.getBlockData().clone());
                        }
                        block.setType(Material.WATER, false);
                    }
                }
            }

            affectEntities(currentWidth, currentHeight);

            if ((int) traveledDistance % 6 == 0) {
                currentCenter.getWorld().playSound(currentCenter, Sound.ENTITY_GENERIC_SWIM, 0.9f, 0.7f + (float) distanceRatio * 0.5f);
            }
        }

        private void affectEntities(double width, double height) {
            double searchRadius = Math.max(width, height);
            for (Entity entity : currentCenter.getWorld().getNearbyEntities(currentCenter, searchRadius, height, searchRadius)) {
                if (entity instanceof LivingEntity target && entity != caster) {
                    Vector push = forward.clone().multiply(0.8).setY(0.25);
                    target.setVelocity(push);
                    target.damage(6.0, caster);
                }
            }
        }

        private void restoreBlocks() {
            for (Map.Entry<Block, BlockData> entry : activeBlocks.entrySet()) {
                entry.getKey().setBlockData(entry.getValue(), false);
            }
            activeBlocks.clear();
        }
    }
}