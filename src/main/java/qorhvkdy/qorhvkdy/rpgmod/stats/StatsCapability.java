package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 플레이어 Capability 키와 접근 상수를 정의합니다.
 * 수정 예시: Capability 이름 변경 시 attach/read 모든 참조를 함께 변경합니다.
 */


import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class StatsCapability {
    public static final Capability<PlayerStats> PLAYER_STATS = CapabilityManager.get(new CapabilityToken<>() {});

    private StatsCapability() {
    }
}
