package me.avatarsmp.core.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.avatarsmp.core.AvatarSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class UpdateManager implements Listener {

    private final AvatarSMP plugin;
    private final String repo; // Format: "Wlasciciel/NazwaRepozytorium"
    private final boolean autoDownload;

    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable = false;

    public UpdateManager(AvatarSMP plugin) {
        this.plugin = plugin;
        this.repo = plugin.getConfig().getString("updates.github-repo", "TwojNick/AvatarSMP");
        this.autoDownload = plugin.getConfig().getBoolean("updates.auto-download", false);

        if (plugin.getConfig().getBoolean("updates.check-on-startup", true)) {
            checkAsync();
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "AvatarSMP-UpdateChecker")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("Nie udało się sprawdzić aktualizacji (HTTP " + response.statusCode() + ").");
                    return;
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                this.latestVersion = json.get("tag_name").getAsString().replace("v", "");

                String currentVersion = plugin.getDescription().getVersion();

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    this.updateAvailable = true;

                    // Pobieranie URL do pliku .jar z assets
                    JsonArray assets = json.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        if (asset.get("name").getAsString().endsWith(".jar")) {
                            this.downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }

                    plugin.getLogger().info("Dostępna jest nowa wersja pluginu: v" + latestVersion + " (Obecna: v" + currentVersion + ")");

                    if (autoDownload && downloadUrl != null) {
                        downloadUpdate();
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Błąd podczas sprawdzania aktualizacji: " + e.getMessage());
            }
        });
    }

    public boolean downloadUpdate() {
        if (downloadUrl == null) {
            return false;
        }

        try {
            // Folder plugins/update jest automatycznie przetwarzany przez silnik przy restarcie
            File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            File destination = new File(updateFolder, plugin.getName() + ".jar");

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "AvatarSMP-UpdateChecker")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                try (InputStream in = response.body()) {
                    Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                plugin.getLogger().info("Pomyślnie pobrano nową wersję (" + latestVersion + "). Zostanie załadowana po restarcie serwera!");
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas pobierania aktualizacji: " + e.getMessage());
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Sprawdzamy czy gracz jest operatorem / posiada uprawnienie admina
        if (player.hasPermission("avatarsmp.admin") && updateAvailable) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(AvatarSMP.MM.deserialize(""));
                player.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>[AvatarSMP] <yellow>Dostępna jest nowa wersja pluginu! <white>v" + latestVersion));
                if (autoDownload) {
                    player.sendMessage(AvatarSMP.MM.deserialize("<green>Nowa wersja została pobrana i załaduje się po restarcie serwera."));
                } else {
                    player.sendMessage(AvatarSMP.MM.deserialize("<gray>Wpisz <yellow>/avatar update<gray>, aby pobrać ją automatycznie."));
                }
                player.sendMessage(AvatarSMP.MM.deserialize(""));
            }, 40L); // Opóźnienie 2s po wejściu na serwer
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}