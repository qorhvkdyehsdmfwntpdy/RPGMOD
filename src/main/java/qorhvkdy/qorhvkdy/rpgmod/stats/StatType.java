package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 기본 스탯 타입(STR/AGI/WIS/LUK) 메타 정보를 정의합니다.
 * 수정 예시: 새 기본 스탯 추가 시 enum 상수와 표시명, 키를 함께 정의합니다.
 */


public enum StatType {
    STR("str", "STR"),
    AGI("agi", "AGI"),
    WIS("wis", "WIS"),
    LUK("luk", "LUK");

    private final String key;
    private final String displayName;

    StatType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }
}
