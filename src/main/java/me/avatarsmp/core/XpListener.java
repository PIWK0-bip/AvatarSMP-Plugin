package me.avatarsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class XpListener implements Listener {

    private final AvatarSMP plugin;
    private final DataManager dataManager;

    public XpListener(AvatarSMP plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        PlayerData data = this.dataManager.getData(killer.getUniqueId());
        if (data == null || data.getElement() == Element.NONE) {
            return;
        }
        int amount = event.getEntity() instanceof Player ? 50 : 15;
        this.plugin.getBendingManager().grantXp(killer, amount);
    }
}