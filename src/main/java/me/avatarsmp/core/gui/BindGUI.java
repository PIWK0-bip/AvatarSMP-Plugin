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

    private static final int[] ABILITY_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16};

    private final PlayerData data;
    private final Element element;
    private final Inventory inventory;
    private int pendingAbility = -1;

    public BindGUI(PlayerData data) {
        this.data = data;
        this.element = data.getElement();
        this.inventory = Bukkit.createInventory(this, 27,
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
        fillBackground();
        String[] names = AbilityRegistry.namesFor(this.element);
        for (int abilityIndex = 0; abilityIndex < names.length; abilityIndex++) {
            boolean unlocked = this.data.getLevel() >= AbilityRegistry.requiredLevel(abilityIndex);
            this.inventory.setItem(ABILITY_SLOTS[abilityIndex],
                    buildAbilityItem(names[abilityIndex], abilityIndex, unlocked,
                            this.data.getAbilitySlot(abilityIndex), abilityIndex == this.pendingAbility));
        }
        this.inventory.setItem(4, buildInfoItem());
        if (this.pendingAbility >= 0) {
            this.inventory.setItem(22, buildPendingItem());
        }
    }

    private ItemStack buildInfoItem() {
        ItemStack item = item("NETHER_STAR");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<aqua><bold>PRZYPISYWANIE MOCY"));
        List<Component> lore = new ArrayList<>();
        lore.add(AvatarSMP.MM.deserialize("<white>Wybierz jedną z dostępnych umiejętności."));
        lore.add(AvatarSMP.MM.deserialize("<gray>Następnie naciśnij klawisz <white>1-9"));
        lore.add(AvatarSMP.MM.deserialize("<gray>lub wpisz numer slotu na czacie."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPendingItem() {
        ItemStack item = item("COMPASS");
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AvatarSMP.MM.deserialize("<aqua><bold>OCZEKIWANIE NA SLOT"));
        List<Component> lore = new ArrayList<>();
        lore.add(AvatarSMP.MM.deserialize("<white>"
                + AbilityRegistry.nameFor(this.element, this.pendingAbility)));
        lore.add(Component.empty());
        lore.add(AvatarSMP.MM.deserialize("<gray>Naciśnij <white>1-9 <gray>na klawiaturze"));
        lore.add(AvatarSMP.MM.deserialize("<gray>lub wpisz cyfrę na czacie."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAbilityItem(String name, int abilityIndex, boolean unlocked,
                                       int boundSlot, boolean selected) {
        ItemStack item = unlocked ? item(iconFor(this.element)) : item("GRAY_DYE");
        ItemMeta meta = item.getItemMeta();
        String color = selected ? "<aqua><bold>" : unlocked ? "<white><bold>" : "<dark_gray>";
        meta.displayName(AvatarSMP.MM.deserialize(color + name));

        List<Component> lore = new ArrayList<>();
        if (unlocked) {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <green>ODBLOKOWANA"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Slot: "
                    + (boundSlot >= 0 ? "<white>" + (boundSlot + 1) : "<red>brak")));
            lore.add(Component.empty());
            lore.add(AvatarSMP.MM.deserialize(selected
                    ? "<aqua>» Teraz wybierz klawisz 1-9"
                    : "<yellow>» Kliknij, aby wybrać"));
            XItemFlag.of("HIDE_ENCHANTS").ifPresent(flag -> flag.set(meta));
            XEnchantment.matchXEnchantment("UNBREAKING")
                    .ifPresent(enchantment -> meta.addEnchant(enchantment.getEnchant(), 1, true));
        } else {
            lore.add(AvatarSMP.MM.deserialize("<gray>Status: <red>ZABLOKOWANA"));
            lore.add(AvatarSMP.MM.deserialize("<gray>Wymagany poziom: <white>"
                    + AbilityRegistry.requiredLevel(abilityIndex)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground() {
        ItemStack black = namedPane("BLACK_STAINED_GLASS_PANE");
        ItemStack gray = namedPane("GRAY_STAINED_GLASS_PANE");
        for (int slot = 0; slot < this.inventory.getSize(); slot++) {
            this.inventory.setItem(slot, slot < 9 || slot >= 18 ? black : gray);
        }
    }

    private ItemStack namedPane(String material) {
        ItemStack item = item(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private String iconFor(Element element) {
        return switch (element) {
            case FIRE -> "BLAZE_POWDER";
            case WATER -> "WATER_BUCKET";
            case EARTH -> "STONE";
            case AIR -> "FEATHER";
            default -> "BARRIER";
        };
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