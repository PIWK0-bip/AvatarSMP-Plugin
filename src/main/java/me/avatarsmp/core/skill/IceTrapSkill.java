package me.avatarsmp.core.skill;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.ISkill;
import me.avatarsmp.core.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class IceTrapSkill implements ISkill {
    private final AvatarSMP plugin;
    private static final Map<UUID, MarkedTarget> MARKED_TARGETS = new HashMap<>();

    public IceTrapSkill(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    public static class MarkedTarget {
        private final LivingEntity entity;
        private final BukkitTask expireTask;

        public MarkedTarget(LivingEntity entity, BukkitTask expireTask) {
            this.entity = entity;
            this.expireTask = expireTask;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public void cancel() {
            if (expireTask != null) {
                expireTask.cancel();
            }
            if (entity.isValid()) {
                entity.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }

    @Override
    public boolean execute(Player player, PlayerData data) {
        UUID pUUID = player.getUniqueId();
        MarkedTarget marked = MARKED_TARGETS.get(pUUID);

        // KROK 2: Jeśli byt jest już podświetlony -> AKTYWACJA PUŁAPKI
        if (marked != null && marked.getEntity().isValid() && !marked.getEntity().isDead()) {
            LivingEntity target = marked.getEntity();
            marked.cancel();
            MARKED_TARGETS.remove(pUUID);

            startTrapProcess(player, target);
            return true;
        } 
        // KROK 1: OZNACZENIE BYTU
        else {
            LivingEntity target = getTargetEntity(player, 6.0);
            if (target == null) {
                player.sendActionBar(AvatarSMP.MM.deserialize("<red>Brak celu w zasięgu!"));
                return false;
            }

            markEntity(player, target);
            return true;
        }
    }

    public void markEntity(Player player, LivingEntity target) {
        UUID pUUID = player.getUniqueId();

        if (MARKED_TARGETS.containsKey(pUUID)) {
            MARKED_TARGETS.get(pUUID).cancel();
        }

        // Podświetlenie na 4 sekundy (80 ticków)
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0, false, false));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        player.sendMessage(AvatarSMP.MM.deserialize("<aqua>Oznaczono cel! Kliknij <yellow>LPM<aqua> ponownie, aby go zamrozić."));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid()) {
                    target.removePotionEffect(PotionEffectType.GLOWING);
                }
                MARKED_TARGETS.remove(pUUID);
            }
        }.runTaskLater(plugin, 80L);

        MARKED_TARGETS.put(pUUID, new MarkedTarget(target, task));
    }

    private LivingEntity getTargetEntity(Player player, double range) {
        var ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                1.2,
                e -> e instanceof LivingEntity && e != player
        );
        if (ray != null && ray.getHitEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private void startTrapProcess(Player player, LivingEntity target) {
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 0.8f);
        
        // POPRAWKA: Użycie target.getWorld().playSound(...) z podaniem target.getLocation()
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BOAT_PADDLE_WATER, 1.0f, 1.0f);

        new BukkitRunnable() {
            private int ticks = 0;
            private final Map<Location, BlockData> originalBlocks = new HashMap<>();
            private final Material[] iceTypes = {Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE};
            private final Random random = new Random();

            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    restoreBlocks();
                    cancel();
                    return;
                }

                // FAZA 1: Woda otaczająca cel (3 sekundy = 60 ticków)
                if (ticks < 60) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 3, false, false));
                    updateWaterBlocks(target.getLocation());

                    if (ticks % 10 == 0) {
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.8f, 1.0f);
                    }
                }
                // FAZA 2: Zamrażanie w różne rodzaje lodu (2 sekundy = 40 ticków)
                else if (ticks < 100) {
                    if (ticks == 60) {
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.6f);
                    }
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 255, false, false));
                    target.setFreezeTicks(Math.min(target.getFreezeTicks() + 10, 140));

                    updateIceBlocks();
                }
                // KONIEC: Przywrócenie bloków
                else {
                    restoreBlocks();
                    cancel();
                    return;
                }

                ticks++;
            }

            private void updateWaterBlocks(Location center) {
                List<Location> targetLocs = getSurroundingLocations(center);
                for (Location loc : targetLocs) {
                    if (!originalBlocks.containsKey(loc)) {
                        BlockData oldData = loc.getBlock().getBlockData();
                        if (loc.getBlock().getType().isAir() || loc.getBlock().getType() == Material.WATER) {
                            originalBlocks.put(loc, oldData);
                            loc.getBlock().setType(Material.WATER, false);
                        }
                    }
                }
            }

            private void updateIceBlocks() {
                for (Location loc : originalBlocks.keySet()) {
                    Material iceMat = iceTypes[random.nextInt(iceTypes.length)];
                    loc.getBlock().setType(iceMat, false);
                }
            }

            private List<Location> getSurroundingLocations(Location center) {
                List<Location> locs = new ArrayList<>();
                Location base = center.getBlock().getLocation();
                for (int y = 0; y <= 1; y++) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            locs.add(base.clone().add(x, y, z));
                        }
                    }
                }
                return locs;
            }

            private void restoreBlocks() {
                for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                    entry.getKey().getBlock().setBlockData(entry.getValue(), false);
                }
                originalBlocks.clear();
            }

        }.runTaskTimer(plugin, 0L, 1L);
    }
}