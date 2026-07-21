package me.avatarsmp.core;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SkillManager {

    private final Map<Element, Map<Integer, ISkill>> skills = new EnumMap<>(Element.class);

    public void register(Element element, int slot, ISkill skill) {
        if (element == null || skill == null || slot < 0 || slot > 7) {
            throw new IllegalArgumentException("Nieprawidłowa rejestracja umiejętności");
        }
        this.skills.computeIfAbsent(element, ignored -> new HashMap<>()).put(slot, skill);
    }

    public ISkill getSkill(Element element, int slot) {
        Map<Integer, ISkill> elementSkills = this.skills.get(element);
        return elementSkills == null ? null : elementSkills.get(slot);
    }
}