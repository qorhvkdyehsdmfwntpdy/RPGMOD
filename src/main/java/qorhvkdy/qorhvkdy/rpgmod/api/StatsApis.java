package qorhvkdy.qorhvkdy.rpgmod.api;

/*
 * [RPGMOD 파일 설명]
 * 역할: StatsApi 접근용 정적 파사드로 호출 코드를 단순화합니다.
 * 수정 예시: 자주 쓰는 API 호출 패턴을 정적 헬퍼로 추가해 재사용합니다.
 */


public final class StatsApis {
    private static final StatsApi API = new StatsApiImpl();

    private StatsApis() {
    }

    public static StatsApi get() {
        return API;
    }
}
