package me.avatarsmp.core;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.UUID;

@Getter
@Setter
public class PlayerData {

    private final UUID uuid;
    private Element element = Element.NONE;
    private int level = 1;
    private int xp = 0;
    private Specialization specialization = Specialization.NONE;
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

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", element=" + element + ", bindings=" + Arrays.toString(abilitySlots) + "}";
    }
}