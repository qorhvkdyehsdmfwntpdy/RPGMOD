package qorhvkdy.qorhvkdy.rpgmod.network;

/*
 * [RPGMOD 파일 설명]
 * 역할: 클라이언트가 전체 스탯 동기화를 요청할 때 사용하는 패킷입니다.
 * 수정 예시: GUI 오픈 시 추가 데이터가 필요하면 응답 패킷 필드를 함께 확장합니다.
 */


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsCapability;

public record StatsSyncRequestC2SPacket() {
    public static void encode(StatsSyncRequestC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static StatsSyncRequestC2SPacket decode(FriendlyByteBuf buffer) {
        return new StatsSyncRequestC2SPacket();
    }

    public static void handle(StatsSyncRequestC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }

        sender.getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> ModNetwork.syncToPlayer(sender, stats));
    }
}
