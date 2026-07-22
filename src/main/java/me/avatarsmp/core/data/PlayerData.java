package me.avatarsmp.core.data;

import me.avatarsmp.core.BendingManager;
import me.avatarsmp.core.Element;
import java.util.Arrays;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private Element element = Element.NONE;
    private int level = 1;
    private int xp = 0;
    private boolean chiBlocked = false;
    private boolean doubleJumpAvailable = true;
    private long avatarStateCooldownUntil = 0L;
    
    /**
     * Maps ability index (0-7) -> hotbar slot (0-8) the ability is bound to, or -1 if unbound.
     * Default binds abilities 0-7 to hotbar slots 0-7 (hotbar slot index 8 / "9" stays free).
     */
    private int[] abilitySlots = defaultBindings();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    private static int[] defaultBindings() {
        int[] bindings = new int[8];
        for (int i = 0; i < 8; i++) {
            bindings[i] = i;
        }
        return bindings;
    }

    // --- GETTERY ---
    public UUID getUuid() { return uuid; }
    public Element getElement() { return element; }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public boolean isChiBlocked() { return chiBlocked; }
    public boolean isDoubleJumpAvailable() { return doubleJumpAvailable; }
    public long getAvatarStateCooldownUntil() { return avatarStateCooldownUntil; }
    public int[] getAbilitySlots() { return abilitySlots; }

    // --- SETTERY Z ZABEZPIECZENIAMI ---
    public void setElement(Element element) { this.element = element; }

    /**
     * Ustawia poziom gracza wymuszając twardy zakres [1, MAX_LEVEL].
     */
    public void setLevel(int level) { 
        this.level = Math.min(BendingManager.MAX_LEVEL, Math.max(1, level)); 
    }

    /**
     * Ustawia XP zabezpieczając przed wartościami ujemnymi.
     */
    public void setXp(int xp) { 
        this.xp = Math.max(0, xp); 
    }

    public void setChiBlocked(boolean chiBlocked) { this.chiBlocked = chiBlocked; }
    public void setDoubleJumpAvailable(boolean doubleJumpAvailable) { this.doubleJumpAvailable = doubleJumpAvailable; }
    public void setAvatarStateCooldownUntil(long avatarStateCooldownUntil) { this.avatarStateCooldownUntil = avatarStateCooldownUntil; }
    public void setAbilitySlots(int[] abilitySlots) { this.abilitySlots = abilitySlots; }

    // --- METODY LOGICZNE ---
    public int getAbilitySlot(int abilityIndex) {
        if (abilityIndex < 0 || abilityIndex >= this.abilitySlots.length) {
            return -1;
        }
        return this.abilitySlots[abilityIndex];
    }

    public int getAbilityForSlot(int hotbarSlot) {
        for (int i = 0; i < this.abilitySlots.length; i++) {
            if (this.abilitySlots[i] == hotbarSlot) {
                return i;
            }
        }
        return -1;
    }

    public void bindAbility(int abilityIndex, int hotbarSlot) {
        if (abilityIndex < 0 || abilityIndex >= this.abilitySlots.length) {
            return;
        }
        for (int i = 0; i < this.abilitySlots.length; i++) {
            if (this.abilitySlots[i] == hotbarSlot) {
                this.abilitySlots[i] = -1;
            }
        }
        this.abilitySlots[abilityIndex] = hotbarSlot;
    }

    public void resetBindings() {
        this.abilitySlots = defaultBindings();
    }

    public void removeElement() {
        this.element = Element.NONE; 
    }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", element=" + element + ", level=" + level + ", xp=" + xp + ", bindings=" + Arrays.toString(abilitySlots) + "}";
    }
}