package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBalanceJson;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBalanceRepository;
import qorhvkdy.qorhvkdy.rpgmod.progression.ProgressionFormulaService;

/**
 * 숙련도 레벨 계산 서비스.
 * 계산식을 한 곳으로 모아 수치 조정과 디버깅을 단순화한다.
 */
public final class ProficiencyCurveService {
    private ProficiencyCurveService() {
    }

    public static int levelOf(ProficiencyType type, int exp) {
        ProficiencyBalanceJson.CurveEntry curve = curve(type);
        int safeExp = Math.max(0, exp);
        int level = 0;
        int consumed = 0;
        while (level < curve.maxLevel) {
            int nextCost = expForNextLevel(type, level);
            if (consumed + nextCost > safeExp) {
                break;
            }
            consumed += nextCost;
            level++;
        }
        return level;
    }

    public static int expForNextLevel(ProficiencyType type, int currentLevel) {
        ProficiencyBalanceJson.CurveEntry curve = curve(type);
        int level = Math.max(0, currentLevel);
        double raw = curve.baseExpPerLevel * Math.pow(curve.growth, level);
        int base = Math.max(1, (int) Math.round(raw));
        return ProgressionFormulaService.scaledProficiencyExp(type, base);
    }

    public static int expIntoCurrentLevel(ProficiencyType type, int exp) {
        int level = levelOf(type, exp);
        int spent = expForLevel(type, level);
        return Math.max(0, exp - spent);
    }

    public static int expForLevel(ProficiencyType type, int targetLevel) {
        int level = Math.max(0, targetLevel);
        int sum = 0;
        for (int i = 0; i < level; i++) {
            sum += expForNextLevel(type, i);
        }
        return sum;
    }

    private static ProficiencyBalanceJson.CurveEntry curve(ProficiencyType type) {
        ProficiencyBalanceJson data = ProficiencyBalanceRepository.get();
        ProficiencyBalanceJson.CurveEntry curve = data.categories.get(type.key());
        if (curve == null) {
            curve = new ProficiencyBalanceJson.CurveEntry();
        }
        curve.baseExpPerLevel = Math.max(1, curve.baseExpPerLevel);
        curve.growth = Math.max(1.01, curve.growth);
        curve.maxLevel = Math.max(1, curve.maxLevel);
        return curve;
    }
}
