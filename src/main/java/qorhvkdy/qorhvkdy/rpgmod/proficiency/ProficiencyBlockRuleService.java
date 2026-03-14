package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBlockRuleJson;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBlockRuleRepository;

/**
 * 블록 기반 숙련도 배율 계산 서비스.
 */
public final class ProficiencyBlockRuleService {
    private ProficiencyBlockRuleService() {
    }

    public static void bootstrap() {
        ProficiencyBlockRuleRepository.bootstrap();
    }

    public static void reload() {
        ProficiencyBlockRuleRepository.reload();
    }

    public static double gatheringMultiplier(String blockId) {
        ProficiencyBlockRuleJson data = ProficiencyBlockRuleRepository.get();
        Double override = data.gathering.get(normalize(blockId));
        if (override != null) {
            return clamp(override);
        }
        return clamp(data.gatheringDefaultMultiplier);
    }

    public static double miningMultiplier(String blockId) {
        ProficiencyBlockRuleJson data = ProficiencyBlockRuleRepository.get();
        Double override = data.mining.get(normalize(blockId));
        if (override != null) {
            return clamp(override);
        }
        return clamp(data.miningDefaultMultiplier);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(10.0, value));
    }
}

