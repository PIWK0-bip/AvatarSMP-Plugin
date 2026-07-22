package me.avatarsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

import me.avatarsmp.core.data.PlayerData;

public class PlayerInteractListener implements Listener {

    private final AvatarSMP plugin;
    private final BendingManager bendingManager;
    private final ComboManager comboManager;

    public PlayerInteractListener(AvatarSMP plugin, BendingManager bendingManager, ComboManager comboManager) {
        this.plugin = plugin;
        this.bendingManager = bendingManager;
        this.comboManager = comboManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Action action = event.getAction();
        boolean leftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        if (!leftClick && !rightClick) return;

        if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) && event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();

        // --- WYSTRZAŁ KULI (LPM) ---
        if (leftClick && this.bendingManager.isHoldingSphere(player)) {
            this.bendingManager.shootWaterSphere(player);
            // Odblokowanie niszczenia bloków przy strzale
            if (action != Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
            return;
        }

        ComboManager.ActionType actionType = leftClick ? ComboManager.ActionType.LEFT : ComboManager.ActionType.RIGHT;
        this.comboManager.registerAction(player, actionType);

        String clickName = leftClick ? "LPM" : "PPM";
        String trigger = player.isSneaking() ? "SHIFT+" + clickName : clickName;

        boolean activated = this.activateSelectedSkill(player, trigger);
        
        // Odblokowanie niszczenia bloków po aktywacji skillów z LPM
        if (activated && action != Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
    
        if (event.isSneaking()) {
            this.comboManager.registerAction(player, ComboManager.ActionType.SHIFT);
            this.bendingManager.handleSneak(player);
            this.activateSelectedSkill(player, "SHIFT");
        } else {
            // Gdy gracz puści SHIFT, wypuszczamy Tsunami jeśli było ładowane
            if (me.avatarsmp.core.skill.water.TsunamiSkill.isCharging(player)) {
                me.avatarsmp.core.skill.water.TsunamiSkill.release(player);
            }
        }
    }

    private boolean activateSelectedSkill(Player player, String trigger) {
        PlayerData data = this.plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
            return false;
        }

        int heldSlot = player.getInventory().getHeldItemSlot();
        int abilityIndex = data.getAbilityForSlot(heldSlot);
        if (abilityIndex == -1) {
            return false;
        }

        return this.bendingManager.activateSkill(player, abilityIndex, trigger);
    }
}