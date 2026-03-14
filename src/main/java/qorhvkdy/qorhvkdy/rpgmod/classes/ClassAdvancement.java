package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.network.chat.Component;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Generic promotion node.
 * Each node can represent base class, first promotion, or second promotion.
 */
public record ClassAdvancement(
        String id,
        String displayName,
        PlayerClassType baseClass,
        JobTier tier,
        String parentId,
        int requiredLevel,
        String summary,
        Map<StatType, Double> statWeights,
        List<String> weaponHints,
        List<RequirementSpec> requirements
) {
    public ClassAdvancement {
        id = normalizeId(id);
        displayName = Objects.requireNonNullElse(displayName, id);
        baseClass = baseClass == null ? PlayerClassType.NONE : baseClass;
        tier = tier == null ? JobTier.BASE : tier;
        parentId = parentId == null || parentId.isBlank() ? null : normalizeId(parentId);
        requiredLevel = Math.max(0, requiredLevel);
        summary = Objects.requireNonNullElse(summary, "");

        Map<StatType, Double> safeWeights = statWeights == null ? Map.of() : statWeights;
        EnumMap<StatType, Double> normalizedWeights = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            normalizedWeights.put(type, Math.max(0.0, safeWeights.getOrDefault(type, 0.0)));
        }
        statWeights = Map.copyOf(normalizedWeights);
        weaponHints = weaponHints == null ? List.of() : List.copyOf(weaponHints);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
    }

    public Optional<String> parentIdOptional() {
        return Optional.ofNullable(parentId);
    }

    public Component displayNameComponent() {
        if (displayName != null && displayName.startsWith("class.rpgmod.")) {
            return Component.translatable(displayName);
        }
        return Component.literal(displayName);
    }

    public static String normalizeId(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}
