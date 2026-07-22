package me.avatarsmp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        this.currentLang = plugin.getConfig().getString("language", "en").toLowerCase();
        File langDir = new File(plugin.getDataFolder(), "lang");
        langDir.mkdirs();

        // 1. Load selected language file (from data folder or jar defaults)
        String fileName = "messages_" + currentLang + ".yml";
        String resourcePath = "lang/" + fileName;

        File langFile = new File(langDir, fileName);
        if (!langFile.exists()) {
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                plugin.getLogger().warning("[LanguageManager] Missing language file " + resourcePath + ". Falling back to English.");
                if (plugin.getResource("lang/messages_en.yml") != null) {
                    plugin.saveResource("lang/messages_en.yml", false);
                }
                langFile = new File(langDir, "messages_en.yml");
                this.currentLang = "en";
            }
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(langFile);

        // Set built-in defaults from jar
        InputStream defaultStream = plugin.getResource(resourcePath);
        if (defaultStream == null) {
            defaultStream = plugin.getResource("lang/messages_en.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defaults);
        }

        // 2. Load custom overrides from data folder root (messages_custom.yml)
        File customFile = new File(plugin.getDataFolder(), "messages_custom.yml");
        if (!customFile.exists()) {
            // Copy default template from jar to data folder
            try (InputStream in = plugin.getResource("lang/messages_custom.yml")) {
                if (in != null) {
                    Files.copy(in, customFile.toPath());
                    plugin.getLogger().info("[LanguageManager] Created messages_custom.yml in plugin folder. Edit it to customize messages.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[LanguageManager] Could not create messages_custom.yml: " + e.getMessage());
            }
        }

        if (customFile.exists()) {
            YamlConfiguration customConfig = YamlConfiguration.loadConfiguration(customFile);
            for (String key : customConfig.getKeys(true)) {
                if (customConfig.isString(key)) {
                    this.messagesConfig.set(key, customConfig.getString(key));
                }
            }
        }
    }

    public String getRaw(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) {
            return "<red>Missing message: " + path + "</red>";
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

    public String getCurrentLang() {
        return currentLang;
    }

    public boolean isLanguageAvailable(String lang) {
        return plugin.getResource("lang/messages_" + lang.toLowerCase() + ".yml") != null;
    }
}