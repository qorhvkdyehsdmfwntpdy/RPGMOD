package qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.data;

import java.util.ArrayList;
import java.util.List;

/**
 * class-set-effects.json DTO.
 */
public class ClassSetEffectJson {
    public int dataVersion = 1;
    public List<Entry> sets = new ArrayList<>();

    public ClassSetEffectJson() {
        sets.add(new Entry("warrior_blade_synergy", "weapon", "warrior", "", 1, "", 1.08, 1.5, 0.0, 5.0));
        sets.add(new Entry("rogue_blade_synergy", "weapon", "rogue", "", 1, "", 1.06, 0.5, 2.0, 8.0));
        sets.add(new Entry("mage_arcane_synergy", "weapon", "mage", "", 1, "", 1.05, 1.0, 0.0, 4.0));
        sets.add(new Entry("archer_precision_synergy", "weapon", "archer", "", 1, "", 1.07, 0.0, 3.0, 10.0));
    }

    public static final class Entry {
        public String id;
        public String category;
        public String requiredClass;
        public String requiredAdvancement;
        public int requiredPieces;
        public String requiredPassiveId;
        public double attackMultiplier;
        public double armorBonus;
        public double critChanceBonus;
        public double critDamageBonus;

        public Entry() {
            this("", "", "", "", 1, "", 1.0, 0.0, 0.0, 0.0);
        }

        public Entry(
                String id,
                String category,
                String requiredClass,
                String requiredAdvancement,
                int requiredPieces,
                String requiredPassiveId,
                double attackMultiplier,
                double armorBonus,
                double critChanceBonus,
                double critDamageBonus
        ) {
            this.id = id;
            this.category = category;
            this.requiredClass = requiredClass;
            this.requiredAdvancement = requiredAdvancement;
            this.requiredPieces = requiredPieces;
            this.requiredPassiveId = requiredPassiveId;
            this.attackMultiplier = attackMultiplier;
            this.armorBonus = armorBonus;
            this.critChanceBonus = critChanceBonus;
            this.critDamageBonus = critDamageBonus;
        }
    }
}
