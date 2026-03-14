package qorhvkdy.qorhvkdy.rpgmod.passive.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 스탯 임계치 패시브 스킬 테이블 DTO.
 */
public class StatPassiveSkillJson {
    public int dataVersion = 1;
    public List<Entry> skills = new ArrayList<>();

    public StatPassiveSkillJson() {
        skills.add(Entry.of("str_guardian", "str", 30, 1.05, 1.02, 0.0, 1.5, 0.0, 5.0));
        skills.add(Entry.of("agi_swiftness", "agi", 30, 1.0, 1.03, 4.0, 0.0, 1.0, 0.0));
        skills.add(Entry.of("wis_focus", "wis", 30, 1.02, 1.0, 0.0, 0.5, 0.0, 8.0));
        skills.add(Entry.of("luk_fortune", "luk", 30, 1.0, 1.0, 2.0, 0.0, 3.0, 5.0));
    }

    public static class Entry {
        public String id = "";
        public String stat = "str";
        public int requiredStat = 0;
        public double hpMultiplier = 1.0;
        public double attackDamageMultiplier = 1.0;
        public double moveSpeedBonusPercent = 0.0;
        public double armorBonus = 0.0;
        public double critChanceBonusPercent = 0.0;
        public double critDamageBonusPercent = 0.0;

        public static Entry of(String id, String stat, int requiredStat,
                               double hpMultiplier, double attackDamageMultiplier, double moveSpeedBonusPercent,
                               double armorBonus, double critChanceBonusPercent, double critDamageBonusPercent) {
            Entry e = new Entry();
            e.id = id;
            e.stat = stat;
            e.requiredStat = requiredStat;
            e.hpMultiplier = hpMultiplier;
            e.attackDamageMultiplier = attackDamageMultiplier;
            e.moveSpeedBonusPercent = moveSpeedBonusPercent;
            e.armorBonus = armorBonus;
            e.critChanceBonusPercent = critChanceBonusPercent;
            e.critDamageBonusPercent = critDamageBonusPercent;
            return e;
        }
    }
}

