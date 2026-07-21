package me.avatarsmp.core;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface ISkill {

    boolean execute(Player player, PlayerData data);
}