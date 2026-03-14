package qorhvkdy.qorhvkdy.rpgmod.classes;

import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.PlayerProficiency;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyCapability;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;
import qorhvkdy.qorhvkdy.rpgmod.progression.ProgressionFormulaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evaluates promotion requirements in a composable way.
 */
public final class ClassAdvancementRequirementEvaluator {
    private static final Map<RequirementType, PromotionRequirement> REQUIREMENT_MAP = Map.of(
            RequirementType.LEVEL, (spec, context) -> {
                int required = ProgressionFormulaService.scaledLevelRequirement(spec.minValue());
                int current = context.player().experienceLevel;
                if (current >= required) {
                    return PromotionRequirement.RequirementCheckResult.pass();
                }
                return PromotionRequirement.RequirementCheckResult.fail("Level " + required + " required. Current: " + current);
            },
            RequirementType.STAT, (spec, context) -> {
                StatType statType = parseStat(spec.value());
                if (statType == null) {
                    return PromotionRequirement.RequirementCheckResult.fail("Invalid stat requirement key: " + spec.value());
                }
                int required = Math.max(0, spec.minValue());
                int current = context.stats().get(statType);
                if (current >= required) {
                    return PromotionRequirement.RequirementCheckResult.pass();
                }
                return PromotionRequirement.RequirementCheckResult.fail("Stat " + statType.key() + " " + required + " required. Current: " + current);
            },
            RequirementType.PROFICIENCY, (spec, context) -> {
                ProficiencyType proficiencyType = ProficiencyType.fromKey(spec.value()).orElse(null);
                if (proficiencyType == null) {
                    return PromotionRequirement.RequirementCheckResult.fail("Invalid proficiency key: " + spec.value());
                }

                PlayerProficiency proficiency = context.player().getCapability(ProficiencyCapability.PLAYER_PROFICIENCY).resolve().orElse(null);
                if (proficiency == null) {
                    return PromotionRequirement.RequirementCheckResult.fail("Proficiency data unavailable.");
                }

                int required = Math.max(0, spec.minValue());
                int current = proficiency.getLevel(proficiencyType);
                if (current >= required) {
                    return PromotionRequirement.RequirementCheckResult.pass();
                }
                return PromotionRequirement.RequirementCheckResult.fail("Proficiency " + proficiencyType.key() + " Lv." + required + " required. Current: Lv." + current);
            },
            RequirementType.QUEST, (spec, context) -> {
                if (PromotionProgressService.hasCompletedQuest(context.player(), spec.value())) {
                    return PromotionRequirement.RequirementCheckResult.pass();
                }
                return PromotionRequirement.RequirementCheckResult.fail("Quest not completed: " + spec.value());
            },
            RequirementType.BOSS_KILL, (spec, context) -> {
                int required = Math.max(1, spec.minValue());
                int kills = PromotionProgressService.getBossKillCount(context.player(), spec.value());
                if (kills >= required) {
                    return PromotionRequirement.RequirementCheckResult.pass();
                }
                return PromotionRequirement.RequirementCheckResult.fail("Boss kill " + spec.value() + " " + kills + "/" + required);
            },
            RequirementType.ITEM, (spec, context) -> {
                int required = Math.max(1, spec.minValue());
                int count = PromotionProgressService.countInventoryItem(context.player(), spec.value());
                if (count >= required) {
                    return PromotionRequirement.RequirementCheckResult.pass();
                }
                return PromotionRequirement.RequirementCheckResult.fail("Item " + spec.value() + " " + count + "/" + required);
            }
    );

    private ClassAdvancementRequirementEvaluator() {
    }

    public static PromotionValidationResult evaluate(PromotionCheckContext context) {
        List<String> errors = new ArrayList<>();
        List<RequirementSpec> requirements = context.target().requirements();

        // Backward compatibility: if no explicit requirements are defined, fallback to requiredLevel.
        if (requirements.isEmpty()) {
            requirements = List.of(RequirementSpec.level(context.target().requiredLevel()));
        }

        for (RequirementSpec spec : requirements) {
            PromotionRequirement requirement = REQUIREMENT_MAP.get(spec.type());
            if (requirement == null) {
                errors.add("Unsupported requirement type: " + spec.type());
                continue;
            }
            PromotionRequirement.RequirementCheckResult result = requirement.check(spec, context);
            if (!result.passed()) {
                errors.add(result.message());
            }
        }
        return new PromotionValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    public record PromotionValidationResult(boolean passed, List<String> errors) {
    }

    private static StatType parseStat(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        for (StatType type : StatType.values()) {
            if (type.key().equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
