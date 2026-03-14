package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

/**
 * 권한 상태 동기화 요청.
 */
public record PermissionSyncRequestC2SPacket() {
    public static void encode(PermissionSyncRequestC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static PermissionSyncRequestC2SPacket decode(FriendlyByteBuf buffer) {
        return new PermissionSyncRequestC2SPacket();
    }

    public static void handle(PermissionSyncRequestC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }
        ModNetwork.syncPermissionToPlayer(sender, RpgPermissionService.viewOf(sender));
    }
}
