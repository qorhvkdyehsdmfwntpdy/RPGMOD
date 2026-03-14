package qorhvkdy.qorhvkdy.rpgmod.core.module;

/**
 * 모드 내부 시스템을 공통 방식으로 다루기 위한 런타임 모듈 인터페이스.
 * 대형화 시에도 bootstrap/reload/health 진입점이 고정되도록 유지한다.
 */
public interface RpgRuntimeModule {
    String id();

    String displayName();

    void bootstrap();

    void reload();

    default String health() {
        return "ok";
    }
}
