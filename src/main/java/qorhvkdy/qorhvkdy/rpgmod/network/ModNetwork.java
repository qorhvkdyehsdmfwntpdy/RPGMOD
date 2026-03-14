package qorhvkdy.qorhvkdy.rpgmod.network;

/*
 * [RPGMOD 파일 설명]
 * 역할: 패킷 채널 생성과 패킷 등록/송신 유틸리티를 담당합니다.
 * 수정 예시: 새 패킷을 추가하면 register 순서와 ID 충돌 여부를 함께 확인합니다.
 */


import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.client.SkillTreeClientState;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.PlayerProficiency;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

import java.util.List;
import java.util.Objects;

public final class ModNetwork {
    private static final Identifier CHANNEL_ID = Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":main"));
    private static final int PROTOCOL_VERSION = 14;
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(CHANNEL_ID)
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) -> version == PROTOCOL_VERSION)
            .serverAcceptedVersions((status, version) -> version == PROTOCOL_VERSION)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(StatChangeC2SPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StatChangeC2SPacket::encode)
                .decoder(StatChangeC2SPacket::decode)
                .consumerMainThread(StatChangeC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(StatsSyncS2CPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatsSyncS2CPacket::encode)
                .decoder(StatsSyncS2CPacket::decode)
                .consumerMainThread(StatsSyncS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(StatsSyncRequestC2SPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StatsSyncRequestC2SPacket::encode)
                .decoder(StatsSyncRequestC2SPacket::decode)
                .consumerMainThread(StatsSyncRequestC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClassActionC2SPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClassActionC2SPacket::encode)
                .decoder(ClassActionC2SPacket::decode)
                .consumerMainThread(ClassActionC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(ProficiencySyncS2CPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProficiencySyncS2CPacket::encode)
                .decoder(ProficiencySyncS2CPacket::decode)
                .consumerMainThread(ProficiencySyncS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(ProficiencySyncRequestC2SPacket.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProficiencySyncRequestC2SPacket::encode)
                .decoder(ProficiencySyncRequestC2SPacket::decode)
                .consumerMainThread(ProficiencySyncRequestC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(PermissionSyncRequestC2SPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PermissionSyncRequestC2SPacket::encode)
                .decoder(PermissionSyncRequestC2SPacket::decode)
                .consumerMainThread(PermissionSyncRequestC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(PermissionSyncS2CPacket.class, 7, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PermissionSyncS2CPacket::encode)
                .decoder(PermissionSyncS2CPacket::decode)
                .consumerMainThread(PermissionSyncS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(PermActionC2SPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PermActionC2SPacket::encode)
                .decoder(PermActionC2SPacket::decode)
                .consumerMainThread(PermActionC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(QuestSyncRequestC2SPacket.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder(QuestSyncRequestC2SPacket::encode)
                .decoder(QuestSyncRequestC2SPacket::decode)
                .consumerMainThread(QuestSyncRequestC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(QuestSyncS2CPacket.class, 10, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuestSyncS2CPacket::encode)
                .decoder(QuestSyncS2CPacket::decode)
                .consumerMainThread(QuestSyncS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkillTreeSyncRequestC2SPacket.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkillTreeSyncRequestC2SPacket::encode)
                .decoder(SkillTreeSyncRequestC2SPacket::decode)
                .consumerMainThread(SkillTreeSyncRequestC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkillTreeSyncS2CPacket.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkillTreeSyncS2CPacket::encode)
                .decoder(SkillTreeSyncS2CPacket::decode)
                .consumerMainThread(SkillTreeSyncS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkillTreeActionC2SPacket.class, 13, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkillTreeActionC2SPacket::encode)
                .decoder(SkillTreeActionC2SPacket::decode)
                .consumerMainThread(SkillTreeActionC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkillTreeActionResultS2CPacket.class, 14, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkillTreeActionResultS2CPacket::encode)
                .decoder(SkillTreeActionResultS2CPacket::decode)
                .consumerMainThread(SkillTreeActionResultS2CPacket::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }

    public static void syncToPlayer(ServerPlayer player, PlayerStats stats) {
        CHANNEL.send(StatsSyncS2CPacket.from(stats), PacketDistributor.PLAYER.with(player));
    }

    public static void syncProficiencyToPlayer(ServerPlayer player, PlayerProficiency proficiency) {
        CHANNEL.send(ProficiencySyncS2CPacket.from(proficiency), PacketDistributor.PLAYER.with(player));
    }

    public static void syncPermissionToPlayer(ServerPlayer player, RpgPermissionService.PermissionView view) {
        CHANNEL.send(PermissionSyncS2CPacket.from(view), PacketDistributor.PLAYER.with(player));
    }

    public static void syncQuestToPlayer(
            ServerPlayer player,
            List<String> available,
            List<String> accepted,
            List<String> completed,
            List<String> objectiveLines,
            List<String> rewardPreviewLines
    ) {
        CHANNEL.send(new QuestSyncS2CPacket(available, accepted, completed, objectiveLines, rewardPreviewLines), PacketDistributor.PLAYER.with(player));
    }

    public static void syncSkillTreeToPlayer(
            ServerPlayer player,
            int points,
            List<SkillTreeClientState.NodeEntry> nodes
    ) {
        CHANNEL.send(new SkillTreeSyncS2CPacket(points, nodes), PacketDistributor.PLAYER.with(player));
    }

    public static void sendSkillTreeActionResult(ServerPlayer player, boolean success, String message) {
        CHANNEL.send(new SkillTreeActionResultS2CPacket(success, message), PacketDistributor.PLAYER.with(player));
    }
}
