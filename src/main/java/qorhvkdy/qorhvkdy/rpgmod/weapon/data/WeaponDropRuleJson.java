package qorhvkdy.qorhvkdy.rpgmod.weapon.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 무기 드랍 파이프라인 룰 DTO.
 */
public class WeaponDropRuleJson {
    public int dataVersion = 1;
    public boolean enabled = true;
    public boolean playerKillOnly = true;
    public int maxDropsPerKill = 1;
    public double globalDropChance = 0.25;
    public double optionRollMin = 0.85;
    public double optionRollMax = 1.15;
    public Map<String, Double> rarityWeight = new LinkedHashMap<>();
    public Map<String, Double> gradeWeight = new LinkedHashMap<>();

    public WeaponDropRuleJson() {
        rarityWeight.put("common", 1.0);
        rarityWeight.put("rare", 1.6);
        rarityWeight.put("epic", 2.2);
        rarityWeight.put("legendary", 3.0);

        gradeWeight.put("d", 1.0);
        gradeWeight.put("c", 1.1);
        gradeWeight.put("b", 1.25);
        gradeWeight.put("a", 1.5);
        gradeWeight.put("s", 1.8);
    }
}

