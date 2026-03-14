package qorhvkdy.qorhvkdy.rpgmod.classes;

import java.util.Locale;
import java.util.Optional;

/**
 * Promotion requirement category.
 */
public enum RequirementType {
    LEVEL,
    STAT,
    PROFICIENCY,
    QUEST,
    BOSS_KILL,
    ITEM;

    public static Optional<RequirementType> fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (RequirementType value : values()) {
            if (value.name().equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
