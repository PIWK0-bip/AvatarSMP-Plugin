package me.avatarsmp.core.gui;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XItemFlag;
import com.cryptomorin.xseries.XMaterial;
import me.avatarsmp.core.AbilityRegistry;
import me.avatarsmp.core.AvatarSMP;
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

public class BindGUI implements InventoryHolder {

    private static final int[] ABILITY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17};
    private final PlayerData data;
    private final Element element;
    private final Inventory inventory;
    private int pendingAbility = -1;

    public BindGUI(PlayerData data) {
        this.data = data;
        this.element = data.getElement();
        this.inventory = Bukkit.createInventory(this, 36,
                AvatarSMP.MM.deserialize("<gradient:#00c3ff:#8e2de2><bold>Bindowanie Umiejętności</gradient>"));
        render();
    }

    public PlayerData getData() {
        return this.data;
    }

    public int getPendingAbility() {
        return this.pendingAbility;
    }

    public int abilityIndexForSlot(int rawSlot) {
        for (int i = 0; i < ABILITY_SLOTS.length; i++) {
            if (ABILITY_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    public void renderPendingState(int abilityIndex) {
        this.pendingAbility = abilityIndex;
        render();
    }

    private void render() {
        fillThemeFrame();

        String[] names = AbilityRegistry.namesFor(this.element);
        for (int abilityIndex = 0; abilityIndex < names.length && abilityIndex < ABILITY_SLOTS.length; abilityIndex++) {
            boolean unlocked = this.data.getLevel() >= AbilityRegistry.requiredLevel(abilityIndex);
            this.inventory.setItem(ABILITY_SLOTS[abilityIndex],
                    buildAbilityItem(names[abilityIndex], abilityIndex, unlocked,
                            this.data.getAbilitySlot(abilityIndex), abilityIndex == this.pendingAbility));
        }

        this.inventory.setItem(4, buildInfoItem());
        this.inventory.setItem(27, buildResetItem());
        this.inventory.setItem(35, buildSkillsOverviewItem());

        if (this.pendingAbility >= 0) {
            this.inventory.setItem(31, buildPendingItem());
        }
    }

    private ItemStack buildInfoItem() {
        ItemStack item = item("NETHER_STAR");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<aqua><bold>PRZYPISYWANIE MOCY"));
        List<Component> lore = List.of(
                AvatarSMP.MM.deserialize("<white>Kliknij wybraną umiejętność."),
                AvatarSMP.MM.deserialize("<gray>Następnie naciśnij klawisz <white>1-9 <gray>na hotbarze"),
                AvatarSMP.MM.deserialize("<gray>lub wpisz cyfrę na czacie.")
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildResetItem() {
        ItemStack item = item("TNT");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<red><bold>RESETUJ BINDY"));
        List<Component> lore = List.of(
                AvatarSMP.MM.deserialize("<gray>Przywraca domyślny układ"),
                AvatarSMP.MM.deserialize("<gray>przypisania slotów (1-8)."),
                Component.empty(),
                AvatarSMP.MM.deserialize("<red>▸ Kliknij, aby zresetować")
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSkillsOverviewItem() {
        ItemStack item = item("BOOK");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<yellow><bold>PRZEGLĄD UMIEJĘTNOŚCI"));
        List<Component> lore = List.of(
                AvatarSMP.MM.deserialize("<gray>Zobacz szczegółowe opisy,"),
                AvatarSMP.MM.deserialize("<gray>cooldowny oraz koszty energii."),
                Component.empty(),
                AvatarSMP.MM.deserialize("<yellow>▸ Kliknij, aby przejść do /avatar skills")
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPendingItem() {
        ItemStack item = item("COMPASS");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<aqua><bold>OCZEKIWANIE NA SLOT HOTBARA"));
        List<Component> lore = List.of(
                AvatarSMP.MM.deserialize("<white>Wybrana moc: <yellow>" + AbilityRegistry.nameFor(this.element, this.pendingAbility)),
                Component.empty(),
                AvatarSMP.MM.deserialize("<gray>Naciśnij <white>1-9 <gray>na klawiaturze"),
                AvatarSMP.MM.deserialize("<gray>lub wpisz cyfrę na czacie.")
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAbilityItem(String name, int abilityIndex, boolean unlocked, int boundSlot, boolean selected) {
        ItemStack item = unlocked ? item(iconForIndex(this.element, abilityIndex)) : item("GRAY_DYE");
        ItemMeta meta = item.getItemMeta();

        String color = selected ? "<aqua><bold>" : unlocked ? "<white><bold>" : "<dark_gray>";
        meta.displayName(AvatarSMP.MM.deserialize(color + (abilityIndex + 1) + ". " + name));

        List<Component> lore = new ArrayList<>();
        if (unlocked) {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <green><bold>✔ ODBLOKOWANA</bold>"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Slot: " + (boundSlot >= 0 ? "<white><bold>Slot " + (boundSlot + 1) + "</bold>" : "<yellow>Brak")));
            lore.add(Component.empty());
            if (selected) {

                lore.add(AvatarSMP.MM.deserialize("<aqua><bold>  ▶ Wybierz teraz klawisz 1-9"));
            } else {
                lore.add(AvatarSMP.MM.deserialize("<yellow>▸ Lewy-Klik: <white>Przypisz slot"));
                lore.add(AvatarSMP.MM.deserialize("<red>▸ Prawy-Klik: <white>Odepnij slot"));
            }

            XItemFlag.of("HIDE_ENCHANTS").ifPresent(flag -> flag.set(meta));
            if (boundSlot >= 0 || selected) {
                XEnchantment.matchXEnchantment("UNBREAKING").ifPresent(e -> meta.addEnchant(e.getEnchant(), 1, true));
            }
        } else {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <red><bold>🔒 ZABLOKOWANA</bold>"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Wymagany poziom: <white>" + AbilityRegistry.requiredLevel(abilityIndex)));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillThemeFrame() {
        ItemStack primaryPane = namedPane(primaryGlass(this.element));
        ItemStack secondaryPane = namedPane(secondaryGlass(this.element));

        for (int i = 0; i < 36; i++) {
            if (i < 9 || i >= 27 || i % 9 == 0 || i % 9 == 8) {
                this.inventory.setItem(i, secondaryPane);
            } else {
                this.inventory.setItem(i, primaryPane);
            }
        }
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

    private String iconForIndex(Element el, int index) {
        return switch (el) {
            case FIRE -> (index == 7) ? "NETHER_STAR" : "BLAZE_POWDER";
            case WATER -> (index == 7) ? "TRIDENT" : "WATER_BUCKET";
            case EARTH -> (index == 7) ? "ANVIL" : "STONE";
            case AIR -> (index == 7) ? "WIND_CHARGE" : "FEATHER";
            default -> "BARRIER";
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

    public static void open(Player player, PlayerData data) {
        player.openInventory(new BindGUI(data).getInventory());
    }
}