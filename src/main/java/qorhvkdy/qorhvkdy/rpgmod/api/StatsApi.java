package qorhvkdy.qorhvkdy.rpgmod.api;

/*
 * [RPGMOD 파일 설명]
 * 역할: 외부 코드가 호출할 스탯 API 계약(인터페이스)을 정의합니다.
 * 수정 예시: 새 조회 기능을 열려면 인터페이스 메서드를 추가하고 구현체를 맞춥니다.
 */


import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.stats.server.DerivedStatsSnapshot;

public interface StatsApi {
    DerivedStatsSnapshot getDerived(Player player);
}
