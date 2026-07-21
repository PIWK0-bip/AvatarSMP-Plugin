package me.avatarsmp.core.gui;

import com.cryptomorin.xseries.XEnchantment;
import me.avatarsmp.core.AbilityRegistry;
import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.DataManager;
import me.avatarsmp.core.Element;
import me.avatarsmp.core.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only compendium showing all 8 abilities of the player's element,
 * always mirroring AbilityRegistry so it stays in sync with BindGUI and
 * the Scoreboard.
 */
public class SkillsGUI implements InventoryHolder {

    private static final int[] SKILL_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16};

    private final Inventory inventory;

    public SkillsGUI(PlayerData data) {
        this.inventory = Bukkit.createInventory(this, 27,
                AvatarSMP.MM.deserialize("<gradient:#00c3ff:#8e2de2><bold>Kompendium Umiejętności</gradient>"));
        ItemStack filler = buildFiller();
        for (int i = 0; i < 27; i++) {
            this.inventory.setItem(i, filler);
        }

        Element element = data.getElement();
        Material iconMaterial = iconFor(element);
        String[] names = AbilityRegistry.namesFor(element);

        for (int i = 0; i < SKILL_SLOTS.length && i < names.length; i++) {
            boolean unlocked = data.getLevel() >= AbilityRegistry.requiredLevel(i);
            this.inventory.setItem(SKILL_SLOTS[i], buildSkillItem(iconMaterial, names[i], i, unlocked));
        }

        this.inventory.setItem(4, buildInfoItem(element, data.getLevel()));
    }

    private Material iconFor(Element element) {
        return switch (element) {
            case FIRE -> Material.BLAZE_POWDER;
            case WATER -> Material.WATER_BUCKET;
            case EARTH -> Material.STONE;
            case AIR -> Material.FEATHER;
            default -> Material.BARRIER;
        };
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<dark_gray> "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(Element element, int level) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<gradient:#00c3ff:#8e2de2><bold>Twoja ścieżka: " + element.name() + "</gradient>"));
        List<Component> lore = new ArrayList<>();
        lore.add(AvatarSMP.MM.deserialize("<gray>Poziom: <white>" + level));
        lore.add(AvatarSMP.MM.deserialize("<gray>Umiejętności odblokowują się"));
        lore.add(AvatarSMP.MM.deserialize("<gray>wraz ze zdobywaniem poziomów."));
        lore.add(AvatarSMP.MM.deserialize("<yellow>Użyj /avatar bind, aby je przypisać."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int cooldownSecondsFor(int abilityIndex) {
        return abilityIndex == 7 ? 60 : 5 + (abilityIndex * 2);
    }

    private ItemStack buildSkillItem(Material material, String name, int abilityIndex, boolean unlocked) {
        ItemStack item = new ItemStack(unlocked ? material : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize((unlocked ? "<yellow><bold>" : "<gray>") + name));
        List<Component> lore = new ArrayList<>();
        if (unlocked) {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <green>ODBLOKOWANO"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Użycie: <white>Prawy Klik"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Cooldown: <white>" + cooldownSecondsFor(abilityIndex) + "s"));
            if (abilityIndex == 7) {
                lore.add(AvatarSMP.MM.deserialize("<gold>✦ Umiejętność Ultimate"));
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            XEnchantment.matchXEnchantment("UNBREAKING").ifPresent(e -> meta.addEnchant(e.getEnchant(), 1, true));
        } else {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <red>ZABLOKOWANE"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Wymaganie: <white>Poziom " + AbilityRegistry.requiredLevel(abilityIndex)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public static void open(Player player, DataManager dataManager) {
        PlayerData data = dataManager.getData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(AvatarSMP.MM.deserialize("<red>Twoje dane wciąż się wczytują."));
            return;
        }
        SkillsGUI gui = new SkillsGUI(data);
        player.openInventory(gui.getInventory());
    }
}