package qorhvkdy.qorhvkdy.rpgmod.core.runtime;

/**
 * 런타임 기능 토글 접근 서비스.
 */
public final class RuntimeFeatureService {
    private RuntimeFeatureService() {
    }

    public static void bootstrap() {
        RuntimeFeatureRepository.bootstrap();
    }

    public static void reload() {
        RuntimeFeatureRepository.reload();
    }

    public static boolean moduleEnabled(String moduleId) {
        String key = normalize(moduleId);
        return RuntimeFeatureRepository.get().moduleEnabled.getOrDefault(key, true);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}
