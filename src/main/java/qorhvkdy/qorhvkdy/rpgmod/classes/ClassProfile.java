package qorhvkdy.qorhvkdy.rpgmod.classes;

import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata-only profile used by UI/commands/balance docs.
 * This class intentionally avoids gameplay logic so it can evolve safely.
 */
public record ClassProfile(
        PlayerClassType type,
        String summary,
        Map<StatType, Double> statWeights,
        List<String> recommendedWeaponFamilies,
        List<String> specializationSlots
) {
    public ClassProfile {
        EnumMap<StatType, Double> normalized = new EnumMap<>(StatType.class);
        for (StatType statType : StatType.values()) {
            double weight = statWeights.getOrDefault(statType, 0.0);
            normalized.put(statType, Math.max(0.0, weight));
        }
        statWeights = Map.copyOf(normalized);
        recommendedWeaponFamilies = List.copyOf(recommendedWeaponFamilies);
        specializationSlots = List.copyOf(specializationSlots);
    }
}

