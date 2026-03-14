package qorhvkdy.qorhvkdy.rpgmod.network;

/*
 * [RPGMOD 파일 설명]
 * 역할: 서버 스탯 상태를 클라이언트로 내려주는 S2C 동기화 패킷입니다.
 * 수정 예시: 필드 추가 시 encode/decode/read 순서를 정확히 동일하게 유지합니다.
 */


import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsCapability;

public record StatsSyncS2CPacket(
        int[] statValues,
        int availablePoints,
        String selectedClassId,
        String currentAdvancementId,
        String classResourceType,
        double classResourceCurrent,
        double classResourceMax
) {
    public StatsSyncS2CPacket {
        if (statValues.length != StatType.values().length) {
            throw new IllegalArgumentException("Invalid stat payload size: " + statValues.length);
        }
        if (selectedClassId == null || selectedClassId.isBlank()) {
            selectedClassId = PlayerClassType.NONE.id();
        }
        if (currentAdvancementId == null || currentAdvancementId.isBlank()) {
            currentAdvancementId = "none";
        }
        if (classResourceType == null || classResourceType.isBlank()) {
            classResourceType = "none";
        }
    }

    public static StatsSyncS2CPacket from(PlayerStats stats) {
        int[] values = new int[StatType.values().length];
        for (StatType type : StatType.values()) {
            values[type.ordinal()] = stats.get(type);
        }
        return new StatsSyncS2CPacket(
                values,
                stats.getAvailableStatPoints(),
                stats.getSelectedClass().id(),
                stats.getCurrentAdvancementId(),
                stats.getClassResourceType(),
                stats.getClassResourceCurrent(),
                stats.getClassResourceMax()
        );
    }

    public static void encode(StatsSyncS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.statValues.length);
        for (int value : packet.statValues) {
            buffer.writeVarInt(value);
        }
        buffer.writeVarInt(packet.availablePoints);
        buffer.writeUtf(packet.selectedClassId);
        buffer.writeUtf(packet.currentAdvancementId);
        buffer.writeUtf(packet.classResourceType);
        buffer.writeDouble(packet.classResourceCurrent);
        buffer.writeDouble(packet.classResourceMax);
    }

    public static StatsSyncS2CPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        int[] values = new int[size];
        for (int i = 0; i < size; i++) {
            values[i] = buffer.readVarInt();
        }
        int points = buffer.readVarInt();
        String selectedClassId = buffer.readUtf();
        String currentAdvancementId = buffer.readUtf();
        String classResourceType = buffer.readUtf();
        double classResourceCurrent = buffer.readDouble();
        double classResourceMax = buffer.readDouble();
        return new StatsSyncS2CPacket(values, points, selectedClassId, currentAdvancementId, classResourceType, classResourceCurrent, classResourceMax);
    }

    public static void handle(StatsSyncS2CPacket packet, CustomPayloadEvent.Context context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        player.getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
            for (StatType type : StatType.values()) {
                stats.set(type, packet.statValues[type.ordinal()]);
            }
            stats.setAvailableStatPoints(packet.availablePoints);
            stats.setSelectedClass(PlayerClassType.fromId(packet.selectedClassId).orElse(PlayerClassType.NONE));
            stats.setCurrentAdvancementId(packet.currentAdvancementId);
            stats.setClassResourceType(packet.classResourceType);
            stats.setClassResourceMax(packet.classResourceMax);
            stats.setClassResourceCurrent(packet.classResourceCurrent);
        });
    }
}
