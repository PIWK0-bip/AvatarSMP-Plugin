package me.avatarsmp.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final DataManager dataManager;

    public PlayerJoinListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Ładowanie danych z pliku YML do pamięci RAM przy wejściu gracza
        dataManager.loadAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Zapis i usunięcie z pamięci RAM przy wyjściu
        var data = dataManager.getData(event.getPlayer().getUniqueId());
        if (data != null) {
            dataManager.saveAsync(data);
            dataManager.remove(event.getPlayer().getUniqueId());
        }
    }
}