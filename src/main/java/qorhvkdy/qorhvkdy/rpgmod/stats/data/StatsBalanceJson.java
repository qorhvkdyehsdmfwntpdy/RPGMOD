package qorhvkdy.qorhvkdy.rpgmod.stats.data;

/*
 * [RPGMOD 파일 설명]
 * 역할: 밸런스 JSON 스키마를 Java 객체로 매핑하는 DTO입니다.
 * 수정 예시: JSON 필드를 늘리면 동일한 이름의 멤버를 여기에 추가합니다.
 */


import java.util.LinkedHashMap;
import java.util.Map;

public class StatsBalanceJson {
    public int dataVersion = 1;
    public String formulaProfile = "default-primary-derivation";
    public Map<String, CapEntry> primaryCaps = new LinkedHashMap<>();

    public StatsBalanceJson() {
        primaryCaps.put("str", new CapEntry(120, 500));
        primaryCaps.put("agi", new CapEntry(120, 500));
        primaryCaps.put("wis", new CapEntry(120, 500));
        primaryCaps.put("luk", new CapEntry(100, 500));
    }

    public static class CapEntry {
        public int softCap;
        public int hardCap;

        public CapEntry(int softCap, int hardCap) {
            this.softCap = softCap;
            this.hardCap = hardCap;
        }
    }
}
