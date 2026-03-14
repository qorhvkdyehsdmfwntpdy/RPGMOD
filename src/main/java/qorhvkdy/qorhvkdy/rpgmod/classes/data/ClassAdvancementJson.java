package qorhvkdy.qorhvkdy.rpgmod.classes.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for class advancement config.
 */
public class ClassAdvancementJson {
    public int dataVersion = 1;
    public List<Entry> advancements = new ArrayList<>();

    public ClassAdvancementJson() {
        // Shared novice root. Players start here and choose one first-job at Lv.30.
        advancements.add(base("novice_base", "class.rpgmod.adv.novice_base", "NONE", "BASE", null, 0, "Common starting class."));

        // First promotion (Lv.30): one of these four only.
        advancements.add(node("warrior_first", "class.rpgmod.adv.warrior_first", "WARRIOR", "FIRST", "novice_base", 30));
        advancements.add(node("rogue_first", "class.rpgmod.adv.rogue_first", "ROGUE", "FIRST", "novice_base", 30));
        advancements.add(node("mage_first", "class.rpgmod.adv.mage_first", "MAGE", "FIRST", "novice_base", 30));
        advancements.add(node("archer_first", "class.rpgmod.adv.archer_first", "ARCHER", "FIRST", "novice_base", 30));

        // Second promotion (Lv.70): branches per first-job.
        advancements.add(node("warrior_second_warrior", "class.rpgmod.adv.warrior_second_warrior", "WARRIOR", "SECOND", "warrior_first", 70));
        advancements.add(node("warrior_second_swordsman", "class.rpgmod.adv.warrior_second_swordsman", "WARRIOR", "SECOND", "warrior_first", 70));

        advancements.add(node("rogue_second_assassin", "class.rpgmod.adv.rogue_second_assassin", "ROGUE", "SECOND", "rogue_first", 70));

        advancements.add(node("mage_second_pyromancer", "class.rpgmod.adv.mage_second_pyromancer", "MAGE", "SECOND", "mage_first", 70));
        advancements.add(node("mage_second_cryomancer", "class.rpgmod.adv.mage_second_cryomancer", "MAGE", "SECOND", "mage_first", 70));
        advancements.add(node("mage_second_shadowcaster", "class.rpgmod.adv.mage_second_shadowcaster", "MAGE", "SECOND", "mage_first", 70));
        advancements.add(node("mage_second_lightcaster", "class.rpgmod.adv.mage_second_lightcaster", "MAGE", "SECOND", "mage_first", 70));

        advancements.add(node("archer_second_sniper", "class.rpgmod.adv.archer_second_sniper", "ARCHER", "SECOND", "archer_first", 70));
    }

    private static Entry base(String id, String name, String baseClass, String tier, String parentId, int requiredLevel, String summary) {
        Entry e = new Entry();
        e.id = id;
        e.displayName = name;
        e.baseClass = baseClass;
        e.tier = tier;
        e.parentId = parentId;
        e.requiredLevel = requiredLevel;
        e.summary = summary;
        e.weaponHints = List.of("Any");
        e.requirements = List.of(RequirementEntry.level(requiredLevel));
        return e;
    }

    private static Entry node(String id, String name, String baseClass, String tier, String parentId, int requiredLevel) {
        Entry e = new Entry();
        e.id = id;
        e.displayName = name;
        e.baseClass = baseClass;
        e.tier = tier;
        e.parentId = parentId;
        e.requiredLevel = requiredLevel;
        e.summary = name + " placeholder.";
        e.weaponHints = List.of("Any");
        e.requirements = new ArrayList<>();
        e.requirements.add(RequirementEntry.level(requiredLevel));
        e.statWeights.put("str", 1.0);
        e.statWeights.put("agi", 1.0);
        e.statWeights.put("wis", 1.0);
        e.statWeights.put("luk", 1.0);
        if ("SECOND".equalsIgnoreCase(tier)) {
            e.requirements.add(RequirementEntry.proficiency("class", 10));
        }
        return e;
    }

    public static class Entry {
        public String id = "";
        public String displayName = "";
        public String baseClass = "NONE";
        public String tier = "BASE";
        public String parentId = null;
        public int requiredLevel = 0;
        public String summary = "";
        public Map<String, Double> statWeights = new LinkedHashMap<>();
        public List<String> weaponHints = new ArrayList<>();
        public List<RequirementEntry> requirements = new ArrayList<>();
    }

    public static class RequirementEntry {
        public String type = "LEVEL";
        public String value = "";
        public int minValue = 0;

        public static RequirementEntry level(int requiredLevel) {
            RequirementEntry entry = new RequirementEntry();
            entry.type = "LEVEL";
            entry.minValue = requiredLevel;
            return entry;
        }

        public static RequirementEntry stat(String statKey, int minValue) {
            RequirementEntry entry = new RequirementEntry();
            entry.type = "STAT";
            entry.value = statKey;
            entry.minValue = minValue;
            return entry;
        }

        public static RequirementEntry proficiency(String proficiencyKey, int minValue) {
            RequirementEntry entry = new RequirementEntry();
            entry.type = "PROFICIENCY";
            entry.value = proficiencyKey;
            entry.minValue = minValue;
            return entry;
        }
    }
}
