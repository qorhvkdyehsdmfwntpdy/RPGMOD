package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraft.server.level.ServerPlayer;

/**
 * 퀘스트 capability 조회 유틸.
 */
public final class QuestUtil {
    private QuestUtil() {
    }

    public static PlayerQuestProgress get(ServerPlayer player) {
        return player.getCapability(QuestCapability.PLAYER_QUEST_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("Player quest capability missing"));
    }
}
