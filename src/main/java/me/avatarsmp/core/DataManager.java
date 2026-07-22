package me.avatarsmp.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import me.avatarsmp.core.data.PlayerData;

public class DataManager {

    private final AvatarSMP plugin;
    private final File userDataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(AvatarSMP plugin) {
        this.plugin = plugin;
        this.userDataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!this.userDataFolder.exists()) {
            this.userDataFolder.mkdirs();
        }
    }

    public PlayerData getData(UUID uuid) {
        return this.cache.get(uuid);
    }

    public CompletableFuture<PlayerData> loadAsync(UUID uuid) {
        CompletableFuture<PlayerData> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = readFromDisk(uuid);
            this.cache.put(uuid, data);
            future.complete(data);
        });
        return future;
    }

    private PlayerData readFromDisk(UUID uuid) {
        File file = new File(this.userDataFolder, uuid.toString() + ".yml");
        PlayerData data = new PlayerData(uuid);
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
            String elementName = config.getString("element", "NONE");
            try {
                data.setElement(Element.valueOf(elementName));
            } catch (IllegalArgumentException ex) {
                data.setElement(Element.NONE);
            }

            int rawLevel = config.getInt("level", 1);
            int rawXp = config.getInt("xp", 0);

            data.setLevel(Math.min(BendingManager.MAX_LEVEL, Math.max(1, rawLevel)));
            data.setXp(Math.max(0, rawXp));
            data.setChiBlocked(false);

            List<Integer> savedBindings = config.getIntegerList("abilitySlots");
            if (savedBindings.size() == 8) {
                int[] bindings = new int[8];
                for (int i = 0; i < 8; i++) {
                    bindings[i] = savedBindings.get(i);
                }
                data.setAbilitySlots(bindings);
            }
        }
        return data;
    }

    public void saveAsync(PlayerData data) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeToDisk(data));
    }

    public void saveSync(PlayerData data) {
        writeToDisk(data);
    }

    private void writeToDisk(PlayerData data) {
        File file = new File(this.userDataFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
    
        config.set("uuid", data.getUuid().toString());
        config.set("element", data.getElement().name());
        config.set("level", data.getLevel());
        config.set("xp", data.getXp());

        List<Integer> bindings = new ArrayList<>();
        for (int slot : data.getAbilitySlots()) {
            bindings.add(slot);
        }
        config.set("abilitySlots", bindings);

        try {
            config.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("Nie udało się zapisać danych gracza " + data.getUuid() + ": " + ex.getMessage());
        }
    }

    public void saveAllSync() {
        for (PlayerData data : this.cache.values()) {
            writeToDisk(data);
        }
    }

    public void remove(UUID uuid) {
        this.cache.remove(uuid);
    }
}