package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.PlayerProficiency;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyCapability;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;

/**
 * 숙련도 전체 스냅샷 동기화 패킷.
 * UI는 이 패킷만 수신하면 모든 숙련도 표시를 갱신할 수 있다.
 */
public record ProficiencySyncS2CPacket(int[] expByType) {
    public ProficiencySyncS2CPacket {
        if (expByType.length != ProficiencyType.values().length) {
            throw new IllegalArgumentException("Invalid proficiency payload size: " + expByType.length);
        }
    }

    public static ProficiencySyncS2CPacket from(PlayerProficiency proficiency) {
        int[] values = new int[ProficiencyType.values().length];
        for (ProficiencyType type : ProficiencyType.values()) {
            values[type.ordinal()] = proficiency.getExp(type);
        }
        return new ProficiencySyncS2CPacket(values);
    }

    public static void encode(ProficiencySyncS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.expByType.length);
        for (int value : packet.expByType) {
            buffer.writeVarInt(value);
        }
    }

    public static ProficiencySyncS2CPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        int[] values = new int[size];
        for (int i = 0; i < size; i++) {
            values[i] = buffer.readVarInt();
        }
        return new ProficiencySyncS2CPacket(values);
    }

    public static void handle(ProficiencySyncS2CPacket packet, CustomPayloadEvent.Context context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getCapability(ProficiencyCapability.PLAYER_PROFICIENCY).ifPresent(data -> {
            for (ProficiencyType type : ProficiencyType.values()) {
                data.setExp(type, packet.expByType[type.ordinal()]);
            }
        });
    }
}

