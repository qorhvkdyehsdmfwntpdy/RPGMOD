package qorhvkdy.qorhvkdy.rpgmod.proficiency.data;

import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 숙련도 수치 밸런스 JSON DTO.
 * 한 파일에서 카테고리별 성장식을 조정할 수 있게 단순 구조로 유지한다.
 */
public class ProficiencyBalanceJson {
    public int dataVersion = 1;
    public Map<String, CurveEntry> categories = new LinkedHashMap<>();

    public ProficiencyBalanceJson() {
        categories.put(ProficiencyType.CLASS.key(), new CurveEntry(100, 1.15, 200));
        categories.put(ProficiencyType.WEAPON.key(), new CurveEntry(90, 1.14, 200));
        categories.put(ProficiencyType.GATHERING.key(), new CurveEntry(80, 1.12, 200));
        categories.put(ProficiencyType.MINING.key(), new CurveEntry(80, 1.12, 200));
    }

    public static class CurveEntry {
        public int baseExpPerLevel = 100;
        public double growth = 1.15;
        public int maxLevel = 200;

        public CurveEntry() {
        }

        public CurveEntry(int baseExpPerLevel, double growth, int maxLevel) {
            this.baseExpPerLevel = baseExpPerLevel;
            this.growth = growth;
            this.maxLevel = maxLevel;
        }
    }
}

