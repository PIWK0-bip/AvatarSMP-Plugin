package me.avatarsmp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LanguageManager {

    private final AvatarSMP plugin;
    private FileConfiguration messagesConfig;
    private String currentLang;

    public LanguageManager(AvatarSMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.currentLang = plugin.getConfig().getString("language", "pl").toLowerCase();
        String fileName = "messages_" + currentLang + ".yml";
        String resourcePath = "lang/" + fileName;

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        File file = new File(langDir, fileName);

        if (!file.exists()) {
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                plugin.getLogger().warning("[LanguageManager] Brak pliku " + resourcePath + ". Wczytuję domyślny lang/messages_pl.yml");
                if (plugin.getResource("lang/messages_pl.yml") != null) {
                    plugin.saveResource("lang/messages_pl.yml", false);
                }
                file = new File(langDir, "messages_pl.yml");
            }
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource(resourcePath);
        if (defaultStream == null && !resourcePath.equals("lang/messages_pl.yml")) {
            defaultStream = plugin.getResource("lang/messages_pl.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defaultConfig);
        }
    }

    public String getRaw(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) {
            return "<red>Brak wiadomości: " + path + "</red>";
        }
        return msg;
    }

    /**
     * Zwraca Component BEZ prefixu (idealne do GUI, ActionBar, Title)
     */
    public Component get(String path) {
        return get(path, Map.of());
    }

    public Component get(String path, Map<String, String> placeholders) {
        String raw = getRaw(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return MiniMessage.miniMessage().deserialize(raw);
    }

    /**
     * Zwraca Component Z PREFIXEM na początku (idealne do zwykłych wiadomości na czacie)
     */
    public Component getPrefixed(String path) {
        return getPrefixed(path, Map.of());
    }

    public Component getPrefixed(String path, Map<String, String> placeholders) {
        String prefix = getRaw("prefix");
        String raw = prefix + getRaw(path);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return MiniMessage.miniMessage().deserialize(raw);
    }
}