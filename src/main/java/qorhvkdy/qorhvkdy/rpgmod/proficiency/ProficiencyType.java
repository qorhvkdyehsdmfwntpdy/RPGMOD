package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import java.util.Locale;
import java.util.Optional;

/**
 * 숙련도 카테고리 키.
 * 값은 JSON/명령/요구조건에서 공통 키로 사용한다.
 */
public enum ProficiencyType {
    CLASS("class"),
    WEAPON("weapon"),
    GATHERING("gathering"),
    MINING("mining");

    private final String key;

    ProficiencyType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<ProficiencyType> fromKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ProficiencyType value : values()) {
            if (value.key.equals(normalized) || value.name().equalsIgnoreCase(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}

