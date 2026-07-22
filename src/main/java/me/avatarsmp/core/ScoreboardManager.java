package me.avatarsmp.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.avatarsmp.core.data.PlayerData;

public class ScoreboardManager {

    private final AvatarSMP plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final ScoreboardTask task;

    public ScoreboardManager(AvatarSMP plugin) {
        this.plugin = plugin;
        this.task = new ScoreboardTask(plugin, this);
        this.task.start();
    }

    public void update(Player player) {
        // 1. ZMIANA: Obsługa wyłączenia scoreboardu w config.yml
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            remove(player.getUniqueId());
            return;
        }

        PlayerData data = this.plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || data.getElement() == Element.NONE) {
            remove(player.getUniqueId());
            return;
        }

        Scoreboard board = this.boards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());
        
        // 2. ZMIANA: Pobieranie tytułu dynamicznie z configu
        String rawTitle = plugin.getConfig().getString("scoreboard.title", "<b><color:#00e1ff>▲ AVATAR SMP ▲");
        
        Objective objective = board.getObjective("avatar");
        if (objective == null) {
            objective = board.registerNewObjective("avatar", Criteria.DUMMY,
                    AvatarSMP.MM.deserialize(rawTitle));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            // Aktualizuj tytuł w locie, jeśli zmienił się w configu
            objective.displayName(AvatarSMP.MM.deserialize(rawTitle));
        }

        List<String> lines = buildLines(player, data);
        int score = 15;
        for (int i = 0; i < 15; i++) {
            String entry = ChatColor.COLOR_CHAR + Integer.toString(i, 16) + ChatColor.COLOR_CHAR + "r";
            Team team = board.getTeam("avatar_" + i);
            if (i < lines.size()) {
                if (team == null) {
                    team = board.registerNewTeam("avatar_" + i);
                    team.addEntry(entry);
                }
                team.prefix(AvatarSMP.MM.deserialize(lines.get(i)));
                objective.getScore(entry).setScore(score);
            } else if (team != null) {
                board.resetScores(entry);
                team.unregister();
            }
            score--;
        }
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    private List<String> buildLines(Player player, PlayerData data) {
        List<String> result = new ArrayList<>();

        for (String line : plugin.getConfig().getStringList("scoreboard.lines")) {
            if (line.contains("%element%")) {
                result.add(line.replace("%element%", elementLabel(data.getElement())));
            } else if (line.contains("%skills%")) {
                appendSkillsSection(player, data, result);
            } else {
                result.add(line);
            }
        }
        return result;
    }

    // 3. ZMIANA: Etykiety żywiołów pobierane z configu (z wartościami domyślnymi)
    private String elementLabel(Element element) {
        String path = "elements." + element.name() + ".display-name";
        String fallback = switch (element) {
            case FIRE -> "<red>Ogień";
            case WATER -> "<aqua>Woda";
            case EARTH -> "<green>Ziemia";
            case AIR -> "<white>Powietrze";
            case WARRIOR -> "<gray>Wojownik";
            default -> "<gray>Brak";
        };
        return plugin.getConfig().getString(path, fallback);
    }

    public void remove(UUID uuid) {
        Scoreboard board = this.boards.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            // Bezpiecznie czyścimy scoreboard i przywracamy domyślny
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void shutdown() {
        this.task.cancel();
        for (UUID uuid : new ArrayList<>(this.boards.keySet())) {
            remove(uuid);
        }
        this.boards.clear();
    }

    private void appendSkillsSection(Player player, PlayerData data, List<String> result) {
        if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
            return;
        }

        int heldSlot = player.getInventory().getHeldItemSlot();
        for (int slot = 0; slot < 8; slot++) {
            int abilityIndex = data.getAbilityForSlot(slot);
            if (abilityIndex != -1) {
                String skillName = AbilityRegistry.nameFor(data.getElement(), abilityIndex);
                String prefix = (slot == heldSlot) ? "<green>▸ " : "<gray>  ";
                result.add(prefix + (slot + 1) + ". " + skillName);
            }
        }
    }
}