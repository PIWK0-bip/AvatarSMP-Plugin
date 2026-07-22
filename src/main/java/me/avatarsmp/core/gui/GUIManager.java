package me.avatarsmp.core.gui;

import com.cryptomorin.xseries.XSound;
import me.avatarsmp.core.AbilityRegistry;
import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.DataManager;
import me.avatarsmp.core.data.PlayerData;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {

    private final AvatarSMP plugin;
    private final DataManager dataManager;
    private final Map<UUID, Integer> pendingAbilities = new ConcurrentHashMap<>();

    public GUIManager(AvatarSMP plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void openMainGUI(Player player, PlayerData data) {
        if (data.getElement() == null || data.getElement() == me.avatarsmp.core.Element.NONE) {
            openElementGUI(player, data);
        } else {
            SkillsGUI.open(player, this.dataManager, this.plugin);
        }
    }

    public void openElementGUI(Player player, PlayerData data) {
        ElementSelectionGUI.open(player);
    }

    public void openBindGUI(Player player, PlayerData data) {
        this.pendingAbilities.remove(player.getUniqueId());
        BindGUI.open(player, data);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof SkillsGUI skillsGui) {
            handleSkillsClick(event, player, skillsGui);
        } else if (event.getInventory().getHolder() instanceof BindGUI bindGui) {
            handleBindClick(event, player, bindGui);
        }
    }

    private void handleSkillsClick(InventoryClickEvent event, Player player, SkillsGUI gui) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 40) {
            openBindGUI(player, gui.getData());
            XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(sound -> sound.play(player));
            return;
        }

        int abilityIndex = gui.abilityIndexForSlot(slot);
        if (abilityIndex >= 0) {
            PlayerData data = gui.getData();
            if (data.getLevel() < AbilityRegistry.requiredLevel(abilityIndex)) {
                player.sendActionBar(AvatarSMP.MM.deserialize("<red>Ta umiejętność jest jeszcze zablokowana."));
                XSound.matchXSound("ENTITY_VILLAGER_NO").ifPresent(sound -> sound.play(player));
                return;
            }

            openBindGUI(player, data);
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BindGUI bindGui) {
                this.pendingAbilities.put(player.getUniqueId(), abilityIndex);
                bindGui.renderPendingState(abilityIndex);
                notifyPendingSelection(player, data, abilityIndex);
            }
        }
    }

    public void handleBindClick(InventoryClickEvent event, Player player, BindGUI gui) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Przycisk Reset
        if (slot == 27) {
            PlayerData data = gui.getData();
            data.resetBindings();
            this.dataManager.saveAsync(data);
            if (this.plugin.getScoreboardManager() != null) {
                this.plugin.getScoreboardManager().update(player);
            }
            openBindGUI(player, data);
            player.sendMessage(AvatarSMP.MM.deserialize("<green>Zresetowano wszystkie bindy do wartości domyślnych!"));
            XSound.matchXSound("BLOCK_ANVIL_USE").ifPresent(sound -> sound.play(player));
            return;
        }

        // Przejście do Skills Overview
        if (slot == 35) {
            SkillsGUI.open(player, this.dataManager, this.plugin);
            XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(sound -> sound.play(player));
            return;
        }

        // Wybór slotu klawiszem numerycznym nad przedmiotem
        if (event.getClick() == ClickType.NUMBER_KEY && gui.getPendingAbility() >= 0) {
            bind(player, event.getHotbarButton());
            return;
        }

        int abilityIndex = gui.abilityIndexForSlot(slot);
        if (abilityIndex < 0) {
            return;
        }

        PlayerData data = gui.getData();
        if (data.getLevel() < AbilityRegistry.requiredLevel(abilityIndex)) {
            player.sendActionBar(AvatarSMP.MM.deserialize("<red>Ta umiejętność jest jeszcze zablokowana."));
            XSound.matchXSound("ENTITY_VILLAGER_NO").ifPresent(sound -> sound.play(player));
            return;
        }

        // Prawy-Klik: Odepnij moc
        if (event.isRightClick()) {
            data.bindAbility(abilityIndex, -1);
            this.dataManager.saveAsync(data);
            if (this.plugin.getScoreboardManager() != null) {
                this.plugin.getScoreboardManager().update(player);
            }
            openBindGUI(player, data);
            player.sendMessage(AvatarSMP.MM.deserialize("<yellow>Odpięto umiejętność <white>" + AbilityRegistry.nameFor(data.getElement(), abilityIndex) + "<yellow>."));
            XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(sound -> sound.play(player));
            return;
        }

        // Lewy-Klik: Rozpocznij bindowanie
        this.pendingAbilities.put(player.getUniqueId(), abilityIndex);
        gui.renderPendingState(abilityIndex);
        notifyPendingSelection(player, data, abilityIndex);
    }

    private void notifyPendingSelection(Player player, PlayerData data, int abilityIndex) {
        String abilityName = AbilityRegistry.nameFor(data.getElement(), abilityIndex);
        player.sendMessage(AvatarSMP.MM.deserialize(
                "<aqua><bold>  <white>Wybierz slot w hotbarze <aqua>(1-9)<white>, naciskając klawisz lub wpisując numer na czacie."));
        player.showTitle(Title.title(
                AvatarSMP.MM.deserialize("<aqua><bold>WYBIERZ SLOT 1-9"),
                AvatarSMP.MM.deserialize("<white>" + abilityName)));
        XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(sound -> sound.play(player));
    }

    @EventHandler
    public void onHotbarSelect(PlayerItemHeldEvent event) {
        if (!this.pendingAbilities.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        bind(event.getPlayer(), event.getNewSlot());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!this.pendingAbilities.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!message.matches("[1-9]")) {
                event.getPlayer().sendMessage(AvatarSMP.MM.deserialize(
                        "<red>Wpisz pojedynczą cyfrę od 1 do 9."));
                return;
            }
            bind(event.getPlayer(), Integer.parseInt(message) - 1);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.pendingAbilities.remove(event.getPlayer().getUniqueId());
    }

    private void bind(Player player, int hotbarSlot) {
        Integer abilityIndex = this.pendingAbilities.remove(player.getUniqueId());
        if (abilityIndex == null || hotbarSlot < 0 || hotbarSlot > 8) {
            return;
        }

        PlayerData data = this.dataManager.getData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(AvatarSMP.MM.deserialize("<red>Dane gracza nie są jeszcze dostępne."));
            return;
        }

        data.bindAbility(abilityIndex, hotbarSlot);
        this.dataManager.saveAsync(data);
        if (this.plugin.getScoreboardManager() != null) {
            this.plugin.getScoreboardManager().update(player);
        }

        player.closeInventory();
        player.sendMessage(AvatarSMP.MM.deserialize("<green>Przypisano <white>"
                + AbilityRegistry.nameFor(data.getElement(), abilityIndex)
                + " <green>do slotu <white>" + (hotbarSlot + 1) + "<green>."));
        XSound.matchXSound("ENTITY_EXPERIENCE_ORB_PICKUP").ifPresent(sound -> sound.play(player));
    }
}