package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassOperationResult;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassProgressionService;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;

import java.util.Locale;

/**
 * Class action request from GUI.
 */
public record ClassActionC2SPacket(Action action, String value) {
    public enum Action {
        SET_BASE,
        PROMOTE;

        public static Action parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return PROMOTE;
            }
            try {
                return Action.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return PROMOTE;
            }
        }
    }

    public static void encode(ClassActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action.name());
        buffer.writeUtf(packet.value == null ? "" : packet.value);
    }

    public static ClassActionC2SPacket decode(FriendlyByteBuf buffer) {
        Action action = Action.parse(buffer.readUtf());
        String value = buffer.readUtf();
        return new ClassActionC2SPacket(action, value);
    }

    public static void handle(ClassActionC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }

        ClassOperationResult result;
        if (packet.action == Action.SET_BASE) {
            PlayerClassType type = PlayerClassType.fromId(packet.value).orElse(PlayerClassType.NONE);
            result = ClassProgressionService.setBaseClass(sender, sender, type);
        } else {
            result = ClassProgressionService.promote(sender, sender, packet.value);
        }

        if (!result.success()) {
            sender.sendSystemMessage(Component.literal(result.message()));
            for (String detail : result.details()) {
                sender.sendSystemMessage(Component.literal("- " + detail));
            }
            return;
        }
        sender.sendSystemMessage(Component.literal(result.message()));
    }
}
