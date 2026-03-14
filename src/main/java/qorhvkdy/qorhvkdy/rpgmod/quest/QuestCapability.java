package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * 퀘스트 진행도 Capability 식별자.
 */
public final class QuestCapability {
    public static final Capability<PlayerQuestProgress> PLAYER_QUEST_PROGRESS =
            CapabilityManager.get(new CapabilityToken<>() {});

    private QuestCapability() {
    }
}
