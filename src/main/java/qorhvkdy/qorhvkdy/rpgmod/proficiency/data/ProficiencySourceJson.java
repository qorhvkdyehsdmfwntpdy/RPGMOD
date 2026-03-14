package qorhvkdy.qorhvkdy.rpgmod.proficiency.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AuraSkills/PMMO 스타일 숙련도 소스 테이블.
 * 이벤트 코드에서 하드코딩된 숫자 대신 sourceId로 경험치 테이블을 참조한다.
 */
public class ProficiencySourceJson {
    public int dataVersion = 1;
    public Map<String, SourceEntry> sources = new LinkedHashMap<>();

    public ProficiencySourceJson() {
        sources.put("mob_kill.class", SourceEntry.of("class", 10));
        sources.put("mob_kill.weapon", SourceEntry.of("weapon", 4));
        sources.put("block_break.gathering", SourceEntry.of("gathering", 6));
        sources.put("block_break.mining", SourceEntry.of("mining", 6));
    }

    public static class SourceEntry {
        public String proficiencyType = "";
        public int amount = 0;
        public double multiplier = 1.0;

        public static SourceEntry of(String proficiencyType, int amount) {
            SourceEntry entry = new SourceEntry();
            entry.proficiencyType = proficiencyType;
            entry.amount = amount;
            entry.multiplier = 1.0;
            return entry;
        }
    }
}
