package qorhvkdy.qorhvkdy.rpgmod.stats.server;

/*
 * [RPGMOD 파일 설명]
 * 역할: 서버 권한 기준으로 파생 스탯을 계산하는 전용 모듈입니다.
 * 수정 예시: 치명타 계산 정책 변경은 이 서버 계산 모듈에서만 수정합니다.
 */


import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

public final class ServerStatCalculator {
    private ServerStatCalculator() {
    }

    public static DerivedStatsSnapshot compute(PlayerStats stats, int level) {
        return new DerivedStatsSnapshot(
                stats.getMoveSpeedPercent(),
                stats.getAttackSpeedPercent(),
                stats.getAttackPower(),
                stats.getMagicPower(),
                stats.getMaxHP(level),
                stats.getMaxMP(level),
                stats.getCritChance(),
                stats.getCritDamage(),
                stats.getDefense(),
                stats.getHpRegenPercent()
        );
    }
}
