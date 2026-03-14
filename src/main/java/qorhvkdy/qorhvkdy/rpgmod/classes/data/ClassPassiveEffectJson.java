package qorhvkdy.qorhvkdy.rpgmod.classes.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 클래스 패시브 효과 테이블 DTO.
 */
public class ClassPassiveEffectJson {
    public int dataVersion = 1;
    public Map<String, BonusEntry> passives = new LinkedHashMap<>();

    public ClassPassiveEffectJson() {
        passives.put("novice_core", new BonusEntry(1.02, 1.0, 0.0, 0.5, 0.0, 0.0));
        passives.put("novice_offense", new BonusEntry(1.0, 1.03, 0.0, 0.0, 0.0, 5.0));
        passives.put("novice_utility", new BonusEntry(1.0, 1.0, 2.0, 0.0, 1.0, 0.0));
    }

    public static class BonusEntry {
        public double hpMultiplier = 1.0;
        public double attackDamageMultiplier = 1.0;
        public double moveSpeedBonusPercent = 0.0;
        public double armorBonus = 0.0;
        public double critChanceBonusPercent = 0.0;
        public double critDamageBonusPercent = 0.0;

        public BonusEntry() {
        }

        public BonusEntry(double hpMultiplier, double attackDamageMultiplier, double moveSpeedBonusPercent,
                          double armorBonus, double critChanceBonusPercent, double critDamageBonusPercent) {
            this.hpMultiplier = hpMultiplier;
            this.attackDamageMultiplier = attackDamageMultiplier;
            this.moveSpeedBonusPercent = moveSpeedBonusPercent;
            this.armorBonus = armorBonus;
            this.critChanceBonusPercent = critChanceBonusPercent;
            this.critDamageBonusPercent = critDamageBonusPercent;
        }
    }
}

