package qorhvkdy.qorhvkdy.rpgmod.core.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 런타임 기능 토글 JSON DTO.
 * 서버 재시작 없이 모듈 로드 범위를 통제해 안정성과 운영 편의성을 올린다.
 */
public class RuntimeFeatureJson {
    public int dataVersion = 1;
    public Map<String, Boolean> moduleEnabled = new LinkedHashMap<>();

    public RuntimeFeatureJson() {
        moduleEnabled.put("permission-core", true);
        moduleEnabled.put("progression-core", true);
        moduleEnabled.put("itemization-core", true);
        moduleEnabled.put("content-core", true);
    }
}
