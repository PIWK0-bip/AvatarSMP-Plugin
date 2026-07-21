package me.avatarsmp.core.update;

import me.avatarsmp.core.AvatarSMP;
import org.bukkit.Bukkit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {

    private final AvatarSMP plugin;

    // TODO: Zmień na swój nick i nazwę repozytorium na GitHubie
    private static final String GITHUB_REPO = "PIWK0-bip/AvatarSMP-Plugin";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private boolean updateAvailable = false;
    private String latestVersion = null;
    private String downloadUrl = null;

    public UpdateManager(AvatarSMP plugin) {
        this.plugin = plugin;
        // Sprawdzamy dostępność aktualizacji w tle przy starcie pluginu
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkForUpdates);
    }

    public void checkForUpdates() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "AvatarSMP-AutoUpdater");

            if (connection.getResponseCode() != 200) {
                plugin.getLogger().warning("[UpdateManager] Nie udało się sprawdzić aktualizacji. Kod odpowiedzi: " + connection.getResponseCode());
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();

            this.latestVersion = extractJsonValue(json, "tag_name");
            String currentVersion = plugin.getDescription().getVersion();

            if (latestVersion != null && !currentVersion.equalsIgnoreCase(latestVersion)) {
                this.downloadUrl = extractJarDownloadUrl(json);
                if (this.downloadUrl != null) {
                    this.updateAvailable = true;
                    plugin.getLogger().info("[UpdateManager] Znaleziono nową wersję: " + latestVersion + " (Obecna: " + currentVersion + ")");
                }
            } else {
                this.updateAvailable = false;
                plugin.getLogger().info("[UpdateManager] Plugin jest aktualny (v" + currentVersion + ").");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[UpdateManager] Błąd podczas sprawdzania aktualizacji: " + e.getMessage());
        }
    }

    // --- METODY WYMAGANE PRZEZ AVATARCOMMAND ---

    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    public String getLatestVersion() {
        return this.latestVersion;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public boolean downloadUpdate() {
        if (!updateAvailable || downloadUrl == null) {
            plugin.getLogger().warning("[UpdateManager] Brak dostępnej aktualizacji do pobrania.");
            return false;
        }

        try {
            plugin.getLogger().info("[UpdateManager] Pobieranie aktualizacji v" + latestVersion + "...");

            // Folder plugins/update/
            File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            // Używamy plugin.getName() + ".jar" zamiast protected getFile()
            File targetFile = new File(updateFolder, plugin.getName() + ".jar");

            HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
            connection.setRequestProperty("User-Agent", "AvatarSMP-AutoUpdater");

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }

            plugin.getLogger().info("[UpdateManager] Pobrano wersję " + latestVersion + "! Zostanie zaaplikowana po restarcie serwera.");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("[UpdateManager] Błąd podczas pobierania pliku aktualizacji: " + e.getMessage());
            return false;
        }
    }

    // Pomocnicze parsowanie prostego JSONa
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        return (end != -1) ? json.substring(start, end) : null;
    }

    private String extractJarDownloadUrl(String json) {
        String searchKey = "\"browser_download_url\":\"";
        int start = 0;
        while ((start = json.indexOf(searchKey, start)) != -1) {
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            if (end != -1) {
                String url = json.substring(start, end);
                if (url.endsWith(".jar")) {
                    return url;
                }
            }
        }
        return null;
    }
}