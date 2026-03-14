package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.server.level.ServerPlayer;

/**
 * 숙련도 Capability 안전 접근 헬퍼.
 */
public final class ProficiencyUtil {
    private ProficiencyUtil() {
    }

    public static PlayerProficiency get(ServerPlayer player) {
        return player.getCapability(ProficiencyCapability.PLAYER_PROFICIENCY)
                .orElseThrow(() -> new IllegalStateException("Player proficiency capability missing"));
    }
}

