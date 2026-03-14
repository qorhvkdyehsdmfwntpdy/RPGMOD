package qorhvkdy.qorhvkdy.rpgmod.classes.skill.data;

import java.util.ArrayList;
import java.util.List;

/**
 * class-skills.json DTO.
 */
public class ClassSkillJson {
    public int dataVersion = 1;
    public List<Entry> skills = new ArrayList<>();

    public ClassSkillJson() {
        skills.add(new Entry("warrior_berserk", "skill.rpgmod.warrior_berserk", "warrior_first", 30, 20, 0, 0, 0, 25.0, 30.0, 120, 1.35, 2.0, 3.0, 10.0));
        skills.add(new Entry("rogue_focus_burst", "skill.rpgmod.rogue_focus_burst", "rogue_first", 30, 0, 20, 0, 10, 20.0, 24.0, 100, 1.28, 1.0, 6.0, 15.0));
        skills.add(new Entry("mage_arcane_barrier", "skill.rpgmod.mage_arcane_barrier", "mage_first", 30, 0, 0, 20, 0, 30.0, 36.0, 140, 1.10, 6.0, 0.0, 0.0));
        skills.add(new Entry("archer_deadeye", "skill.rpgmod.archer_deadeye", "archer_first", 30, 0, 15, 0, 15, 18.0, 20.0, 80, 1.22, 0.0, 8.0, 20.0));
        // 한글 주석: 2차 전직(70+) 기본 예시 스킬. 밸런스는 JSON에서 실시간 튜닝.
        skills.add(new Entry("warrior_last_stand", "skill.rpgmod.warrior_last_stand", "warrior_second_warrior", 70, 40, 0, 0, 10, 45.0, 45.0, 120, 1.40, 8.0, 0.0, 15.0));
        skills.add(new Entry("swordsman_blade_dance", "skill.rpgmod.swordsman_blade_dance", "warrior_second_swordsman", 70, 30, 25, 0, 15, 35.0, 28.0, 100, 1.36, 2.0, 10.0, 18.0));
        skills.add(new Entry("assassin_shadow_execution", "skill.rpgmod.assassin_shadow_execution", "rogue_second_assassin", 70, 10, 35, 0, 30, 30.0, 24.0, 80, 1.45, 0.0, 14.0, 30.0));
        skills.add(new Entry("pyromancer_overheat", "skill.rpgmod.pyromancer_overheat", "mage_second_pyromancer", 70, 0, 0, 40, 10, 45.0, 34.0, 120, 1.42, 0.0, 8.0, 22.0));
        skills.add(new Entry("cryomancer_frozen_core", "skill.rpgmod.cryomancer_frozen_core", "mage_second_cryomancer", 70, 0, 0, 38, 14, 42.0, 34.0, 140, 1.26, 10.0, 0.0, 12.0));
        skills.add(new Entry("shadowcaster_void_mark", "skill.rpgmod.shadowcaster_void_mark", "mage_second_shadowcaster", 70, 0, 0, 36, 24, 40.0, 30.0, 100, 1.34, 2.0, 12.0, 24.0));
        skills.add(new Entry("lightcaster_sanctuary", "skill.rpgmod.lightcaster_sanctuary", "mage_second_lightcaster", 70, 0, 0, 36, 18, 42.0, 38.0, 140, 1.20, 12.0, 0.0, 8.0));
        skills.add(new Entry("sniper_killing_zone", "skill.rpgmod.sniper_killing_zone", "archer_second_sniper", 70, 8, 35, 0, 28, 30.0, 22.0, 80, 1.44, 0.0, 14.0, 28.0));
    }

    public static final class Entry {
        public String id;
        public String displayName;
        public String requiredAdvancementId;
        public int requiredLevel;
        public int minStr;
        public int minAgi;
        public int minWis;
        public int minLuk;
        public double resourceCost;
        public double cooldownSeconds;
        public int durationTicks;
        public double bonusAttackMultiplier;
        public double bonusArmor;
        public double bonusCritChance;
        public double bonusCritDamage;

        public Entry() {
            this("", "", "", 1, 0, 0, 0, 0, 0.0, 1.0, 40, 1.0, 0.0, 0.0, 0.0);
        }

        public Entry(
                String id,
                String displayName,
                String requiredAdvancementId,
                int requiredLevel,
                int minStr,
                int minAgi,
                int minWis,
                int minLuk,
                double resourceCost,
                double cooldownSeconds,
                int durationTicks,
                double bonusAttackMultiplier,
                double bonusArmor,
                double bonusCritChance,
                double bonusCritDamage
        ) {
            this.id = id;
            this.displayName = displayName;
            this.requiredAdvancementId = requiredAdvancementId;
            this.requiredLevel = requiredLevel;
            this.minStr = minStr;
            this.minAgi = minAgi;
            this.minWis = minWis;
            this.minLuk = minLuk;
            this.resourceCost = resourceCost;
            this.cooldownSeconds = cooldownSeconds;
            this.durationTicks = durationTicks;
            this.bonusAttackMultiplier = bonusAttackMultiplier;
            this.bonusArmor = bonusArmor;
            this.bonusCritChance = bonusCritChance;
            this.bonusCritDamage = bonusCritDamage;
        }
    }
}
