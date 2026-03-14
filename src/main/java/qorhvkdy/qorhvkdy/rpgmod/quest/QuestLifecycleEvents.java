package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import net.minecraft.server.level.ServerPlayer;

/**
 * 사망/리로그 시 퀘스트 진행도 복사.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestLifecycleEvents {
    private QuestLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            try {
                PlayerQuestProgress oldData = event.getOriginal().getCapability(QuestCapability.PLAYER_QUEST_PROGRESS).resolve().orElse(null);
                PlayerQuestProgress newData = event.getEntity().getCapability(QuestCapability.PLAYER_QUEST_PROGRESS).resolve().orElse(null);
                if (oldData != null && newData != null) {
                    newData.loadSnapshot(
                            new java.util.LinkedHashSet<>(oldData.acceptedList()),
                            new java.util.LinkedHashSet<>(oldData.completedList()),
                            new java.util.LinkedHashMap<>(oldData.objectiveSnapshot()),
                            new java.util.LinkedHashMap<>(oldData.completedAtSnapshot()),
                            new java.util.LinkedHashMap<>(oldData.failedAtSnapshot())
                    );
                }
            } finally {
                event.getOriginal().invalidateCaps();
            }
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestService.refreshMirror(serverPlayer);
            QuestService.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestService.refreshMirror(serverPlayer);
            QuestService.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestService.clearMirror(serverPlayer.getUUID());
        }
    }
}
