package qorhvkdy.qorhvkdy.rpgmod.weapon.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 무기 메타데이터 JSON DTO.
 * 등급/희귀도/옵션/획득처를 데이터로 분리해 밸런싱 반복 속도를 높인다.
 */
public class WeaponDataJson {
    public int dataVersion = 1;
    public List<Entry> weapons = new ArrayList<>();

    public WeaponDataJson() {
        Entry example = new Entry();
        example.itemId = "minecraft:iron_sword";
        example.displayName = "weapon.rpgmod.sample.iron_sword";
        example.category = "class_weapon";
        example.grade = "A";
        example.rarity = "rare";
        example.requiredClass = "warrior";
        example.requiredAdvancement = "";
        example.options = List.of(
                OptionEntry.flat("attack_power", 5, "+5 Attack Power"),
                OptionEntry.percent("crit_chance", 2, "+2% Crit Chance")
        );
        example.affixPool = List.of(
                AffixEntry.prefix("강인한", "strong", 1.05),
                AffixEntry.suffix("학살", "slayer", 1.08)
        );
        example.socket = SocketEntry.of(0, 2, List.of("ruby", "sapphire"));
        example.obtainMethods = List.of("Dungeon Drop", "Blacksmith Craft");
        weapons.add(example);
    }

    public static class Entry {
        public String itemId = "";
        public String displayName = "";
        public String category = "class_weapon";
        public String grade = "D";
        public String rarity = "common";
        public String requiredClass = "";
        public String requiredAdvancement = "";
        public List<OptionEntry> options = new ArrayList<>();
        public List<AffixEntry> affixPool = new ArrayList<>();
        public SocketEntry socket = new SocketEntry();
        public List<String> obtainMethods = new ArrayList<>();
        public boolean dropEnabled = false;
        public double baseDropChance = 1.0;
        public double dropWeight = 1.0;
        public double minMobMaxHealth = 0.0;
    }

    public static class OptionEntry {
        public String mode = "FLAT";
        public String stat = "";
        public double value = 0.0;
        public String label = "";

        public static OptionEntry flat(String stat, double value, String label) {
            OptionEntry entry = new OptionEntry();
            entry.mode = "FLAT";
            entry.stat = stat;
            entry.value = value;
            entry.label = label;
            return entry;
        }

        public static OptionEntry percent(String stat, double value, String label) {
            OptionEntry entry = new OptionEntry();
            entry.mode = "PERCENT";
            entry.stat = stat;
            entry.value = value;
            entry.label = label;
            return entry;
        }
    }

    public static class AffixEntry {
        public String type = "prefix";
        public String id = "";
        public String display = "";
        public double optionMultiplier = 1.0;

        public static AffixEntry prefix(String display, String id, double optionMultiplier) {
            AffixEntry entry = new AffixEntry();
            entry.type = "prefix";
            entry.display = display;
            entry.id = id;
            entry.optionMultiplier = optionMultiplier;
            return entry;
        }

        public static AffixEntry suffix(String display, String id, double optionMultiplier) {
            AffixEntry entry = new AffixEntry();
            entry.type = "suffix";
            entry.display = display;
            entry.id = id;
            entry.optionMultiplier = optionMultiplier;
            return entry;
        }
    }

    public static class SocketEntry {
        public int min = 0;
        public int max = 0;
        public List<String> allowed = new ArrayList<>();

        public static SocketEntry of(int min, int max, List<String> allowed) {
            SocketEntry entry = new SocketEntry();
            entry.min = min;
            entry.max = max;
            entry.allowed = allowed == null ? new ArrayList<>() : new ArrayList<>(allowed);
            return entry;
        }
    }
}
