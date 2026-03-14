package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import java.util.EnumMap;
import java.util.Map;

/**
 * 플레이어 숙련도 상태 모델.
 * 경험치만 저장하고 레벨은 CurveService에서 계산해 확장성과 일관성을 확보한다.
 */
public class PlayerProficiency {
    private final EnumMap<ProficiencyType, Integer> expByType = new EnumMap<>(ProficiencyType.class);

    public PlayerProficiency() {
        for (ProficiencyType type : ProficiencyType.values()) {
            expByType.put(type, 0);
        }
    }

    public int getExp(ProficiencyType type) {
        return expByType.getOrDefault(type, 0);
    }

    public int getLevel(ProficiencyType type) {
        return ProficiencyCurveService.levelOf(type, getExp(type));
    }

    public int getExpIntoCurrentLevel(ProficiencyType type) {
        return ProficiencyCurveService.expIntoCurrentLevel(type, getExp(type));
    }

    public int getExpForNextLevel(ProficiencyType type) {
        return ProficiencyCurveService.expForNextLevel(type, getLevel(type));
    }

    public void setExp(ProficiencyType type, int exp) {
        expByType.put(type, Math.max(0, exp));
    }

    public int addExp(ProficiencyType type, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int next = getExp(type) + amount;
        setExp(type, next);
        return amount;
    }

    public Map<ProficiencyType, Integer> snapshotExp() {
        return Map.copyOf(expByType);
    }

    public void loadSnapshot(Map<ProficiencyType, Integer> values) {
        for (ProficiencyType type : ProficiencyType.values()) {
            setExp(type, values.getOrDefault(type, 0));
        }
    }
}

