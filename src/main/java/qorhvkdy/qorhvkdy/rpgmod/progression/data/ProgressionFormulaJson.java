package qorhvkdy.qorhvkdy.rpgmod.progression.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 성장 수식 조정 JSON DTO.
 * 고정 코드 대신 배율 테이블로 운영 중 밸런스 조정이 가능하도록 분리한다.
 */
public class ProgressionFormulaJson {
    public int dataVersion = 1;
    public double levelRequirementMultiplier = 1.0;
    public Map<String, Double> proficiencyExpMultiplier = new LinkedHashMap<>();

    public ProgressionFormulaJson() {
        proficiencyExpMultiplier.put("class", 1.0);
        proficiencyExpMultiplier.put("weapon", 1.0);
        proficiencyExpMultiplier.put("gathering", 1.0);
        proficiencyExpMultiplier.put("mining", 1.0);
    }
}
