package me.avatarsmp.core.gui;

import me.avatarsmp.core.AvatarSMP;
import me.avatarsmp.core.DataManager;
import me.avatarsmp.core.Element;
import me.avatarsmp.core.Specialization;
import me.avatarsmp.core.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final AvatarSMP plugin;
    private final DataManager dataManager;
    private final GUIManager guiManager;

    public GUIListener(AvatarSMP plugin, DataManager dataManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 1. Element Selection GUI
        if (isElementGUI(event)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Element selectedElement = switch (event.getSlot()) {
                case 10 -> Element.WATER;
                case 12 -> Element.FIRE;
                case 14 -> Element.EARTH;
                case 16 -> Element.AIR;
                default -> null;
            };

            if (selectedElement == null) return;

            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data != null) {
                data.setElement(selectedElement);
                this.dataManager.saveAsync(data);
                
                player.sendMessage(AvatarSMP.MM.deserialize("<gold><bold>You have chosen the path of: <white>" + selectedElement.name() + "</white>!</bold></gold>"));
                
                if (this.plugin.getBendingManager() != null) {
                    this.plugin.getBendingManager().executeOnSelectActions(player, selectedElement);
                }
            }
            player.closeInventory();

        // 2. Specialization GUI
        } else if (event.getInventory().getHolder() instanceof SpecializationGUI gui) {
            event.setCancelled(true);

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
            
            player.sendMessage(AvatarSMP.MM.deserialize("<light_purple><bold>Unlocked specialization: <white>" + specialization.name() + "</white>!</bold></light_purple>"));
            player.closeInventory();

        // 3. Ability Binding GUI
        } else if (event.getInventory().getHolder() instanceof BindGUI gui) {
            this.guiManager.handleBindClick(event, gui);
        }
    }

    private boolean isElementGUI(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        return event.getInventory().getHolder() instanceof ElementSelectionGUI 
            || title.contains("Wybierz Żywioł") 
            || title.contains("Choose Your Element");
    }
}