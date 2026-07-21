package me.avatarsmp.core.skill;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.ISkill;
import me.avatarsmp.core.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EarthBoulderSkill implements ISkill {

    private static final Set<String> EARTH_MATERIALS = Set.of(
            "STONE", "COBBLESTONE", "GRASS_BLOCK", "DIRT", "COARSE_DIRT", "ROOTED_DIRT",
            "PODZOL", "MYCELIUM", "GRAVEL", "SAND", "RED_SAND", "CLAY", "MUD",
            "PACKED_MUD", "MOSS_BLOCK", "ANDESITE", "DIORITE", "GRANITE", "DEEPSLATE",
            "COBBLED_DEEPSLATE", "TUFF", "CALCITE", "DRIPSTONE_BLOCK", "TERRACOTTA",
            "NETHERRACK", "SOUL_SOIL", "BLACKSTONE", "BASALT", "END_STONE", "SNOW_BLOCK"
    );
    private static final Map<Location, BlockData> PENDING_RESTORE = new HashMap<>();
    private static final Set<BoulderProjectile> ACTIVE_PROJECTILES = new HashSet<>();
    private static final Material AIR = XMaterial.matchXMaterial("AIR")
            .map(XMaterial::parseMaterial)
            .orElseThrow(() -> new IllegalStateException("Materiał AIR nie jest dostępny"));

    private final AvatarSMP plugin;

    public EarthBoulderSkill(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, PlayerData data) {
        Block source = findSourceBlock(player);
        if (source == null) {
            player.sendActionBar(AvatarSMP.MM.deserialize(
                    "<red>Potrzebujesz bloku ziemi lub skały w zasięgu."));
            return false;
        }

        BlockBreakEvent breakEvent = new BlockBreakEvent(source, player);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            player.sendActionBar(AvatarSMP.MM.deserialize(
                    "<red>Nie możesz manipulować terenem w tym miejscu."));
            return false;
        }

        Location sourceLocation = source.getLocation();
        if (PENDING_RESTORE.containsKey(sourceLocation)) {
            return false;
        }

        BlockData originalData = source.getBlockData().clone();
        PENDING_RESTORE.put(sourceLocation.clone(), originalData);
        source.setBlockData(AIR.createBlockData(), false);

        Vector direction = player.getEyeLocation().getDirection();
        if (direction.lengthSquared() == 0.0) {
            restore(sourceLocation);
            return false;
        }

        BoulderProjectile projectile = new BoulderProjectile(
                this.plugin, player, sourceLocation, originalData, direction.normalize());
        try {
            projectile.start();
        } catch (RuntimeException exception) {
            restore(sourceLocation);
            this.plugin.getLogger().severe("Nie udało się uruchomić umiejętności Głaz: "
                    + exception.getMessage());
            return false;
        }

        player.sendMessage(AvatarSMP.MM.deserialize("<green><bold>Głaz!"));
        return true;
    }

    private Block findSourceBlock(Player player) {
        Block target = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (isEarthMaterial(target)) {
            return target;
        }

        Block below = player.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
        return isEarthMaterial(below) ? below : null;
    }

    private boolean isEarthMaterial(Block block) {
        return block != null && EARTH_MATERIALS.contains(
                XMaterial.matchXMaterial(block.getType()).name());
    }

    private static void restore(Location location) {
        BlockData originalData = PENDING_RESTORE.remove(location);
        if (originalData == null || location.getWorld() == null) {
            return;
        }

        BlockState state = location.getBlock().getState();
        state.setBlockData(originalData);
        state.update(true, false);
    }

    public static void restoreAll() {
        for (BoulderProjectile projectile : new HashSet<>(ACTIVE_PROJECTILES)) {
            projectile.finish(false);
        }
        for (Location location : new HashSet<>(PENDING_RESTORE.keySet())) {
            restore(location);
        }
        ACTIVE_PROJECTILES.clear();
    }

    private static final class BoulderProjectile extends BukkitRunnable {

        private static final int LIFT_TICKS = 10;
        private static final int MAX_TICKS = 90;
        private static final double SPEED = 0.8;

        private final AvatarSMP plugin;
        private final Player caster;
        private final Location sourceLocation;
        private final BlockData blockData;
        private final Vector direction;
        private Location current;
        private BlockDisplay display;
        private int ticks;
        private boolean finished;

        private BoulderProjectile(AvatarSMP plugin, Player caster, Location sourceLocation,
                                  BlockData blockData, Vector direction) {
            this.plugin = plugin;
            this.caster = caster;
            this.sourceLocation = sourceLocation.clone();
            this.blockData = blockData;
            this.direction = direction;
            this.current = sourceLocation.clone().add(0.5, 0.15, 0.5);
        }

        private void start() {
            this.display = this.current.getWorld().spawn(this.current, BlockDisplay.class, entity -> {
                entity.setBlock(this.blockData);
                entity.setTeleportDuration(2);
                entity.setInterpolationDuration(2);
            });
            ACTIVE_PROJECTILES.add(this);
            runTaskTimer(this.plugin, 0L, 1L);
        }

        @Override
        public void run() {
            if (this.finished || !this.caster.isOnline() || !this.display.isValid()) {
                finish(false);
                return;
            }

            this.ticks++;
            if (this.ticks > MAX_TICKS) {
                finish(false);
                return;
            }

            if (this.ticks <= LIFT_TICKS) {
                this.current.add(0.0, 0.14, 0.0);
                this.display.teleport(this.current);
                renderOrbit(0.32);
                if (this.ticks % 2 == 0) {
                    float pitch = 0.55f + (this.ticks * 0.07f);
                    XSound.matchXSound("BLOCK_STONE_PLACE")
                            .ifPresent(sound -> sound.play(this.caster, 0.65f, pitch));
                }
                return;
            }

            RayTraceResult entityHit = this.current.getWorld().rayTraceEntities(
                    this.current, this.direction, SPEED + 0.55, 0.7,
                    this::isValidTarget);
            if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity target) {
                target.damage(12.0, this.caster);
                this.current = target.getLocation().add(0.0, target.getHeight() * 0.5, 0.0);
                impact();
                return;
            }

            RayTraceResult blockHit = this.current.getWorld().rayTraceBlocks(
                    this.current, this.direction, SPEED + 0.55,
                    FluidCollisionMode.NEVER, true);
            if (blockHit != null && blockHit.getHitPosition() != null) {
                this.current = blockHit.getHitPosition().toLocation(this.current.getWorld());
                impact();
                return;
            }

            this.current.add(this.direction.clone().multiply(SPEED));
            this.display.teleport(this.current);
            renderOrbit(0.44);
            if (this.ticks % 5 == 0) {
                float pitch = Math.min(1.8f, 0.75f + (this.ticks * 0.012f));
                XSound.matchXSound("BLOCK_STONE_HIT")
                        .ifPresent(sound -> sound.play(this.caster, 0.45f, pitch));
            }
        }

        private boolean isValidTarget(Entity entity) {
            return entity instanceof LivingEntity && entity != this.caster;
        }

        private void renderOrbit(double radius) {
            Vector side = this.direction.clone().crossProduct(new Vector(0.0, 1.0, 0.0));
            if (side.lengthSquared() < 0.01) {
                side = new Vector(1.0, 0.0, 0.0);
            }
            side.normalize();
            Vector vertical = this.direction.clone().crossProduct(side).normalize();
            double angle = this.ticks * 0.72;

            Location first = this.current.clone()
                    .add(side.clone().multiply(Math.cos(angle) * radius))
                    .add(vertical.clone().multiply(Math.sin(angle) * radius));
            Location second = this.current.clone()
                    .add(side.clone().multiply(Math.cos(angle + Math.PI) * radius))
                    .add(vertical.clone().multiply(Math.sin(angle + Math.PI) * radius));

            XParticle.of("BLOCK").ifPresent(particle -> {
                this.current.getWorld().spawnParticle(
                        particle.get(), first, 2, 0.03, 0.03, 0.03, 0.0, this.blockData);
                this.current.getWorld().spawnParticle(
                        particle.get(), second, 2, 0.03, 0.03, 0.03, 0.0, this.blockData);
            });
        }

        private void impact() {
            XParticle.of("BLOCK").ifPresent(particle -> {
                for (int latitude = 0; latitude <= 6; latitude++) {
                    double phi = Math.PI * latitude / 6.0;
                    for (int longitude = 0; longitude < 12; longitude++) {
                        double theta = Math.PI * 2.0 * longitude / 12.0;
                        Vector offset = new Vector(
                                Math.sin(phi) * Math.cos(theta),
                                Math.cos(phi),
                                Math.sin(phi) * Math.sin(theta)
                        ).multiply(1.35);
                        Location point = this.current.clone().add(offset);
                        this.current.getWorld().spawnParticle(
                                particle.get(), point, 2, 0.08, 0.08, 0.08, 0.08, this.blockData);
                    }
                }
            });
            XSound.matchXSound("ENTITY_GENERIC_EXPLODE")
                    .ifPresent(sound -> sound.play(this.caster, 1.2f, 0.62f));
            finish(true);
        }

        private void finish(boolean impacted) {
            if (this.finished) {
                return;
            }
            this.finished = true;
            ACTIVE_PROJECTILES.remove(this);
            if (this.display != null && this.display.isValid()) {
                this.display.remove();
            }
            restore(this.sourceLocation);
            if (!isCancelled()) {
                cancel();
            }
        }
    }
}