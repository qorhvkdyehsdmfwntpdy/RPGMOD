package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyCapability;

/**
 * 클라이언트 숙련도 동기화 요청 패킷.
 */
public class ProficiencySyncRequestC2SPacket {
    public static void encode(ProficiencySyncRequestC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static ProficiencySyncRequestC2SPacket decode(FriendlyByteBuf buffer) {
        return new ProficiencySyncRequestC2SPacket();
    }

    public static void handle(ProficiencySyncRequestC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }
        sender.getCapability(ProficiencyCapability.PLAYER_PROFICIENCY)
                .ifPresent(data -> ModNetwork.syncProficiencyToPlayer(sender, data));
    }
}

