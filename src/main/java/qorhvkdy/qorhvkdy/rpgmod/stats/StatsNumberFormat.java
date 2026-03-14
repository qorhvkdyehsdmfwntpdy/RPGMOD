package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: HUD/GUI 출력용 숫자 포맷(최대 3자리 소수 등)을 담당합니다.
 * 수정 예시: 소수점 규칙 변경이 필요하면 포맷 메서드 하나만 수정하면 됩니다.
 */


import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StatsNumberFormat {
    private StatsNumberFormat() {
    }

    public static String format3(double value) {
        BigDecimal scaled = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return scaled.toPlainString();
    }

    public static String formatFixed3(double value) {
        BigDecimal scaled = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
        return scaled.toPlainString();
    }
}
