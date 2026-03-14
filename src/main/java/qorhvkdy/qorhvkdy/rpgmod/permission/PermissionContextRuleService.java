package qorhvkdy.qorhvkdy.rpgmod.permission;

import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionContextRuleJson;
import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionContextRuleRepository;

import java.util.List;
import java.util.Locale;

/**
 * 컨텍스트 지역 룰 서비스.
 * JSON 룰 기반으로 region id를 판정한다.
 */
public final class PermissionContextRuleService {
    private PermissionContextRuleService() {
    }

    public static void bootstrap() {
        PermissionContextRuleRepository.bootstrap();
    }

    public static void reload() {
        PermissionContextRuleRepository.reload();
    }

    public static String resolveRegion(int distance, String dimension, String module, String time) {
        List<PermissionContextRuleJson.RegionRule> rules = PermissionContextRuleRepository.get().regionRules;
        String dim = normalize(dimension);
        String mod = normalize(module);
        String t = normalize(time);
        for (PermissionContextRuleJson.RegionRule rule : rules) {
            if (rule == null) {
                continue;
            }
            if (distance < rule.minDistance || distance > rule.maxDistance) {
                continue;
            }
            String ruleDimContains = normalize(rule.dimensionContains);
            if (!ruleDimContains.isBlank() && !dim.contains(ruleDimContains)) {
                continue;
            }
            String ruleModule = normalize(rule.module);
            if (!ruleModule.isBlank() && !ruleModule.equals(mod)) {
                continue;
            }
            String ruleTime = normalize(rule.time);
            if (!ruleTime.isBlank() && !ruleTime.equals(t)) {
                continue;
            }
            return normalize(rule.id).isBlank() ? "wild" : normalize(rule.id);
        }
        return "wild";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
