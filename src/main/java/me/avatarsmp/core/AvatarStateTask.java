package me.avatarsmp.core;

import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import me.avatarsmp.core.data.PlayerData;

public class AvatarStateTask extends BukkitRunnable {

    private final AvatarSMP plugin;
    private final Player player;
    private final int durationTicks;
    private final boolean lowHealthMode;
    private int elapsedTicks = 0;
    private double angle = 0;

    public AvatarStateTask(AvatarSMP plugin, Player player) {
        this(plugin, player, 2400, false);
    }

    public AvatarStateTask(AvatarSMP plugin, Player player, int durationTicks, boolean lowHealthMode) {
        this.plugin = plugin;
        this.player = player;
        this.durationTicks = durationTicks;
        this.lowHealthMode = lowHealthMode;
    }

    public void start() {
        this.plugin.getBendingManager().setAvatarState(this.player.getUniqueId(), true);
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, this.durationTicks, 0));
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, this.durationTicks, 0));
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.durationTicks, 2));
        if (this.lowHealthMode) {
            this.player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, this.durationTicks, 4));
            this.player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, this.durationTicks, 0));
            this.player.showTitle(Title.title(
                    AvatarSMP.MM.deserialize("<gold><bold>STAN AVATARA"),
                    AvatarSMP.MM.deserialize("<white>Instynkt przetrwania cię obudził!")));
        } else {
            this.player.showTitle(Title.title(
                    AvatarSMP.MM.deserialize("<gold><bold>STAN AVATARA"),
                    AvatarSMP.MM.deserialize("<white>Moc czterech żywiołów płynie przez Ciebie.")));
        }
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        this.runTaskTimer(this.plugin, 0L, 2L);
    }

    @Override
    public void run() {
        if (!this.player.isOnline() || this.elapsedTicks >= this.durationTicks) {
            stopState();
            return;
        }
        Location center = this.player.getLocation().add(0, 1, 0);
        double radius = 1.5;
        for (int i = 0; i < 4; i++) {
            double theta = this.angle + (Math.PI / 2) * i;
            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;
            Location point = center.clone().add(x, Math.sin(this.angle) * 0.5, z);
            switch (i) {
                case 0 -> point.getWorld().spawnParticle(Particle.FLAME, point, 2, 0, 0, 0, 0);
                case 1 -> point.getWorld().spawnParticle(Particle.SPLASH, point, 2, 0, 0, 0, 0);
                case 2 -> point.getWorld().spawnParticle(Particle.CLOUD, point, 2, 0, 0, 0, 0);
                case 3 -> point.getWorld().spawnParticle(Particle.FALLING_DUST, point, 2, 0, 0, 0, 0,
                        org.bukkit.Material.DIRT.createBlockData());
                default -> {
                }
            }
        }
        this.angle += 0.35;
        this.elapsedTicks += 2;
    }

    private void stopState() {
        this.plugin.getBendingManager().setAvatarState(this.player.getUniqueId(), false);
        if (this.player.isOnline()) {
            this.player.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>Stan Avatara wygasł."));
            if (this.lowHealthMode) {
                PlayerData data = this.plugin.getDataManager().getData(this.player.getUniqueId());
                if (data != null) {
                    data.setAvatarStateCooldownUntil(System.currentTimeMillis() + 600000L);
                }
            }
        }
        this.cancel();
    }
}