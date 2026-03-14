package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.client.QuestClientState;

import java.util.ArrayList;
import java.util.List;

/**
 * 서버 퀘스트 상태 -> 클라이언트 동기화 패킷.
 */
public record QuestSyncS2CPacket(List<String> available, List<String> accepted, List<String> completed, List<String> objectiveLines, List<String> rewardPreviewLines) {
    public static void encode(QuestSyncS2CPacket packet, FriendlyByteBuf buffer) {
        writeList(buffer, packet.available);
        writeList(buffer, packet.accepted);
        writeList(buffer, packet.completed);
        writeList(buffer, packet.objectiveLines);
        writeList(buffer, packet.rewardPreviewLines);
    }

    public static QuestSyncS2CPacket decode(FriendlyByteBuf buffer) {
        return new QuestSyncS2CPacket(readList(buffer), readList(buffer), readList(buffer), readList(buffer), readList(buffer));
    }

    public static void handle(QuestSyncS2CPacket packet, CustomPayloadEvent.Context context) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        QuestClientState.apply(new QuestClientState.Snapshot(packet.available, packet.accepted, packet.completed, packet.objectiveLines, packet.rewardPreviewLines));
    }

    private static void writeList(FriendlyByteBuf buffer, List<String> values) {
        List<String> safe = values == null ? List.of() : values;
        buffer.writeVarInt(safe.size());
        for (String value : safe) {
            buffer.writeUtf(value == null ? "" : value, 128);
        }
    }

    private static List<String> readList(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(buffer.readUtf(128));
        }
        return out;
    }
}
