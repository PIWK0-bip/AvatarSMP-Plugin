package me.avatarsmp.core.commands;

import me.avatarsmp.core.AvatarSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class AvatarCommand implements TabExecutor {

    private final AvatarSMP plugin;

    public AvatarCommand(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Brak argumentów lub /avatar help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>AvatarSMP</bold> <gray>v" + plugin.getDescription().getVersion()));
            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar help <gray>- Wyświetla menu pomocy"));
            
            if (sender.hasPermission("avatarsmp.admin")) {
                sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar update <gray>- Pobiera najnowszą wersję pluginu"));
            }
            return true;
        }

        // Subkomenda /avatar update
        if (args[0].equalsIgnoreCase("update")) {
            if (!sender.hasPermission("avatarsmp.admin")) {
                sender.sendMessage(AvatarSMP.MM.deserialize("<red>Nie masz uprawnień do wykonania tej komendy!"));
                return true;
            }

            if (!plugin.getUpdateManager().isUpdateAvailable()) {
                sender.sendMessage(AvatarSMP.MM.deserialize("<green>Plugin jest w najnowszej wersji!"));
                return true;
            }

            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>Pobieranie aktualizacji v" + plugin.getUpdateManager().getLatestVersion() + "..."));

            // Pobieranie w osobnym wątku (async), aby uniknąć lagów serwera
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean success = plugin.getUpdateManager().downloadUpdate();
                if (success) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<green><bold>Pomyślnie pobrano!</bold> Zrestartuj serwer, aby wdrożyć zmiany."));
                } else {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>Wystąpił błąd podczas pobierania pliku. Sprawdź konsole."));
                }
            });
            return true;
        }

        sender.sendMessage(AvatarSMP.MM.deserialize("<red>Nieznana subkomenda. Użyj <yellow>/avatar help</yellow>."));
        return true;
    }

    // Auto-completetion (podpowiadanie komend po kliknięciu TAB)
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("help");
            if (sender.hasPermission("avatarsmp.admin")) {
                completions.add("update");
            }
        }
        
        return completions;
    }
}