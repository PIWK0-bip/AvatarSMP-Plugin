package me.avatarsmp.core.gui;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XItemFlag;
import com.cryptomorin.xseries.XMaterial;
import me.avatarsmp.core.AbilityRegistry;
import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.BendingManager;
import me.avatarsmp.core.DataManager;
import me.avatarsmp.core.Element;
import me.avatarsmp.core.data.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SkillsGUI implements InventoryHolder {

    private static final int[] SKILL_SLOTS = {19, 20, 21, 22, 23, 24, 25, 31};
    private final PlayerData data;
    private final Element element;
    private final Inventory inventory;

    public SkillsGUI(PlayerData data, DataManager dataManager, AvatarSMP plugin) {
        this.data = data;
        this.element = data.getElement();
        this.inventory = Bukkit.createInventory(this, 45,
                AvatarSMP.MM.deserialize("<gradient:#00c3ff:#8e2de2><bold>Umiejętności Żywiołu</gradient>"));
        render(plugin);
    }

    public PlayerData getData() {
        return this.data;
    }

    public int abilityIndexForSlot(int rawSlot) {
        for (int i = 0; i < SKILL_SLOTS.length; i++) {
            if (SKILL_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    private void render(AvatarSMP plugin) {
        fillThemeFrame();

        // Nagłówek - Profil Gracza
        this.inventory.setItem(4, buildProfileItem(plugin));

        // Lista Umiejętności
        String[] names = AbilityRegistry.namesFor(this.element);
        for (int i = 0; i < names.length && i < SKILL_SLOTS.length; i++) {
            boolean unlocked = this.data.getLevel() >= AbilityRegistry.requiredLevel(i);
            int boundSlot = this.data.getAbilitySlot(i);
            this.inventory.setItem(SKILL_SLOTS[i], buildSkillItem(plugin, names[i], i, unlocked, boundSlot));
        }

        // Przycisk Przejścia do Bindowania
        this.inventory.setItem(40, buildBindNavigationItem());
    }

    private ItemStack buildProfileItem(AvatarSMP plugin) {
        ItemStack item = item(profileIcon(this.element));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<gold><bold>PROFIL ŻYWIOŁU: " + elementLabel(this.element)));

        List<Component> lore = new ArrayList<>();
        lore.add(AvatarSMP.MM.deserialize("<gray>Poziom: <yellow><bold>" + this.data.getLevel() + "</bold> / " + BendingManager.MAX_LEVEL));

        BendingManager bm = plugin.getBendingManager();
        int reqXp = (bm != null) ? bm.cumulativeXpForLevel(this.data.getLevel() + 1) : 0;
        double progress = reqXp > 0 ? Math.min(1.0, (double) this.data.getXp() / reqXp) : 1.0;
        
        lore.add(AvatarSMP.MM.deserialize("<gray>XP: <green>" + this.data.getXp() + "</green> / <gray>" + reqXp));
        lore.add(AvatarSMP.MM.deserialize("<gray>Postęp: " + buildProgressBar(progress)));
        lore.add(Component.empty());
        lore.add(AvatarSMP.MM.deserialize("<dark_gray>Kliknij umiejętność poniżej, aby ją przypisać."));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSkillItem(AvatarSMP plugin, String name, int abilityIndex, boolean unlocked, int boundSlot) {
        ItemStack item = unlocked ? item(iconForIndex(this.element, abilityIndex)) : item("GRAY_DYE");
        ItemMeta meta = item.getItemMeta();

        String color = unlocked ? "<white><bold>" : "<dark_gray><bold>";
        meta.displayName(AvatarSMP.MM.deserialize(color + (abilityIndex + 1) + ". " + name));

        List<Component> lore = new ArrayList<>();
        int reqLevel = AbilityRegistry.requiredLevel(abilityIndex);

        if (unlocked) {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <green><bold>✔ ODBLOKOWANA</bold>"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Przypisany Slot: " + (boundSlot >= 0 ? "<aqua><bold>Slot " + (boundSlot + 1) + "</bold>" : "<yellow>Brak")));

            int cd = plugin.getConfig().getInt("elements." + this.element.name() + ".cooldowns." + abilityIndex, 5);
            double energy = plugin.getConfig().getDouble("elements." + this.element.name() + ".energy." + abilityIndex, (abilityIndex == 7) ? 40.0 : 10.0);

            lore.add(AvatarSMP.MM.deserialize("<gray>Czas Odnowienia: <yellow>" + cd + "s"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Koszt Energii: <light_purple>" + (int) energy + " Chi"));
            lore.add(Component.empty());
            lore.add(AvatarSMP.MM.deserialize("<yellow>▸ Kliknij, aby przypisać do hotbara"));

            XItemFlag.of("HIDE_ENCHANTS").ifPresent(flag -> flag.set(meta));
            if (boundSlot >= 0) {
                XEnchantment.matchXEnchantment("UNBREAKING").ifPresent(e -> meta.addEnchant(e.getEnchant(), 1, true));
            }
        } else {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <red><bold>🔒 ZABLOKOWANA</bold>"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Wymagany Poziom: <gold><bold>Lvl " + reqLevel + "</bold>"));
            lore.add(Component.empty());
            lore.add(AvatarSMP.MM.deserialize("<red>Zdobywaj XP, aby odblokować tę moc!"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBindNavigationItem() {
        ItemStack item = item("NETHER_STAR");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<gradient:#00c3ff:#8e2de2><bold>⚡ ZARZĄDZAJ BINDAMI</bold></gradient>"));
        List<Component> lore = List.of(
                AvatarSMP.MM.deserialize("<gray>Otwórz dedykowane menu bindowania,"),
                AvatarSMP.MM.deserialize("<gray>aby dostosować układy slotów."),
                Component.empty(),
                AvatarSMP.MM.deserialize("<yellow>▸ Kliknij, aby przejść do /avatar bind")
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillThemeFrame() {
        String mainGlass = primaryGlass(this.element);
        String borderGlass = secondaryGlass(this.element);

        ItemStack primaryPane = namedPane(mainGlass);
        ItemStack secondaryPane = namedPane(borderGlass);

        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                this.inventory.setItem(i, secondaryPane);
            } else {
                this.inventory.setItem(i, primaryPane);
            }
        }
    }

    private String buildProgressBar(double progress) {
        int totalBars = 10;
        int filled = (int) Math.round(progress * totalBars);
        StringBuilder sb = new StringBuilder("<green>");
        for (int i = 0; i < totalBars; i++) {
            if (i == filled) sb.append("<dark_gray>");
            sb.append("█");
        }
        sb.append("</dark_gray> <white>").append((int) (progress * 100)).append("%");
        return sb.toString();
    }

    private String elementLabel(Element el) {
        return switch (el) {
            case FIRE -> "<red>Ogień";
            case WATER -> "<aqua>Woda";
            case EARTH -> "<green>Ziemia";
            case AIR -> "<white>Powietrze";
            default -> "<gray>Brak";
        };
    }

    private String primaryGlass(Element el) {
        return switch (el) {
            case FIRE -> "ORANGE_STAINED_GLASS_PANE";
            case WATER -> "CYAN_STAINED_GLASS_PANE";
            case EARTH -> "LIME_STAINED_GLASS_PANE";
            case AIR -> "LIGHT_GRAY_STAINED_GLASS_PANE";
            default -> "GRAY_STAINED_GLASS_PANE";
        };
    }

    private String secondaryGlass(Element el) {
        return switch (el) {
            case FIRE -> "RED_STAINED_GLASS_PANE";
            case WATER -> "LIGHT_BLUE_STAINED_GLASS_PANE";
            case EARTH -> "GREEN_STAINED_GLASS_PANE";
            case AIR -> "WHITE_STAINED_GLASS_PANE";
            default -> "BLACK_STAINED_GLASS_PANE";
        };
    }

    private String profileIcon(Element el) {
        return switch (el) {
            case FIRE -> "FIRE_CHARGE";
            case WATER -> "HEART_OF_THE_SEA";
            case EARTH -> "EMERALD";
            case AIR -> "FEATHER";
            default -> "BOOK";
        };
    }

    private String iconForIndex(Element el, int index) {
        return switch (el) {
            case FIRE -> (index == 7) ? "NETHER_STAR" : "BLAZE_POWDER";
            case WATER -> (index == 7) ? "TRIDENT" : "WATER_BUCKET";
            case EARTH -> (index == 7) ? "ANVIL" : "STONE";
            case AIR -> (index == 7) ? "WIND_CHARGE" : "FEATHER";
            default -> "PAPER";
        };
    }

    private ItemStack namedPane(String material) {
        ItemStack item = item(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(String material) {
        return XMaterial.matchXMaterial(material)
                .map(XMaterial::parseItem)
                .orElseThrow(() -> new IllegalStateException("Brak materiału GUI: " + material));
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public static void open(Player player, DataManager dataManager, AvatarSMP plugin) {
        PlayerData data = dataManager.getData(player.getUniqueId());
        if (data != null) {
            player.openInventory(new SkillsGUI(data, dataManager, plugin).getInventory());
        }
    }
}