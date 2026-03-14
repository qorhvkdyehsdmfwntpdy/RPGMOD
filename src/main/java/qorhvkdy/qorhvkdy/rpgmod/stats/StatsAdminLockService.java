package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 관리자 명령으로 플레이어의 스탯 변경 잠금 상태를 관리합니다.
 * 수정 예시: 잠금을 영구 저장하고 싶다면 NBT/파일 저장소와 연동하면 됩니다.
 */

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsAdminLockService {
    private static final Set<UUID> LOCKED_PLAYERS = ConcurrentHashMap.newKeySet();

    private StatsAdminLockService() {
    }

    public static boolean isLocked(ServerPlayer player) {
        return LOCKED_PLAYERS.contains(player.getUUID());
    }

    public static boolean setLocked(ServerPlayer player, boolean locked) {
        UUID id = player.getUUID();
        if (locked) {
            return LOCKED_PLAYERS.add(id);
        }
        return LOCKED_PLAYERS.remove(id);
    }
}
