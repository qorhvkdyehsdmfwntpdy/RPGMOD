package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 스탯 capability 안전 접근 및 공통 헬퍼를 제공합니다.
 * 수정 예시: 접근 실패 동작을 바꾸려면 예외 처리 정책을 이 파일에서 조정합니다.
 */


import net.minecraft.world.entity.player.Player;

public final class StatsUtil {
    private StatsUtil() {
    }

    public static PlayerStats get(Player player) {
        return player.getCapability(StatsCapability.PLAYER_STATS)
                .orElseThrow(() -> new IllegalStateException("Stats missing for player " + player.getName().getString()));
    }
}
