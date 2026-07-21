package me.avatarsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

public class ChiBlockListener implements Listener {

    private final AvatarSMP plugin;
    private final DataManager dataManager;

    public ChiBlockListener(AvatarSMP plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        PlayerData damagerData = this.dataManager.getData(damager.getUniqueId());
        if (damagerData == null || damagerData.getElement() != Element.WARRIOR) {
            return;
        }
        PlayerData victimData = this.dataManager.getData(victim.getUniqueId());
        if (victimData == null) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            victimData.setChiBlocked(true);
            victim.sendActionBar(AvatarSMP.MM.deserialize("<red><bold>Twoje Chi zostało zablokowane!"));
            damager.sendActionBar(AvatarSMP.MM.deserialize("<green><bold>Zablokowałeś Chi przeciwnika!"));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                victimData.setChiBlocked(false);
                if (victim.isOnline()) {
                    victim.sendActionBar(AvatarSMP.MM.deserialize("<gray>Twoje Chi zostało odblokowane."));
                }
            }, 100L);
        }
    }
}