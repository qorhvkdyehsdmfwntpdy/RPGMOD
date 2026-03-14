package qorhvkdy.qorhvkdy.rpgmod.proficiency.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 블록별 숙련도 보정치 DTO.
 */
public class ProficiencyBlockRuleJson {
    public int dataVersion = 1;
    public double gatheringDefaultMultiplier = 1.0;
    public double miningDefaultMultiplier = 1.0;
    public Map<String, Double> gathering = new LinkedHashMap<>();
    public Map<String, Double> mining = new LinkedHashMap<>();

    public ProficiencyBlockRuleJson() {
        gathering.put("minecraft:wheat", 1.2);
        gathering.put("minecraft:carrots", 1.2);
        mining.put("minecraft:diamond_ore", 1.5);
        mining.put("minecraft:deepslate_diamond_ore", 1.7);
    }
}

