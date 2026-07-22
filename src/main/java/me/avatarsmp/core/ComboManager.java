package me.avatarsmp.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.avatarsmp.core.data.PlayerData;

public class ComboManager {

    public enum ActionType {
        LEFT, RIGHT, SHIFT
    }

    record ActionEntry(ActionType type, long time) {
    }

    private static final long WINDOW_MILLIS = 2000L;
    private static final List<ActionType> SECRET_SEQUENCE = List.of(ActionType.LEFT, ActionType.RIGHT, ActionType.SHIFT);

    private final AvatarSMP plugin;
    private final Map<UUID, List<ActionEntry>> history = new HashMap<>();

    public ComboManager(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    public void registerAction(Player player, ActionType type) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getData(uuid);
        if (data == null || data.getElement() == Element.WARRIOR || data.getElement() == Element.NONE) {
            return;
        }
        long now = System.currentTimeMillis();
        List<ActionEntry> list = this.history.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.removeIf(entry -> now - entry.time() > WINDOW_MILLIS);
        list.add(new ActionEntry(type, now));
        if (list.size() > 3) {
            list.subList(0, list.size() - 3).clear();
        }
        if (list.size() < 3) {
            return;
        }
        List<ActionType> lastThree = List.of(list.get(list.size() - 3).type(), list.get(list.size() - 2).type(), list.get(list.size() - 1).type());
        if (!lastThree.equals(SECRET_SEQUENCE)) {
            return;
        }
        list.clear();
        switch (data.getElement()) {
            case FIRE -> fireCombo(player);
            case WATER -> waterCombo(player);
            case EARTH -> earthCombo(player);
            case AIR -> airCombo(player);
            default -> {
            }
        }
    }

    private void fireCombo(Player player) {
        player.sendMessage(AvatarSMP.MM.deserialize("<blue><bold>Niebieski Płomień!"));
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        World world = player.getWorld();
        for (double d = 0; d < 20; d += 0.3) {
            Location point = start.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 2, 0.05, 0.05, 0.05, 0.01);
        }
        RayTraceResult result = world.rayTraceEntities(start, direction, 20, 0.5, e -> e instanceof LivingEntity && e != player);
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.setHealth(Math.max(0.0, target.getHealth() - 15.0));
            target.setFireTicks(60);
        }
    }

    private void waterCombo(Player player) {
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Lustrzane Odbicie!"));
        Location base = player.getLocation();
        World world = player.getWorld();
        List<ArmorStand> clones = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 / 3) * i;
            Location loc = base.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setSilent(true);
                as.setGravity(false);
            });
            clones.add(stand);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0));
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 80) {
                    clones.forEach(Entity::remove);
                    this.cancel();
                    return;
                }
                for (ArmorStand stand : clones) {
                    stand.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, stand.getLocation().add(0, 1, 0), 3, 0.2, 0.4, 0.2, 0.01);
                }
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void earthCombo(Player player) {
        player.sendMessage(AvatarSMP.MM.deserialize("<green><bold>Forteca!"));
        Location center = player.getLocation();
        Map<Block, BlockData> original = new HashMap<>();
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq > radius * radius - 1 && distSq <= radius * radius + 1) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        original.put(block, block.getBlockData().clone());
                        block.setType(org.bukkit.Material.COBBLESTONE);
                    }
                }
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Block, BlockData> entry : original.entrySet()) {
                    BlockState state = entry.getKey().getState();
                    state.setBlockData(entry.getValue());
                    state.update(true, false);
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    private void airCombo(Player player) {
        player.sendMessage(AvatarSMP.MM.deserialize("<white><bold>Soniczny Podmuch!"));
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1);
        for (Entity entity : center.getWorld().getNearbyEntities(center, 8, 8, 8)) {
            if (entity instanceof LivingEntity living && entity != player) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4));
            }
        }
    }

    public void clearPlayer(UUID uuid) {
        this.history.remove(uuid);
    }
}