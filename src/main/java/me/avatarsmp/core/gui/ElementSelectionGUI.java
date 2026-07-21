package me.avatarsmp.core.gui;

import me.avatarsmp.core.AvatarSMP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

// TEGO BRAKOWAŁO:
public class ElementSelectionGUI implements InventoryHolder {

    // TEGO TEŻ BRAKOWAŁO:
    private final Inventory inventory;

    public ElementSelectionGUI() {
        this.inventory = Bukkit.createInventory(this, 45, AvatarSMP.MM.deserialize("<gold><bold>Wybierz swoją ścieżkę"));

        // 1. Najpierw wypełniamy całe GUI (sloty od 0 do 44) szarym szkłem jako tłem
        for (int i = 0; i < 45; i++) {
            this.inventory.setItem(i, buildItem(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));
        }

        // 2. Następnie układamy główne przedmioty w wybranych slotach (nadpisując szkło)
        this.inventory.setItem(10, buildItem(Material.PACKED_ICE, "<aqua><bold>Woda", "<gray>Bender wody"));
        this.inventory.setItem(12, buildItem(Material.BLAZE_POWDER, "<red><bold>Ogień", "<gray>Bender ognia"));
        this.inventory.setItem(14, buildItem(Material.GRASS_BLOCK, "<green><bold>Ziemia", "<gray>Bender ziemi"));
        this.inventory.setItem(16, buildItem(Material.WIND_CHARGE, "<white><bold>Powietrze", "<gray>Bender powietrza"));
        this.inventory.setItem(31, buildItem(Material.IRON_SWORD, "<gray><bold>Wojownik", "<gray>Chi Blocker"));
    }

    private ItemStack buildItem(Material material, String name, String lore) {
        return buildItem(material, name, List.of(lore));
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize(name));
        List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(AvatarSMP.MM.deserialize(line));
        }
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public static void open(Player player) {
        ElementSelectionGUI gui = new ElementSelectionGUI();
        player.openInventory(gui.getInventory());
    }
}