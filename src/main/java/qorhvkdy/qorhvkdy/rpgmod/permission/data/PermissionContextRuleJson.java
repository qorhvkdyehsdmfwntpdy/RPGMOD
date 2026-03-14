package qorhvkdy.qorhvkdy.rpgmod.permission.data;

import java.util.ArrayList;
import java.util.List;

/**
 * permission-context-rules.json DTO.
 */
public class PermissionContextRuleJson {
    public int dataVersion = 1;
    public List<RegionRule> regionRules = new ArrayList<>();

    public PermissionContextRuleJson() {
        regionRules.add(new RegionRule("spawn", 0, 256, "", "", ""));
        regionRules.add(new RegionRule("near", 257, 1024, "", "", ""));
        regionRules.add(new RegionRule("wild", 1025, 10000000, "", "", ""));
    }

    public static final class RegionRule {
        public String id = "wild";
        public int minDistance = 0;
        public int maxDistance = 10000000;
        public String dimensionContains = "";
        public String module = "";
        public String time = "";

        public RegionRule() {
        }

        public RegionRule(String id, int minDistance, int maxDistance, String dimensionContains, String module, String time) {
            this.id = id;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.dimensionContains = dimensionContains;
            this.module = module;
            this.time = time;
        }
    }
}
