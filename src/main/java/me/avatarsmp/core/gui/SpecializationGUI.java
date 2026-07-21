package me.avatarsmp.core.gui;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.Element;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpecializationGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Element element;

    public SpecializationGUI(Element element) {
        this.element = element;
        this.inventory = Bukkit.createInventory(this, 9, AvatarSMP.MM.deserialize("<light_purple><bold>Wybierz specjalizację"));
        Material material = switch (element) {
            case FIRE -> Material.LIGHTNING_ROD;
            case WATER -> Material.REDSTONE;
            case EARTH -> Material.IRON_INGOT;
            case AIR -> Material.FEATHER;
            default -> Material.BARRIER;
        };
        String name = switch (element) {
            case FIRE -> "<yellow><bold>Błyskawica";
            case WATER -> "<dark_red><bold>Magia Krwi";
            case EARTH -> "<gray><bold>Magia Metalu";
            case AIR -> "<white><bold>Lot";
            default -> "<red>Niedostępne";
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize(name));
        item.setItemMeta(meta);
        this.inventory.setItem(4, item);
    }

    public Element getElement() {
        return this.element;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public static void open(Player player, Element element) {
        SpecializationGUI gui = new SpecializationGUI(element);
        player.openInventory(gui.getInventory());
    }
}