package me.avatarsmp.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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
        PlayerData data = this.plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || data.getElement() == Element.NONE) {
            return;
        }
        Scoreboard board = this.boards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective objective = board.getObjective("avatar");
        if (objective == null) {
            objective = board.registerNewObjective("avatar", Criteria.DUMMY,
                    AvatarSMP.MM.deserialize("<b><color:#00e1ff>▲ AVATAR SMP ▲"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
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
        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add("<gray>Żywioł: " + elementLabel(data.getElement()));
        lines.add("  ");
        if (data.getElement() == Element.WARRIOR) {
            lines.add("<gray>Rola: <white>Chi Blocker");
            lines.add("<gray>Status: <green>● Passywne");
            lines.add("<gray>Aktywacja: <yellow>Atak w melee");
        } else {
            int heldSlot = player.getInventory().getHeldItemSlot();
            String[] names = AbilityRegistry.namesFor(data.getElement());
            boolean anyBound = false;
            for (int abilityIndex = 0; abilityIndex < names.length; abilityIndex++) {
                int boundSlot = data.getAbilitySlot(abilityIndex);
                if (boundSlot < 0) {
                    continue;
                }
                anyBound = true;
                boolean onCooldown = this.plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), abilityIndex);
                String color;
                String suffix = "";
                if (onCooldown) {
                    color = "<dark_gray>";
                    long remaining = this.plugin.getCooldownManager().getRemainingMillis(player.getUniqueId(), abilityIndex);
                    suffix = " <dark_gray>" + String.format("%.1fs", remaining / 1000.0);
                } else if (heldSlot == boundSlot) {
                    color = "<white>";
                } else {
                    color = "<gray>";
                }
                lines.add(color + "=> " + names[abilityIndex] + suffix);
            }
            if (!anyBound) {
                lines.add("<gray>Brak przypisanych umiejętności.");
                lines.add("<yellow>Użyj /avatar bind");
            }
        }
        lines.add("   ");
        return lines;
    }

    private String elementLabel(Element element) {
        return switch (element) {
            case FIRE -> "<red>Ogień";
            case WATER -> "<aqua>Woda";
            case EARTH -> "<green>Ziemia";
            case AIR -> "<white>Powietrze";
            case WARRIOR -> "<gray>Wojownik";
            default -> "<gray>Brak";
        };
    }

    public void remove(UUID uuid) {
        Scoreboard board = this.boards.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (board != null && player != null && player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void shutdown() {
        this.task.cancel();
        this.boards.clear();
    }
}