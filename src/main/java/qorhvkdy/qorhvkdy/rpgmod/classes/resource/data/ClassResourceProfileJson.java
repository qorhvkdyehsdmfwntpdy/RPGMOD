package qorhvkdy.qorhvkdy.rpgmod.classes.resource.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 클래스 리소스 프로필 DTO.
 */
public class ClassResourceProfileJson {
    public int dataVersion = 1;
    public Map<String, Profile> profiles = new LinkedHashMap<>();

    public ClassResourceProfileJson() {
        profiles.put("none", new Profile("none", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        profiles.put("warrior", new Profile("rage", 100.0, 0.2, 1.2, 2.5, 4.0, 6.0));
        profiles.put("rogue", new Profile("focus", 100.0, 0.15, 1.5, 2.0, 3.0, 5.0));
        profiles.put("mage", new Profile("mana", 120.0, 0.35, 2.0, 0.0, 0.0, 2.0));
        profiles.put("archer", new Profile("concentration", 100.0, 0.2, 1.4, 1.5, 2.5, 4.0));
    }

    public static final class Profile {
        public String resourceType;
        public double maxBase;
        public double maxPerLevel;
        public double regenPerSecond;
        public double gainOnHit;
        public double gainOnCrit;
        public double gainOnKill;

        public Profile() {
            this("none", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public Profile(
                String resourceType,
                double maxBase,
                double maxPerLevel,
                double regenPerSecond,
                double gainOnHit,
                double gainOnCrit,
                double gainOnKill
        ) {
            this.resourceType = resourceType;
            this.maxBase = maxBase;
            this.maxPerLevel = maxPerLevel;
            this.regenPerSecond = regenPerSecond;
            this.gainOnHit = gainOnHit;
            this.gainOnCrit = gainOnCrit;
            this.gainOnKill = gainOnKill;
        }
    }
}
