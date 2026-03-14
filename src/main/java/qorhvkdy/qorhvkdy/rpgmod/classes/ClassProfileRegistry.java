package qorhvkdy.qorhvkdy.rpgmod.classes;

import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Central registry for base classes.
 * Add new base classes here, and later wire specialization trees per class.
 */
public final class ClassProfileRegistry {
    private static final Map<PlayerClassType, ClassProfile> PROFILES = Map.of(
            PlayerClassType.NONE, profile(
                    PlayerClassType.NONE,
                    "No class selected.",
                    Map.of(),
                    "Any",
                    "Any",
                    "Any",
                    "Tier2-PathA",
                    "Tier2-PathB"
            ),
            PlayerClassType.WARRIOR, profile(
                    PlayerClassType.WARRIOR,
                    "Frontline melee base class.",
                    Map.of(StatType.STR, 1.0, StatType.AGI, 0.5, StatType.WIS, 0.1, StatType.LUK, 0.4),
                    "Greatsword", "Sword+Shield", "Axe",
                    "Tier2-PathA", "Tier2-PathB"
            ),
            PlayerClassType.ROGUE, profile(
                    PlayerClassType.ROGUE,
                    "High-mobility melee class with burst damage.",
                    Map.of(StatType.STR, 0.6, StatType.AGI, 1.0, StatType.WIS, 0.2, StatType.LUK, 0.9),
                    "Dagger", "Dual Blade", "Throwing Knife",
                    "Tier2-PathA", "Tier2-PathB"
            ),
            PlayerClassType.MAGE, profile(
                    PlayerClassType.MAGE,
                    "Ranged caster base class.",
                    Map.of(StatType.STR, 0.1, StatType.AGI, 0.3, StatType.WIS, 1.0, StatType.LUK, 0.6),
                    "Staff", "Wand", "Spellbook",
                    "Tier2-PathA", "Tier2-PathB"
            ),
            PlayerClassType.ARCHER, profile(
                    PlayerClassType.ARCHER,
                    "Mobile ranged physical base class.",
                    Map.of(StatType.STR, 0.4, StatType.AGI, 1.0, StatType.WIS, 0.2, StatType.LUK, 0.7),
                    "Bow", "Crossbow", "Throwing",
                    "Tier2-PathA", "Tier2-PathB"
            )
    );

    private ClassProfileRegistry() {
    }

    public static ClassProfile get(PlayerClassType type) {
        return PROFILES.getOrDefault(type, PROFILES.get(PlayerClassType.NONE));
    }

    private static ClassProfile profile(
            PlayerClassType type,
            String summary,
            Map<StatType, Double> statWeights,
            String weaponA,
            String weaponB,
            String weaponC,
            String specializationA,
            String specializationB
    ) {
        return new ClassProfile(
                type,
                summary,
                toEnumMap(statWeights),
                java.util.List.of(weaponA, weaponB, weaponC),
                java.util.List.of(specializationA, specializationB)
        );
    }

    private static EnumMap<StatType, Double> toEnumMap(Map<StatType, Double> source) {
        EnumMap<StatType, Double> result = new EnumMap<>(StatType.class);
        result.putAll(source);
        return result;
    }
}
