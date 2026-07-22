package me.avatarsmp.core;

import me.avatarsmp.core.data.PlayerData;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ISkill {
    boolean execute(Player player, PlayerData data);
}