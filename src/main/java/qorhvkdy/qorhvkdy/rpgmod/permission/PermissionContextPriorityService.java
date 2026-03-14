package qorhvkdy.qorhvkdy.rpgmod.permission;

import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionContextPriorityRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 컨텍스트 우선순위 서비스.
 */
public final class PermissionContextPriorityService {
    private PermissionContextPriorityService() {
    }

    public static void bootstrap() {
        PermissionContextPriorityRepository.bootstrap();
    }

    public static void reload() {
        PermissionContextPriorityRepository.reload();
    }

    public static Map<String, String> sorted(Map<String, String> raw) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        List<String> order = PermissionContextPriorityRepository.get().order;
        for (String key : order) {
            if (raw.containsKey(key)) {
                out.put(key, raw.get(key));
            }
        }
        for (var entry : raw.entrySet()) {
            if (!out.containsKey(entry.getKey())) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(out);
    }

    public static List<String> orderedPairs(Map<String, String> raw) {
        List<String> out = new ArrayList<>();
        for (var entry : sorted(raw).entrySet()) {
            out.add(entry.getKey() + "=" + entry.getValue());
        }
        return out;
    }
}
