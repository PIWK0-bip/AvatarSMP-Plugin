package me.avatarsmp.core.commands;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.Element;
import me.avatarsmp.core.data.PlayerData;
import me.avatarsmp.core.gui.SkillsGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AvatarCommand implements TabExecutor {

    private final AvatarSMP plugin;

    public AvatarCommand(AvatarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
                if (data != null) {
                    plugin.getGuiManager().openMainGUI(player, data);
                }
            } else {
                sender.sendMessage(AvatarSMP.MM.deserialize("<red>This command can only be executed by a player. Use /avatar help."));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> {
                sendHelpMenu(sender);
                return true;
            }
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>Only players can open the menu!"));
                    return true;
                }
                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
                if (data != null) {
                    plugin.getGuiManager().openMainGUI(player, data);
                }
                return true;
            }
            case "skills" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>Only players can view skills!"));
                    return true;
                }
                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
                if (data != null) {
                    SkillsGUI.open(player, plugin.getDataManager(), plugin);
                }
                return true;
            }
            case "revoke" -> {
                if (!sender.hasPermission("avatarsmp.admin")) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>You do not have permission to use this command!"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>Usage: <white>/avatar revoke <player>"));
                    return true;
                }
                return executeRevoke(sender, args[1]);
            }
            case "bind" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>Only players can bind skills!"));
                    return true;
                }
                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
                if (data != null) {
                    plugin.getGuiManager().openBindGUI(player, data);
                }
                return true;
            }
            case "update" -> {
                return handleUpdateCommand(sender);
            }
            case "admin" -> {
                if (!sender.hasPermission("avatarsmp.admin")) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>You do not have permission for admin commands!"));
                    return true;
                }
                return handleAdminCommand(sender, args);
            }
            default -> {
                sender.sendMessage(AvatarSMP.MM.deserialize("<red>Unknown subcommand. Use <yellow>/avatar help</yellow>."));
                return true;
            }
        }
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>--- Avatar Admin Menu ---</bold>"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar admin xp reset <player> <gray>- Resets player's XP"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar admin xp add <player> <amount> <gray>- Adds XP to player"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar admin revoke <player> <gray>- Revokes power from player"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar admin setelement <player> <element> <gray>- Sets player's element"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar admin reload <gray>- Reloads configuration"));
            return true;
        }

        String adminSub = args[1].toLowerCase();

        switch (adminSub) {
            case "revoke" -> {
                if (args.length < 3) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>Usage: <white>/avatar admin revoke <player>"));
                    return true;
                }
                return executeRevoke(sender, args[2]);
            }
            case "xp" -> {
                if (args.length < 4) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>Usage: /avatar admin xp <reset|add|set> <player> [amount]"));
                    return true;
                }
                String xpAction = args[2].toLowerCase();
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage(AvatarSMP.MM.deserialize("<red>Player not found!"));
                    return true;
                }
                PlayerData data = plugin.getDataManager().getData(target.getUniqueId());
                if (xpAction.equals("reset")) {
                    data.setXp(0);
                    data.setLevel(1);
                    sender.sendMessage(AvatarSMP.MM.deserialize("<green>Successfully reset XP and level for player <yellow>" + target.getName()));
                    target.sendMessage(AvatarSMP.MM.deserialize("<red>Your level and experience have been reset by an administrator."));
                    return true;
                }
                if (xpAction.equals("add") || xpAction.equals("set")) {
                    if (args.length < 5) {
                        sender.sendMessage(AvatarSMP.MM.deserialize("<red>Please specify an XP amount!"));
                        return true;
                    }
                    try {
                        int amount = Integer.parseInt(args[4]);
                        if (xpAction.equals("add")) {
                            plugin.getBendingManager().grantXp(target, amount);
                            sender.sendMessage(AvatarSMP.MM.deserialize("<green>Added <yellow>" + amount + " XP</yellow> to player " + target.getName()));
                        } else {
                            data.setXp(amount);
                            sender.sendMessage(AvatarSMP.MM.deserialize("<green>Set XP of player <yellow>" + target.getName() + "</yellow> to " + amount));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(AvatarSMP.MM.deserialize("<red>XP amount must be a number!"));
                    }
                    return true;
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(AvatarSMP.MM.deserialize("<green>AvatarSMP configuration has been reloaded!"));
                return true;
            }
        }

        sender.sendMessage(AvatarSMP.MM.deserialize("<red>Unknown admin command. Use <yellow>/avatar admin</yellow>."));
        return true;
    }

    private boolean executeRevoke(CommandSender sender, String targetName) {
        Player targetPlayer = Bukkit.getPlayer(targetName);
        PlayerData targetData = null;
    
        if (targetPlayer != null) {
            targetData = plugin.getDataManager().getData(targetPlayer.getUniqueId());
        }
    
        if (targetData == null) {
            sender.sendMessage(AvatarSMP.MM.deserialize("<red>Player or player data not found!"));
            return true;
        }

        // 1. Reset player's element, XP, and level
        targetData.setElement(Element.NONE);
        targetData.setXp(0);
        targetData.setLevel(1);

        // Save changes asynchronously
        plugin.getDataManager().saveAsync(targetData);

        sender.sendMessage(AvatarSMP.MM.deserialize("<green>Successfully revoked powers and reset XP for player <white>" + targetName + "<green>!"));

        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(AvatarSMP.MM.deserialize("<red>Your power and level have been reset by an administrator!"));

            // 2. Reset and remove player's scoreboard
            targetPlayer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    
        return true;
    }

    private boolean handleUpdateCommand(CommandSender sender) {
        if (!sender.hasPermission("avatarsmp.admin")) {
            sender.sendMessage(AvatarSMP.MM.deserialize("<red>You do not have permission to execute this command!"));
            return true;
        }
        if (!plugin.getUpdateManager().isUpdateAvailable()) {
            sender.sendMessage(AvatarSMP.MM.deserialize("<green>The plugin is already on the latest version!"));
            return true;
        }
        sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>Downloading update v" + plugin.getUpdateManager().getLatestVersion() + "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getUpdateManager().downloadUpdate();
            if (success) {
                sender.sendMessage(AvatarSMP.MM.deserialize("<green><bold>Downloaded successfully!</bold> Restart the server to apply changes."));
            } else {
                sender.sendMessage(AvatarSMP.MM.deserialize("<red>An error occurred while downloading the update file. Check the console."));
            }
        });
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>AvatarSMP</bold> <gray>v" + plugin.getDescription().getVersion()));
        sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar menu <gray>- Opens selection/progress menu"));
        sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar skills <gray>- View available skills"));
        sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar bind <gray>- Open skill binding menu"));
        sender.sendMessage(AvatarSMP.MM.deserialize("<yellow>/avatar help <gray>- Displays this help menu"));
        if (sender.hasPermission("avatarsmp.admin")) {
            sender.sendMessage(AvatarSMP.MM.deserialize("<red>/avatar admin revoke <player> <gray>- Revokes power from a player"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<red>/avatar admin <gray>- Administration commands"));
            sender.sendMessage(AvatarSMP.MM.deserialize("<red>/avatar update <gray>- Downloads the latest plugin version"));
        }
    }

    // --- TAB Completion ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Argument 1: /avatar <tab>
        if (args.length == 1) {
            completions.addAll(List.of("help", "menu", "skills", "bind"));
            if (sender.hasPermission("avatarsmp.admin")) {
                completions.add("admin");
                completions.add("update");
                completions.add("revoke");
            }
            return filter(completions, args[0]);
        }

        // Argument 2: /avatar admin <tab> OR /avatar revoke <tab>
        if (args.length == 2 && sender.hasPermission("avatarsmp.admin")) {
            if (args[0].equalsIgnoreCase("admin")) {
                completions.addAll(List.of("xp", "revoke", "setelement", "reload"));
                return filter(completions, args[1]);
            }
            if (args[0].equalsIgnoreCase("revoke")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
            }
        }

        // Argument 3: /avatar admin revoke <tab> OR /avatar admin xp <tab>
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("avatarsmp.admin")) {
            if (args[1].equalsIgnoreCase("revoke")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
            }
            if (args[1].equalsIgnoreCase("xp")) {
                completions.addAll(List.of("reset", "add", "set"));
                return filter(completions, args[2]);
            }
        }

        // Argument 4: /avatar admin xp add/set/reset <tab>
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("xp") && sender.hasPermission("avatarsmp.admin")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}