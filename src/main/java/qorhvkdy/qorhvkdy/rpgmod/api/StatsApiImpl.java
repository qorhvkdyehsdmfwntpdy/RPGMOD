package qorhvkdy.qorhvkdy.rpgmod.api;

/*
 * [RPGMOD 파일 설명]
 * 역할: StatsApi의 실제 동작 구현체로 내부 서비스에 연결합니다.
 * 수정 예시: 반환 수식이 바뀌면 구현 메서드 로직만 수정하면 됩니다.
 */


import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;
import qorhvkdy.qorhvkdy.rpgmod.stats.server.DerivedStatsSnapshot;
import qorhvkdy.qorhvkdy.rpgmod.stats.server.ServerStatCalculator;

public final class StatsApiImpl implements StatsApi {
    @Override
    public DerivedStatsSnapshot getDerived(Player player) {
        return ServerStatCalculator.compute(StatsUtil.get(player), player.experienceLevel);
    }
}
