package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.quest.QuestService;

/**
 * 클라이언트의 퀘스트 상태 동기화 요청 패킷.
 */
public class QuestSyncRequestC2SPacket {
    public static void encode(QuestSyncRequestC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static QuestSyncRequestC2SPacket decode(FriendlyByteBuf buffer) {
        return new QuestSyncRequestC2SPacket();
    }

    public static void handle(QuestSyncRequestC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }
        QuestService.sync(sender);
    }
}
