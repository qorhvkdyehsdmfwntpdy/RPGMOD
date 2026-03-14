package qorhvkdy.qorhvkdy.rpgmod.stats.server;

/*
 * [RPGMOD 파일 설명]
 * 역할: 서버 계산 파생 스탯 묶음을 전달하는 불변 레코드입니다.
 * 수정 예시: 새 수치 전달 시 record 필드와 생성 코드를 함께 확장합니다.
 */


public record DerivedStatsSnapshot(
        double moveSpeedPercent,
        double attackSpeedPercent,
        double attackPower,
        double magicPower,
        int maxHp,
        int maxMp,
        double critChancePercent,
        double critDamagePercent,
        double defense,
        double hpRegenPercent
) {
}
