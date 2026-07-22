package me.avatarsmp.core;

import org.bukkit.Bukkit;
import me.avatarsmp.core.data.PlayerData;
import me.avatarsmp.core.skill.EarthBoulderSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.avatarsmp.core.skill.task.WaterSphereTask;
import me.avatarsmp.core.skill.water.FluidCircleSkill;
import me.avatarsmp.core.skill.water.WaterBarrierSkill;
import me.avatarsmp.core.skill.water.WaveFocusSkill;
import me.avatarsmp.core.skill.water.HealingWatersSkill;
import me.avatarsmp.core.skill.water.IceShardsSkill;
import me.avatarsmp.core.skill.water.WaterSpoutSkill;

public class BendingManager {

    private final AvatarSMP plugin;
    private final DataManager dataManager;
    private final CooldownManager cooldownManager;
    private final SkillManager skillManager;
    private final Set<UUID> avatarStatePlayers = new HashSet<>();
    private final Map<UUID, WaterSphereTask> activeCharges = new HashMap<>();
    private InteractionListener interactionListener;
    private EnvironmentalListener environmentalListener;
    private EnergyManager energyManager;

    public BendingManager(AvatarSMP plugin, DataManager dataManager, CooldownManager cooldownManager,
                          SkillManager skillManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.cooldownManager = cooldownManager;
        this.skillManager = skillManager;
        registerSkills();
    }

    private void registerSkills() {
        Map<Element, SkillExecutor> executors = new EnumMap<>(Element.class);
        executors.put(Element.FIRE, this::fireAbility);
        executors.put(Element.WATER, this::waterAbility);
        executors.put(Element.EARTH, this::earthAbility);
        executors.put(Element.AIR, this::airAbility);

        executors.forEach((element, executor) -> {
            for (int slot = 0; slot < 8; slot++) {
                int skillSlot = slot;
                this.skillManager.register(element, skillSlot,
                        (player, data) -> executor.execute(player, skillSlot, data));
            }
        });
    }

    @FunctionalInterface
    private interface SkillExecutor {
        boolean execute(Player player, int slot, PlayerData data);
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void setEnvironmentalListener(EnvironmentalListener environmentalListener) {
        this.environmentalListener = environmentalListener;
    }

    public void setEnergyManager(EnergyManager energyManager) {
        this.energyManager = energyManager;
    }

    public boolean isInAvatarState(UUID uuid) {
        return this.avatarStatePlayers.contains(uuid);
    }

    public void setAvatarState(UUID uuid, boolean state) {
        if (state) {
            this.avatarStatePlayers.add(uuid);
        } else {
            this.avatarStatePlayers.remove(uuid);
        }
    }

    public void triggerLowHealthAvatarState(Player player) {
        UUID uuid = player.getUniqueId();
        if (isInAvatarState(uuid)) {
            return;
        }
        PlayerData data = this.dataManager.getData(uuid);
        if (data == null || System.currentTimeMillis() < data.getAvatarStateCooldownUntil()) {
            return;
        }
        new AvatarStateTask(this.plugin, player, 300, true).start();
    }

    public boolean isSlotUnlocked(PlayerData data, int slot) {
        if (this.avatarStatePlayers.contains(data.getUuid())) {
            return true;
        }
        int level = data.getLevel();
        return switch (slot) {
            case 0, 1 -> level >= 1;
            case 2 -> level >= 5;
            case 3 -> level >= 10;
            case 4 -> level >= 15;
            case 5 -> level >= 20;
            case 6 -> level >= 25;
            case 7 -> level >= 50;
            default -> false;
        };
    }

    // Dodaj to na początku klasy BendingManager jako pole klasy:
    private final Map<UUID, Long> clickDelay = new HashMap<>();

    // Zaktualizowana metoda activateSkill:
    // Overload metody activateSkill pozwalający przekazać uderzony byt
public boolean activateSkill(Player player, int slot, String trigger) {
    return activateSkill(player, slot, trigger, null);
}

public boolean activateSkill(Player player, int slot, String trigger, LivingEntity directTarget) {
    UUID uuid = player.getUniqueId();
    PlayerData data = this.dataManager.getData(uuid);

    if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
        return false;
    }

    if (data.isChiBlocked()) {
        player.sendActionBar(AvatarSMP.MM.deserialize("<red>Nie możesz bendować - Chi zablokowane!"));
        return false;
    }

    if (!isSlotUnlocked(data, slot)) {
        player.sendActionBar(AvatarSMP.MM.deserialize("<gray>Ta umiejętność jest jeszcze zablokowana."));
        return false;
    }

    // 1. Zmiana: Dynamiczne pobieranie cooldownu z configu (z użyciem metody omówionej wcześniej)
    int cdTime = cooldownSecondsFor(data.getElement(), slot);

    if (this.cooldownManager.isOnCooldown(uuid, slot)) {
        // Usunięto zhardcodowane 15 sekund dla wody, używamy pobranego cdTime
        this.cooldownManager.sendCooldownActionbar(player, slot, cdTime);
        return false;
    }

    // 2. Zmiana: Dynamiczne pobieranie kosztu energii (zamiast sztywnego 40.0 lub 10.0)
    // Tworzy ścieżkę np: elements.WATER.energy.3
    double defaultEnergy = (slot == 7) ? 40.0 : 10.0; 
    String energyPath = "elements." + data.getElement().name() + ".energy." + slot;
    double energyCost = this.plugin.getConfig().getDouble(energyPath, defaultEnergy);

    if (this.energyManager != null && !this.energyManager.consume(uuid, energyCost)) {
        player.sendActionBar(AvatarSMP.MM.deserialize("<dark_purple>Za mało energii Chi!"));
        return false;
    }

    // Przekazujemy directTarget do egzekucji umiejętności wody
    boolean success;
    if (data.getElement() == Element.WATER && slot == 3) {
        success = executeWaterTrap(player, data, directTarget);
    } else {
        ISkill skill = this.skillManager.getSkill(data.getElement(), slot);
        success = skill != null && skill.execute(player, data);
    }

    if (!success) {
        if (this.energyManager != null) {
            this.energyManager.consume(uuid, -energyCost); // Zwrócenie energii
        }
        return false;
    }

    // 3. Zmiana: Ustawienie cooldownu używając wartości z configu
    this.cooldownManager.setCooldown(uuid, slot, cdTime);
    
    playElementSound(player, data.getElement());
    this.dataManager.saveAsync(data);

    if (this.plugin.getScoreboardManager() != null) {
        this.plugin.getScoreboardManager().update(player);
    }
    return true;
}

/**
 * Logika Lodowej Pułapki po uderzeniu w byt
 */
private boolean executeWaterTrap(Player player, PlayerData data, LivingEntity directTarget) {
    double waterMultiplier = this.environmentalListener != null ? this.environmentalListener.getWaterDamageMultiplier(player) : 1.0;
    
    // Jeśli nie przekazano celu z eventu uderzenia, próbujemy go namierzyć raytracem
    LivingEntity target = directTarget != null ? directTarget : getTargetEntity(player, 4);

    if (target == null) {
        player.sendActionBar(AvatarSMP.MM.deserialize("<red>Uderz byt, aby zamknąć go w wodnej pułapce!"));
        return false;
    }

    player.sendMessage(AvatarSMP.MM.deserialize("<aqua><bold>Lodowa Pułapka aktywowana!"));
    target.setGlowing(true);

    Location targetLoc = target.getLocation().getBlock().getLocation();
    Map<Block, BlockData> originalBlocks = new HashMap<>();

    // Zapisujemy oryginalne bloki 3x3x3 wokół celu
    for (int x = -1; x <= 1; x++) {
        for (int y = 0; y <= 2; y++) {
            for (int z = -1; z <= 1; z++) {
                Block b = targetLoc.clone().add(x, y, z).getBlock();
                if (b.isPassable() || b.getType().isAir()) {
                    originalBlocks.putIfAbsent(b, b.getBlockData());
                }
            }
        }
    }

    Material[] iceTypes = {Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE};

    new BukkitRunnable() {
        int ticks = 0;

        @Override
        public void run() {
            // Faza 1: Woda otacza cel poziom po poziomie
            if (ticks == 0) spawnWaterLayer(targetLoc, 0, originalBlocks);
            if (ticks == 4) spawnWaterLayer(targetLoc, 1, originalBlocks);
            if (ticks == 8) spawnWaterLayer(targetLoc, 2, originalBlocks);

            // Blokowanie ruchu celu wewnątrz wody/lodu
            if (ticks <= 120) {
                target.teleport(targetLoc.clone().add(0.5, 0, 0.5));
            }

            // Faza 2: Zamiana wody w lód po 1 sekundzie (20 ticków)
            if (ticks == 20) {
                for (Block b : originalBlocks.keySet()) {
                    Material randomIce = iceTypes[java.util.concurrent.ThreadLocalRandom.current().nextInt(iceTypes.length)];
                    b.setType(randomIce, false);
                }
                target.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            }

            // Obrażenia od mrozu podczas uwięzienia
            if (ticks >= 20 && ticks <= 120 && ticks % 10 == 0) {
                damage(player, target, 1.5 * waterMultiplier);
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            }

            ticks++;

            // Faza 3: Koniec umiejętności i przywrócenie bloków
            if (ticks > 120) {
                for (Map.Entry<Block, BlockData> entry : originalBlocks.entrySet()) {
                    entry.getKey().setBlockData(entry.getValue(), false);
                }
                target.setGlowing(false);
                cancel();
            }
        }
    }.runTaskTimer(plugin, 0L, 1L);

    return true;
}

    public void shootWaterSphere(Player player) {
        UUID uuid = player.getUniqueId();
        
        WaterSphereTask task = activeCharges.remove(uuid);
        if (task != null) {
            task.launch();
        } else if (WaveFocusSkill.isHoldingSphere(player)) {
            WaveFocusSkill.shootWaterSphere(player);
            this.cooldownManager.setCooldown(uuid, 2, cooldownSeconds(2));
        }
    }

    public boolean isHoldingSphere(Player player) {
        return activeCharges.containsKey(player.getUniqueId()) || WaveFocusSkill.isHoldingSphere(player);
    }

    public void handleSneak(Player player) {
        PlayerData data = this.dataManager.getData(player.getUniqueId());
        if (data == null) {
            return;
        }
        switch (data.getElement()) {
            case AIR -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
            case EARTH -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0));
            case WATER -> player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0));
            default -> {
            }
        }
    }

    private int cooldownSeconds(int slot) {
        if (slot == 4) {
            return 60; // Cooldown 1 minuta dla Tsunami
        }
        return slot == 7 ? 60 : 5 + (slot * 2);
    }

    public int cooldownSecondsFor(Element element, int slot) {
        if (element == null || element == Element.NONE) return 5;
        String path = "elements." + element.name() + ".cooldowns." + slot;
        return plugin.getConfig().getInt(path, 5); // To poprawnie czyta config!
    }

    public void grantXp(Player player, int amount) {
        PlayerData data = this.dataManager.getData(player.getUniqueId());
        if (data == null || data.getElement() == Element.NONE) {
            return;
        }
        addXp(data, player, amount);
        this.dataManager.saveAsync(data);
    }

    public static final int MAX_LEVEL = 50;

    public int xpRequiredForNextLevel(int level) {
        if (level >= MAX_LEVEL) {
            return cumulativeXpForLevel(MAX_LEVEL);
        }
        return cumulativeXpForLevel(level + 1);
    }

    public int cumulativeXpForLevel(int level) {
        if (level <= 1) return 0;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
    
        int l = level - 1;
        return (50 * l * l) + (100 * l);
    }

    private void addXp(PlayerData data, Player player, int amount) {
        if (data.getLevel() >= MAX_LEVEL) {
            data.setLevel(MAX_LEVEL);
            data.setXp(cumulativeXpForLevel(MAX_LEVEL));
            return;
        }
        data.setXp(data.getXp() + amount);
        int nextLevel = data.getLevel() + 1;
        
        // Dodano ograniczenie data.getLevel() < MAX_LEVEL
        while (data.getLevel() < MAX_LEVEL && data.getXp() >= cumulativeXpForLevel(nextLevel)) {
            data.setLevel(nextLevel);
            
            player.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>Awans! <white>Osi to poziom " + data.getLevel() + "!"));
            player.showTitle(Title.title(
                    AvatarSMP.MM.deserialize("<gold><bold>AWANS!"),
                    AvatarSMP.MM.deserialize("<white>Poziom " + data.getLevel())));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            if (data.getLevel() == MAX_LEVEL) {
                data.setXp(cumulativeXpForLevel(MAX_LEVEL));
            }
            nextLevel = data.getLevel() + 1;
        }
    }
    
    private LivingEntity getTargetEntity(Player player, double range) {
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),
                range, 0.4, entity -> entity instanceof LivingEntity && entity != player);
        if (result == null || result.getHitEntity() == null) {
            return null;
        }
        return result.getHitEntity() instanceof LivingEntity living ? living : null;
    }

    public void damage(Player caster, LivingEntity target, double amount) {
        target.setHealth(Math.max(0.0, target.getHealth() - amount));
        if (!(target instanceof Player)) {
            PlayerData casterData = this.dataManager.getData(caster.getUniqueId());
            if (casterData != null) {
                addXp(casterData, caster, 2);
            }
        }
    }

    private boolean fireAbility(Player player, int slot, PlayerData data) {
        double multiplier = this.environmentalListener != null ? this.environmentalListener.getFireDamageMultiplier(player) : 1.0;
        switch (slot) {
            case 0 -> {
                LivingEntity target = getTargetEntity(player, 4);
                if (target == null) {
                    return false;
                }
                damage(player, target, 4 * multiplier);
                target.setFireTicks(60);
                player.sendMessage(AvatarSMP.MM.deserialize("<red>Ognisty Cios!"));
            }
            case 1 -> {
                LivingEntity target = getTargetEntity(player, 15);
                spawnDoubleHelix(player.getEyeLocation(), Particle.FLAME, Particle.LARGE_SMOKE, 1.0, 20, 0.1);
                if (target == null) {
                    return false;
                }
                damage(player, target, 6 * multiplier);
                target.setFireTicks(80);
                player.sendMessage(AvatarSMP.MM.deserialize("<red>Ognisty Podmuch!"));
            }
            case 2 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0));
                player.sendMessage(AvatarSMP.MM.deserialize("<red>Ognista Osłona!"));
            }
            case 3 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        damage(player, living, 5 * multiplier);
                        living.setFireTicks(60);
                    }
                }
                player.getWorld().spawnParticle(Particle.FLAME, loc, 40, 2, 1, 2, 0.1);
                player.sendMessage(AvatarSMP.MM.deserialize("<red>Falisty Ogień!"));
            }
            case 4 -> {
                LivingEntity target = getTargetEntity(player, 10);
                if (target == null) {
                    return false;
                }
                Location loc = target.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                    if (entity instanceof LivingEntity living) {
                        damage(player, living, 8 * multiplier);
                    }
                }
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                player.sendMessage(AvatarSMP.MM.deserialize("<red>Kombustia!"));
            }
            case 5 -> {
                LivingEntity target = getTargetEntity(player, 12);
                if (target == null) {
                    return false;
                }
                target.setVelocity(target.getVelocity().add(new Vector(0, 1.2, 0)));
                damage(player, target, 7 * multiplier);
                target.setFireTicks(60);
                player.sendMessage(AvatarSMP.MM.deserialize("<red>Ognisty Słup!"));
            }
            case 6 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 6, 4, 6)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        damage(player, living, 10 * multiplier);
                        living.setFireTicks(100);
                    }
                }
                loc.getWorld().spawnParticle(Particle.LAVA, loc, 30, 3, 1, 3, 0.1);
                player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                player.sendMessage(AvatarSMP.MM.deserialize("<red><bold>Inferno!"));
            }
            case 7 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 8, 5, 8)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        damage(player, living, 14 * multiplier);
                        living.setFireTicks(140);
                        Vector push = living.getLocation().toVector().subtract(loc.toVector()).normalize().setY(0.4);
                        living.setVelocity(push.multiply(1.6));
                    }
                }
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 100, 4, 2, 4, 0.15);
                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 40, 3, 2, 3, 0.1);
                player.playSound(loc, Sound.BLOCK_LAVA_POP, 1.5f, 0.8f);
                player.sendMessage(AvatarSMP.MM.deserialize("<red><bold>Płomienna Nowa!"));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

private boolean waterAbility(Player player, int slot, PlayerData data) {
    double waterMultiplier = this.environmentalListener != null ? this.environmentalListener.getWaterDamageMultiplier(player) : 1.0;
    switch (slot) {
        case 0 -> {
            return new FluidCircleSkill(this.plugin).execute(player, data);
        }
        case 1 -> {
            return new WaterBarrierSkill(this.plugin).execute(player, data);
        }
        case 2 -> {
            return new WaveFocusSkill(this.plugin, waterMultiplier).execute(player, data);
        }
        case 3 -> {
            // 1. Pobieramy cel uderzenia (wymagany kontakt fizyczny - max 3 bloki)
            LivingEntity target = getTargetEntity(player, 3);
            if (target == null) {
                return false; // Brak celu w zasięgu uderzenia
            }

            // 2. Cooldown 15 sekund
            if (this.cooldownManager.isOnCooldown(player.getUniqueId(), slot)) {
                return false;
            }
            this.cooldownManager.setCooldown(player.getUniqueId(), slot, cooldownSecondsFor(Element.WATER, slot));

            // 3. Efekt wizualny podświetlenia celu
            target.setGlowing(true);
            Location targetLoc = target.getLocation().getBlock().getLocation();
            Map<Block, BlockData> originalBlocks = new HashMap<>();

            // Zbiór bloków 3x3x3 wokół uderzonego bytu
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block b = targetLoc.clone().add(x, y, z).getBlock();
                        if (b.isPassable() || b.getType().isAir()) {
                            originalBlocks.putIfAbsent(b, b.getBlockData());
                        }
                    }
                }
            }

            player.sendMessage(AvatarSMP.MM.deserialize("<aqua>Lodowa Pułapka aktywowana!"));
            Material[] iceTypes = {Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE};

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    // --- ANIMACJA ROŚNIĘCIA WODY (0-5s) ---
                    // Warstwa 1: Stopy (y = 0) - natychmiast przy uderzeniu
                    if (ticks == 0) {
                        spawnWaterLayer(targetLoc, 0, originalBlocks);
                    }
                    // Warstwa 2: Klatka (y = 1) - po 0.3s (6 ticków)
                    if (ticks == 6) {
                        spawnWaterLayer(targetLoc, 1, originalBlocks);
                    }
                    // Warstwa 3: Głowa (y = 2) - po 0.6s (12 ticków)
                    if (ticks == 12) {
                        spawnWaterLayer(targetLoc, 2, originalBlocks);
                    }

                    // Unieruchomienie celu w miejscu na cały czas trwania pułapki (8 sekund = 160 ticków)
                    if (ticks <= 160) {
                        target.teleport(targetLoc.clone().add(0.5, 0, 0.5));
                    }

                    // --- ZAMRAŻANIE (Po 5 sekundach = 100 ticków) ---
                    if (ticks == 100) {
                        for (Block b : originalBlocks.keySet()) {
                            Material randomIce = iceTypes[java.util.concurrent.ThreadLocalRandom.current().nextInt(iceTypes.length)];
                            b.setType(randomIce, false);
                        }
                    }

                    // --- OBRAŻENIA PODCZAS ZAMROŻENIA (Trwają przez 3 sekundy: od 100 do 160 ticków) ---
                    if (ticks >= 100 && ticks <= 160 && ticks % 10 == 0) {
                        damage(player, target, 1.25 * waterMultiplier);
                    }

                    ticks++;

                    // --- SPRZĄTANIE (Po 8 sekundach) ---
                    if (ticks > 160) {
                        for (Map.Entry<Block, BlockData> entry : originalBlocks.entrySet()) {
                            entry.getKey().setBlockData(entry.getValue(), false);
                        }
                        target.setGlowing(false);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        case 4 -> {
            return new me.avatarsmp.core.skill.water.TsunamiSkill(this.plugin).execute(player, data);
        }
        case 5 -> HealingWatersSkill.execute(plugin, player);
        case 6 -> IceShardsSkill.execute(plugin, player, waterMultiplier, this);
        case 7 -> WaterSpoutSkill.execute(plugin, player, waterMultiplier, this);
        default -> {
            return false;
        }
    }
    return true;
}

    private boolean earthAbility(Player player, int slot, PlayerData data) {
        switch (slot) {
            case 0 -> {
                LivingEntity target = getTargetEntity(player, 5);
                if (target == null) {
                    return false;
                }
                damage(player, target, 5);
                player.sendMessage(AvatarSMP.MM.deserialize("<green>Rzut Kamieniem!"));
            }
            case 1 -> {
                LivingEntity target = getTargetEntity(player, 8);
                if (target == null) {
                    return false;
                }
                damage(player, target, 6);
                target.setVelocity(target.getVelocity().add(new Vector(0, 0.8, 0)));
                player.sendMessage(AvatarSMP.MM.deserialize("<green>Kamienny Grot!"));
            }
            case 2 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 0));
                player.sendMessage(AvatarSMP.MM.deserialize("<green>Kamienna Zbroja!"));
            }
            case 3 -> {
                Location base = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
                for (int i = -1; i <= 1; i++) {
                    for (int y = 0; y < 3; y++) {
                        var block = base.clone().add(i, y, 0).getBlock();
                        if (block.getType().isAir()) {
                            block.setType(org.bukkit.Material.COBBLESTONE);
                            var loc = block.getLocation();
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                if (loc.getBlock().getType() == org.bukkit.Material.COBBLESTONE) {
                                    loc.getBlock().setType(org.bukkit.Material.AIR);
                                }
                            }, 100L);
                        }
                    }
                }
                player.sendMessage(AvatarSMP.MM.deserialize("<green>Kamienna Ściana!"));
            }
            case 4 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 3, 5)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        damage(player, living, 6);
                        living.setVelocity(living.getVelocity().add(new Vector(0, 0.6, 0)));
                    }
                }
                spawnExpandingRings(loc, Material.STONE);
                player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                player.sendMessage(AvatarSMP.MM.deserialize("<green>Trzęsienie Ziemi!"));
            }
            case 5 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 3, 5)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                    }
                }
                player.sendMessage(AvatarSMP.MM.deserialize("<green>Ruchome Piaski!"));
            }
            case 6 -> {
                return new EarthBoulderSkill(this.plugin).execute(player, data);
            }
            case 7 -> {
                LivingEntity target = getTargetEntity(player, 12);
                if (target == null) {
                    return false;
                }
                damage(player, target, 14);
                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 60, 1, 1, 1, 0.1,
                        org.bukkit.Material.STONE.createBlockData());
                player.sendMessage(AvatarSMP.MM.deserialize("<green><bold>Kamienna Furia!"));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean airAbility(Player player, int slot, PlayerData data) {
        switch (slot) {
            case 0 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 2, 3)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        Vector push = living.getLocation().toVector().subtract(loc.toVector()).normalize().setY(0.2);
                        living.setVelocity(push.multiply(1.5));
                    }
                }
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Podmuch Powietrza!"));
            }
            case 1 -> {
                LivingEntity target = getTargetEntity(player, 5);
                if (target == null) {
                    return false;
                }
                damage(player, target, 3);
                target.setVelocity(target.getVelocity().add(player.getLocation().getDirection().multiply(1.2)));
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Uderzenie Powietrza!"));
            }
            case 2 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0));
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 2, 3)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        Vector push = living.getLocation().toVector().subtract(loc.toVector()).normalize();
                        living.setVelocity(push.multiply(1.2));
                    }
                }
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Osłona Powietrza!"));
            }
            case 3 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 6, 3, 6)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        Vector pull = loc.toVector().subtract(living.getLocation().toVector()).normalize().setY(0.1);
                        living.setVelocity(pull.multiply(1.3));
                    }
                }
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Zasysanie Powietrza!"));
            }
            case 4 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        living.setVelocity(living.getVelocity().add(new Vector(0, 1.5, 0)));
                    }
                }
                spawnTornado(loc);
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Tornado!"));
            }
            case 5 -> {
                player.setVelocity(player.getLocation().getDirection().multiply(2.2).setY(0.3));
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Podryw!"));
            }
            case 6 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
                player.setVelocity(player.getVelocity().add(new Vector(0, 0.5, 0)));
                player.sendMessage(AvatarSMP.MM.deserialize("<white>Wodospad Powietrza!"));
            }
            case 7 -> {
                Location loc = player.getLocation();
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 8, 5, 8)) {
                    if (entity instanceof LivingEntity living && entity != player) {
                        living.setVelocity(living.getVelocity().add(new Vector(0, 1.8, 0)));
                        damage(player, living, 6);
                    }
                }
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 1, 0.5, 1, 0);
                player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 1.0f);
                player.sendMessage(AvatarSMP.MM.deserialize("<white><bold>Huragan!"));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spawnSpiral(Location center, Particle particle, double radius, int points, double heightStep) {
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double y = i * heightStep;
            Location point = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    private void spawnDoubleHelix(Location center, Particle primary, Particle secondary, double radius, int points, double heightStep) {
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double y = i * heightStep;
            Location pointA = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Location pointB = center.clone().add(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius);
            pointA.getWorld().spawnParticle(primary, pointA, 1, 0, 0, 0, 0);
            pointB.getWorld().spawnParticle(secondary, pointB, 1, 0, 0, 0, 0);
        }
    }

    private void spawnRing(Location center, Particle particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    private void spawnExpandingRings(Location center, Material material) {
        for (int r = 1; r <= 4; r++) {
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i;
                Location point = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                point.getWorld().spawnParticle(Particle.BLOCK, point, 1, 0, 0, 0, 0, material.createBlockData());
            }
        }
    }

    private void spawnWave(Location center, double radius) {
        for (int i = 0; i < 24; i++) {
            double angle = (2 * Math.PI / 24) * i;
            double y = Math.sin(angle * 3) * 0.3;
            Location point = center.clone().add(Math.cos(angle) * radius, y + 0.2, Math.sin(angle) * radius);
            point.getWorld().spawnParticle(Particle.SPLASH, point, 1, 0, 0.05, 0, 0.01);
        }
    }

    private void spawnTornado(Location center) {
        for (int h = 0; h < 6; h++) {
            spawnRing(center.clone().add(0, h * 0.5, 0), Particle.CLOUD, 1.2 - (h * 0.1), 10);
        }
    }

    private void playElementSound(Player player, Element element) {
        Sound sound = switch (element) {
            case FIRE -> Sound.ITEM_FIRECHARGE_USE;
            case WATER -> Sound.ITEM_BUCKET_EMPTY;
            case EARTH -> Sound.BLOCK_STONE_BREAK;
            case AIR -> Sound.ENTITY_PHANTOM_FLAP;
            default -> null;
        };
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private String elementUltimateName(Element element) {
        return switch (element) {
            case FIRE -> "<red>Płomienna Nowa rozgrywa się!";
            case WATER -> "<aqua>Moc oceanu eksploduje!";
            case EARTH -> "<green>Ziemia się trzęsie!";
            case AIR -> "<white>Wiatr szaleje!";
            default -> "";
        };
    }

    private void spawnWaterLayer(Location baseLoc, int yOffset, Map<Block, BlockData> originalBlocks) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = baseLoc.clone().add(x, yOffset, z).getBlock();
                if (originalBlocks.containsKey(b)) {
                    b.setType(Material.WATER, false); // false = woda stacjonarna, nie rozlewa się
                }
            }
        }
    }

    // ==========================================
    // Akcje wykonywane po wyborze żywiołu
    // ==========================================
    public void executeOnSelectActions(Player player, Element element) {
        List<String> actions = plugin.getConfig().getStringList("elements." + element.name() + ".on-select");

        for (String action : actions) {
            String formattedAction = action.replace("%player%", player.getName());

            if (formattedAction.startsWith("[CONSOLE] ")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedAction.substring(10));
            } else if (formattedAction.startsWith("[PLAYER] ")) {
                player.performCommand(formattedAction.substring(9));
            } else if (formattedAction.startsWith("[MESSAGE] ")) {
                player.sendMessage(AvatarSMP.MM.deserialize(formattedAction.substring(10)));
            } else if (formattedAction.startsWith("[TITLE] ")) {
                String[] parts = formattedAction.substring(8).split("<subtitle>");
                String title = parts[0];
                String subtitle = parts.length > 1 ? parts[1] : "";

                player.showTitle(Title.title(
                    AvatarSMP.MM.deserialize(title),
                    AvatarSMP.MM.deserialize(subtitle)
                ));
            }
        }
    }
}