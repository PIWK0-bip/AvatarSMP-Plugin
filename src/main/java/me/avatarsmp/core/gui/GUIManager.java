package me.avatarsmp.core.gui;

import com.cryptomorin.xseries.XSound;
import me.avatarsmp.core.AbilityRegistry;
import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.DataManager;
import me.avatarsmp.core.PlayerData;
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

    public void openBindGUI(Player player, PlayerData data) {
        this.pendingAbilities.remove(player.getUniqueId());
        BindGUI.open(player, data);
    }

    public void handleBindClick(InventoryClickEvent event, BindGUI gui) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY && gui.getPendingAbility() >= 0) {
            bind(player, event.getHotbarButton());
            return;
        }

        int abilityIndex = gui.abilityIndexForSlot(event.getRawSlot());
        if (abilityIndex < 0) {
            return;
        }
        PlayerData data = gui.getData();
        if (data.getLevel() < AbilityRegistry.requiredLevel(abilityIndex)) {
            player.sendActionBar(AvatarSMP.MM.deserialize("<red>Ta umiejętność jest jeszcze zablokowana."));
            return;
        }

        this.pendingAbilities.put(player.getUniqueId(), abilityIndex);
        gui.renderPendingState(abilityIndex);
        String abilityName = AbilityRegistry.nameFor(data.getElement(), abilityIndex);
        player.sendMessage(AvatarSMP.MM.deserialize(
                "<aqua><bold>» <white>Wybierz slot w hotbarze <aqua>(1-9)<white>, naciskając klawisz lub wpisując numer na czacie."));
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