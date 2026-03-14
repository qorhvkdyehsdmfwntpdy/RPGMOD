package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Top-level player class.
 * Keep this enum small and stable; later specializations should branch from these base classes.
 */
public enum PlayerClassType {
    NONE("none", "None", "class.rpgmod.base.none"),
    WARRIOR("warrior", "Warrior", "class.rpgmod.base.warrior"),
    ROGUE("rogue", "Rogue", "class.rpgmod.base.rogue"),
    MAGE("mage", "Mage", "class.rpgmod.base.mage"),
    ARCHER("archer", "Archer", "class.rpgmod.base.archer");

    private final String id;
    private final String displayName;
    private final String displayNameKey;

    PlayerClassType(String id, String displayName, String displayNameKey) {
        this.id = id;
        this.displayName = displayName;
        this.displayNameKey = displayNameKey;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String displayNameKey() {
        return displayNameKey;
    }

    public Component displayNameComponent() {
        return Component.translatable(displayNameKey);
    }

    public static Optional<PlayerClassType> fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (PlayerClassType value : values()) {
            if (value.id.equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
