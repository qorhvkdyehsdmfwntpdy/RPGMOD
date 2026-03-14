package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.client.PermissionClientState;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

/**
 * 서버 권한 상태 -> 클라이언트 동기화 패킷.
 */
public record PermissionSyncS2CPacket(
        String groupId,
        String prefix,
        int weight,
        boolean permissionAdmin,
        boolean classAdmin,
        boolean statsAdmin,
        boolean proficiencyAdmin,
        boolean debugAdmin,
        boolean permUiOpen,
        boolean skillTreeUiOpen,
        boolean partyManage,
        boolean partyForceKick,
        boolean guildManage,
        boolean titleManage
) {
    public static PermissionSyncS2CPacket from(RpgPermissionService.PermissionView view) {
        return new PermissionSyncS2CPacket(
                view.groupId(),
                view.prefix(),
                view.weight(),
                view.permissionAdmin(),
                view.classAdmin(),
                view.statsAdmin(),
                view.proficiencyAdmin(),
                view.debugAdmin(),
                view.permUiOpen(),
                view.skillTreeUiOpen(),
                view.partyManage(),
                view.partyForceKick(),
                view.guildManage(),
                view.titleManage()
        );
    }

    public static void encode(PermissionSyncS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.groupId == null ? "player" : packet.groupId);
        buffer.writeUtf(packet.prefix == null ? "" : packet.prefix);
        buffer.writeVarInt(packet.weight);
        buffer.writeBoolean(packet.permissionAdmin);
        buffer.writeBoolean(packet.classAdmin);
        buffer.writeBoolean(packet.statsAdmin);
        buffer.writeBoolean(packet.proficiencyAdmin);
        buffer.writeBoolean(packet.debugAdmin);
        buffer.writeBoolean(packet.permUiOpen);
        buffer.writeBoolean(packet.skillTreeUiOpen);
        buffer.writeBoolean(packet.partyManage);
        buffer.writeBoolean(packet.partyForceKick);
        buffer.writeBoolean(packet.guildManage);
        buffer.writeBoolean(packet.titleManage);
    }

    public static PermissionSyncS2CPacket decode(FriendlyByteBuf buffer) {
        return new PermissionSyncS2CPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(PermissionSyncS2CPacket packet, CustomPayloadEvent.Context context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        PermissionClientState.apply(new PermissionClientState.Snapshot(
                packet.groupId,
                packet.prefix,
                packet.weight,
                packet.permissionAdmin,
                packet.classAdmin,
                packet.statsAdmin,
                packet.proficiencyAdmin,
                packet.debugAdmin,
                packet.permUiOpen,
                packet.skillTreeUiOpen,
                packet.partyManage,
                packet.partyForceKick,
                packet.guildManage,
                packet.titleManage
        ));
    }
}
