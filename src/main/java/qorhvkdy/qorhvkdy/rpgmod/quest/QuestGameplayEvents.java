package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;

/**
 * 퀘스트 Objective 자동 진행 이벤트.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestGameplayEvents {
    private QuestGameplayEvents() {
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer deadPlayer) {
            int failedCount = QuestService.failDeathSensitiveQuests(deadPlayer);
            if (failedCount > 0) {
                deadPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Quest failed on death: " + failedCount + " (retry cooldown may apply)"
                ));
                QuestService.sync(deadPlayer);
            }
            return;
        }
        if (!(event.getEntity().getKillCredit() instanceof ServerPlayer killer)) {
            return;
        }
        String id = QuestService.normalizeEntityId(event.getEntity().getType());
        if (id.isBlank()) {
            return;
        }
        if (QuestService.onKillMob(killer, id)) {
            QuestService.sync(killer);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        String id = QuestService.normalizeBlockId(event.getState().getBlock());
        if (id.isBlank()) {
            return;
        }
        if (QuestService.onBreakBlock(player, id)) {
            QuestService.sync(player);
        }
    }
}
