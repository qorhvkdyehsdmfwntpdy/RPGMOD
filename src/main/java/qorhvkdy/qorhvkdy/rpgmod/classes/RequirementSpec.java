package qorhvkdy.qorhvkdy.rpgmod.classes;

import java.util.Objects;

/**
 * Serializable requirement specification.
 * - LEVEL: value can be empty, minValue is required level.
 * - PROFICIENCY: value is proficiency type key(class/weapon/gathering/mining), minValue is required level.
 * - QUEST/BOSS_KILL/ITEM: value is identifier, minValue is required count (default 1).
 */
public record RequirementSpec(RequirementType type, String value, int minValue) {
    public RequirementSpec {
        type = type == null ? RequirementType.LEVEL : type;
        value = Objects.requireNonNullElse(value, "");
        minValue = Math.max(0, minValue);
    }

    public static RequirementSpec level(int requiredLevel) {
        return new RequirementSpec(RequirementType.LEVEL, "", Math.max(0, requiredLevel));
    }
}
