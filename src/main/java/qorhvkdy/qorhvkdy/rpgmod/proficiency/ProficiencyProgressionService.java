package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;

/**
 * 숙련도 변경 단일 진입점.
 * 모든 증가를 여기로 모아 추후 로그/보상/업적 연동 지점을 일원화한다.
 */
public final class ProficiencyProgressionService {
    private ProficiencyProgressionService() {
    }

    public static int grant(ServerPlayer player, ProficiencyType type, int amount) {
        if (amount <= 0) {
            return 0;
        }
        PlayerProficiency data = ProficiencyUtil.get(player);
        int before = data.getLevel(type);
        int applied = data.addExp(type, amount);
        int after = data.getLevel(type);

        // 한글 주석: 숙련도 변경 직후 동기화해 GUI/조건 검사 지연을 줄인다.
        ModNetwork.syncProficiencyToPlayer(player, data);
        if (after > before) {
            // 레벨업 후처리는 추후 이 위치에 확장(칭호/보상 등).
        }
        return applied;
    }
}

