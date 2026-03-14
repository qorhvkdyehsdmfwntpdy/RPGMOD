package qorhvkdy.qorhvkdy.rpgmod.spell.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Iron's Spells 계열 원소/학파 친화도 설정.
 */
public class SpellSchoolJson {
    public int dataVersion = 1;
    public Map<String, Entry> schools = new LinkedHashMap<>();

    public SpellSchoolJson() {
        schools.put("fire", Entry.of(1.0, 0.6, 0.1));
        schools.put("ice", Entry.of(1.0, 0.5, 0.2));
        schools.put("dark", Entry.of(1.0, 0.4, 0.4));
        schools.put("light", Entry.of(1.0, 0.5, 0.3));
        schools.put("physical", Entry.of(1.0, 0.2, 0.2));
    }

    public static class Entry {
        public double base = 1.0;
        public double wisScale = 0.0;
        public double lukScale = 0.0;

        public static Entry of(double base, double wisScale, double lukScale) {
            Entry e = new Entry();
            e.base = base;
            e.wisScale = wisScale;
            e.lukScale = lukScale;
            return e;
        }
    }
}
