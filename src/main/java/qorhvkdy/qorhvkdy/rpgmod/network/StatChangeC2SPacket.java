package qorhvkdy.qorhvkdy.rpgmod.network;

/*
 * [RPGMOD 파일 설명]
 * 역할: 클라이언트 스탯 증감 요청을 서버로 전달하는 C2S 패킷입니다.
 * 수정 예시: 입력 검증을 강화하려면 서버 핸들러에서 범위 검사 코드를 추가합니다.
 */

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsAdminLockService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsAttributeService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsHistoryService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsSnapshotService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

import java.util.Locale;

public record StatChangeC2SPacket(StatType statType, int delta) {
    public static void encode(StatChangeC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.statType.name());
        buffer.writeInt(packet.delta);
    }

    public static StatChangeC2SPacket decode(FriendlyByteBuf buffer) {
        String typeName = buffer.readUtf();
        int delta = buffer.readInt();
        return new StatChangeC2SPacket(StatType.valueOf(typeName.toUpperCase(Locale.ROOT)), delta);
    }

    public static void handle(StatChangeC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }

        if (StatsAdminLockService.isLocked(sender)) {
            sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("현재 스탯 변경이 잠겨 있습니다. 관리자에게 문의하세요."));
            return;
        }

        PlayerStats stats = StatsUtil.get(sender);
        int beforeValue = stats.get(packet.statType);
        StatsSnapshotService.snapshot(sender, stats, "Before GUI change " + packet.statType.key() + " delta=" + packet.delta);

        int changedAmount = 0;
        if (packet.delta > 0) {
            changedAmount = stats.increaseStatMany(packet.statType, packet.delta);
        } else if (packet.delta < 0) {
            changedAmount = stats.decreaseStatMany(packet.statType, -packet.delta);
        }

        if (changedAmount <= 0) {
            return;
        }

        int afterValue = stats.get(packet.statType);
        StatsAttributeService.apply(sender, stats);
        ModNetwork.syncToPlayer(sender, stats);
        StatsHistoryService.log(sender,
                "GUI stat change: " + packet.statType.name()
                        + " " + beforeValue + " -> " + afterValue
                        + ", requestedDelta=" + packet.delta
                        + ", applied=" + changedAmount
                        + ", points=" + stats.getAvailableStatPoints());
    }
}