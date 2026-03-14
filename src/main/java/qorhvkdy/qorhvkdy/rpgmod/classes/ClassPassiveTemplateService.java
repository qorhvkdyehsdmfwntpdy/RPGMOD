package qorhvkdy.qorhvkdy.rpgmod.classes;

import qorhvkdy.qorhvkdy.rpgmod.classes.data.ClassPassiveJson;
import qorhvkdy.qorhvkdy.rpgmod.classes.data.ClassPassiveRepository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 패시브 슬롯 템플릿 조회 서비스.
 * 문자열 맵 형태로 유지해 향후 슬롯 확장(예: survival, boss 등)을 쉽게 만든다.
 */
public final class ClassPassiveTemplateService {
    private static volatile Map<String, Map<String, String>> templates = Map.of();

    private ClassPassiveTemplateService() {
    }

    public static void bootstrap() {
        ClassPassiveRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        ClassPassiveRepository.reload();
        ClassPassiveJson data = ClassPassiveRepository.get();
        Map<String, Map<String, String>> next = new LinkedHashMap<>();
        for (ClassPassiveJson.Entry entry : data.templates) {
            String advId = normalize(entry.advancementId);
            if (advId.isBlank()) {
                continue;
            }
            Map<String, String> slots = new LinkedHashMap<>();
            for (Map.Entry<String, String> slot : entry.slots.entrySet()) {
                String slotKey = normalize(slot.getKey());
                if (slotKey.isBlank()) {
                    continue;
                }
                slots.put(slotKey, normalize(slot.getValue()));
            }
            next.put(advId, Map.copyOf(slots));
        }
        templates = Map.copyOf(next);
    }

    public static Map<String, String> resolveForAdvancement(String advancementId) {
        return templates.getOrDefault(normalize(advancementId), Map.of());
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}

