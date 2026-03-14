package qorhvkdy.qorhvkdy.rpgmod.combat.profile.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Better Combat 스타일 무기 전투 프로필.
 */
public class CombatProfileJson {
    public int dataVersion = 1;
    public List<Entry> profiles = new ArrayList<>();

    public CombatProfileJson() {
        Entry sample = new Entry();
        sample.itemId = "minecraft:iron_sword";
        sample.comboWindowTicks = 8;
        sample.recoveryTicks = 10;
        sample.damageMultiplier = 1.0;
        profiles.add(sample);
    }

    public static class Entry {
        public String itemId = "";
        public int comboWindowTicks = 6;
        public int recoveryTicks = 8;
        public double damageMultiplier = 1.0;
    }
}
