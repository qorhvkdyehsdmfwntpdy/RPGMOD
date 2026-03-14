package qorhvkdy.qorhvkdy.rpgmod.progression;

import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;
import qorhvkdy.qorhvkdy.rpgmod.progression.data.ProgressionFormulaJson;
import qorhvkdy.qorhvkdy.rpgmod.progression.data.ProgressionFormulaRepository;

/**
 * 성장 수식 접근 서비스.
 */
public final class ProgressionFormulaService {
    private ProgressionFormulaService() {
    }

    public static void bootstrap() {
        ProgressionFormulaRepository.bootstrap();
    }

    public static void reload() {
        ProgressionFormulaRepository.reload();
    }

    public static int scaledLevelRequirement(int baseRequiredLevel) {
        ProgressionFormulaJson data = ProgressionFormulaRepository.get();
        double multiplier = clampMultiplier(data.levelRequirementMultiplier);
        return Math.max(0, (int) Math.round(Math.max(0, baseRequiredLevel) * multiplier));
    }

    public static int scaledProficiencyExp(ProficiencyType type, int baseExpCost) {
        ProgressionFormulaJson data = ProgressionFormulaRepository.get();
        String key = type == null ? "" : type.key();
        double multiplier = clampMultiplier(data.proficiencyExpMultiplier.getOrDefault(key, 1.0));
        return Math.max(1, (int) Math.round(Math.max(1, baseExpCost) * multiplier));
    }

    private static double clampMultiplier(double value) {
        return Math.max(0.1, Math.min(10.0, value));
    }
}
