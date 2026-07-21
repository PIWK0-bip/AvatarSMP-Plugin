package me.avatarsmp.core.gui;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.DataManager;
import me.avatarsmp.core.Element;
import me.avatarsmp.core.PlayerData;
import me.avatarsmp.core.Specialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final DataManager dataManager;
    private final GUIManager guiManager;

    public GUIListener(DataManager dataManager, GUIManager guiManager) {
        this.dataManager = dataManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SkillsGUI) {
            event.setCancelled(true);
            return;
        }
        if (event.getInventory().getHolder() instanceof ElementSelectionGUI) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data == null || data.getElement() != Element.NONE) {
                player.closeInventory();
                return;
            }
            Element chosen = switch (slot) {
                case 10 -> Element.WATER;
                case 12 -> Element.FIRE;
                case 14 -> Element.EARTH;
                case 16 -> Element.AIR;
                case 31 -> Element.WARRIOR;
                default -> null;
            };
            if (chosen == null) {
                return;
            }
            data.setElement(chosen);
            this.dataManager.saveAsync(data);
            player.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>Wybrałeś ścieżkę: <white>" + chosen.name()));
            player.closeInventory();
        } else if (event.getInventory().getHolder() instanceof SpecializationGUI gui) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data == null || data.getSpecialization() != Specialization.NONE) {
                player.closeInventory();
                return;
            }
            Specialization specialization = switch (gui.getElement()) {
                case FIRE -> Specialization.LIGHTNING;
                case WATER -> Specialization.BLOODBENDING;
                case EARTH -> Specialization.METALBENDING;
                case AIR -> Specialization.FLIGHT;
                default -> Specialization.NONE;
            };
            data.setSpecialization(specialization);
            this.dataManager.saveAsync(data);
            player.sendMessage(AvatarSMP.MM.deserialize("<light_purple><bold>Odblokowano specjalizacj\u0119: <white>" + specialization.name()));
            player.closeInventory();
        } else if (event.getInventory().getHolder() instanceof BindGUI gui) {
            this.guiManager.handleBindClick(event, gui);
        }
    }
}