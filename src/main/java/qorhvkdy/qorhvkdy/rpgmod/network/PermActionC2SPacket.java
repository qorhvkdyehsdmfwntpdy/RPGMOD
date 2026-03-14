package qorhvkdy.qorhvkdy.rpgmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

import java.util.Locale;
import java.util.UUID;

/**
 * Perm GUI 액션 패킷.
 * arg1/arg2를 공통 페이로드로 사용해 액션 추가 비용을 줄인다.
 */
public record PermActionC2SPacket(Action action, String arg1, String arg2) {
    public enum Action {
        PARTY_CREATE_SELF,
        PARTY_INVITE_TARGET,
        PARTY_ACCEPT_INVITE,
        PARTY_LEAVE_SELF,
        PARTY_KICK_TARGET,
        PARTY_DISBAND_SELF,
        PARTY_FORCE_KICK,
        PERM_SET_GROUP_UUID,
        PERM_ADD_NODE_UUID,
        PERM_REMOVE_NODE_UUID,
        PERM_ADD_TEMP_NODE_UUID,
        PERM_ADD_CONTEXT_NODE_UUID,
        PERM_REMOVE_CONTEXT_NODE_UUID,
        PERM_SET_GROUP_WEIGHT,
        PERM_SET_GROUP_PREFIX,
        PERM_SET_GROUP_META;

        static Action parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return PARTY_CREATE_SELF;
            }
            try {
                return Action.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return PARTY_CREATE_SELF;
            }
        }
    }

    public static void encode(PermActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action.name());
        buffer.writeUtf(packet.arg1 == null ? "" : packet.arg1);
        buffer.writeUtf(packet.arg2 == null ? "" : packet.arg2);
    }

    public static PermActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new PermActionC2SPacket(Action.parse(buffer.readUtf()), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(PermActionC2SPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }

        switch (packet.action) {
            case PARTY_CREATE_SELF -> send(sender, PartyService.createParty(sender.getUUID()));
            case PARTY_ACCEPT_INVITE -> send(sender, PartyService.acceptInvite(sender.getUUID()));
            case PARTY_LEAVE_SELF -> send(sender, PartyService.leave(sender.getUUID()));
            case PARTY_DISBAND_SELF -> send(sender, PartyService.disband(sender.getUUID()));
            case PARTY_INVITE_TARGET -> {
                ServerPlayer target = findOnlinePlayer(sender, packet.arg1);
                if (target == null) {
                    send(sender, "Target not online: " + packet.arg1);
                    return;
                }
                String result = PartyService.invite(sender.getUUID(), target.getUUID());
                send(sender, result);
                target.sendSystemMessage(Component.literal(sender.getName().getString() + " invited you. Use /party accept"));
            }
            case PARTY_KICK_TARGET -> {
                ServerPlayer target = findOnlinePlayer(sender, packet.arg1);
                if (target == null) {
                    send(sender, "Target not online: " + packet.arg1);
                    return;
                }
                send(sender, PartyService.kick(sender.getUUID(), target.getUUID()));
            }
            case PARTY_FORCE_KICK -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PARTY_FORCE_KICK)) {
                    send(sender, "No permission: " + PermissionNodes.PARTY_FORCE_KICK);
                    return;
                }
                ServerPlayer target = findOnlinePlayer(sender, packet.arg1);
                if (target == null) {
                    send(sender, "Target not online: " + packet.arg1);
                    return;
                }
                String result = PartyService.forceKick(target);
                send(sender, result);
                target.sendSystemMessage(Component.literal("You were removed from party by admin."));
                RpgAuditLogService.permission("perm_action actor=" + sender.getName().getString()
                        + ", action=party_force_kick, target=" + target.getName().getString());
            }
            case PERM_SET_GROUP_UUID -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                UUID target = parseUuid(packet.arg1);
                if (target == null) {
                    send(sender, "Invalid uuid: " + packet.arg1);
                    return;
                }
                if (!RpgPermissionService.setUserGroup(target, packet.arg2)) {
                    send(sender, "Invalid group: " + packet.arg2);
                    return;
                }
                send(sender, "Set group by uuid: " + target + " -> " + packet.arg2);
            }
            case PERM_ADD_NODE_UUID -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                UUID target = parseUuid(packet.arg1);
                if (target == null) {
                    send(sender, "Invalid uuid: " + packet.arg1);
                    return;
                }
                if (!RpgPermissionService.addUserNode(target, packet.arg2)) {
                    send(sender, "Failed to add node (invalid/duplicate)");
                    return;
                }
                send(sender, "Added node by uuid: " + target + " + " + packet.arg2);
            }
            case PERM_REMOVE_NODE_UUID -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                UUID target = parseUuid(packet.arg1);
                if (target == null) {
                    send(sender, "Invalid uuid: " + packet.arg1);
                    return;
                }
                if (!RpgPermissionService.removeUserNode(target, packet.arg2)) {
                    send(sender, "Failed to remove node");
                    return;
                }
                send(sender, "Removed node by uuid: " + target + " - " + packet.arg2);
            }
            case PERM_ADD_TEMP_NODE_UUID -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                UUID target = parseUuid(packet.arg1);
                if (target == null) {
                    send(sender, "Invalid uuid: " + packet.arg1);
                    return;
                }
                TempSpec spec = parseTempSpec(packet.arg2);
                if (spec == null) {
                    send(sender, "Invalid temp format. use: <seconds>|<node>");
                    return;
                }
                if (!RpgPermissionService.addUserTempNode(target, spec.node, spec.seconds)) {
                    send(sender, "Failed to add temporary node");
                    return;
                }
                send(sender, "Added temp node by uuid: " + target + " + " + spec.node + " (" + spec.seconds + "s)");
            }
            case PERM_ADD_CONTEXT_NODE_UUID -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                UUID target = parseUuid(packet.arg1);
                if (target == null) {
                    send(sender, "Invalid uuid: " + packet.arg1);
                    return;
                }
                ContextSpec spec = parseContextSpec(packet.arg2);
                if (spec == null) {
                    send(sender, "Invalid context format. use: <selector>|<node>");
                    return;
                }
                if (!RpgPermissionService.addUserContextNode(target, spec.selector, spec.node)) {
                    send(sender, "Failed to add context node");
                    return;
                }
                send(sender, "Added context node by uuid: " + target + " [" + spec.selector + "] + " + spec.node);
            }
            case PERM_REMOVE_CONTEXT_NODE_UUID -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                UUID target = parseUuid(packet.arg1);
                if (target == null) {
                    send(sender, "Invalid uuid: " + packet.arg1);
                    return;
                }
                ContextSpec spec = parseContextSpec(packet.arg2);
                if (spec == null) {
                    send(sender, "Invalid context format. use: <selector>|<node>");
                    return;
                }
                if (!RpgPermissionService.removeUserContextNode(target, spec.selector, spec.node)) {
                    send(sender, "Failed to remove context node");
                    return;
                }
                send(sender, "Removed context node by uuid: " + target + " [" + spec.selector + "] - " + spec.node);
            }
            case PERM_SET_GROUP_WEIGHT -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                try {
                    int weight = Integer.parseInt(packet.arg2.trim());
                    if (!RpgPermissionService.setGroupWeight(packet.arg1, weight)) {
                        send(sender, "Failed to set group weight");
                        return;
                    }
                    send(sender, "Set group weight: " + packet.arg1 + " -> " + weight);
                } catch (NumberFormatException e) {
                    send(sender, "Invalid weight: " + packet.arg2);
                }
            }
            case PERM_SET_GROUP_PREFIX -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                if (!RpgPermissionService.setGroupPrefix(packet.arg1, packet.arg2)) {
                    send(sender, "Failed to set group prefix");
                    return;
                }
                send(sender, "Set group prefix: " + packet.arg1 + " -> " + packet.arg2);
            }
            case PERM_SET_GROUP_META -> {
                if (!RpgPermissionService.has(sender, PermissionNodes.PERMISSION_ADMIN)) {
                    send(sender, "No permission: " + PermissionNodes.PERMISSION_ADMIN);
                    return;
                }
                MetaSpec spec = parseMetaSpec(packet.arg2);
                if (spec == null) {
                    send(sender, "Invalid meta format. use: <key>|<value>");
                    return;
                }
                if (!RpgPermissionService.setGroupMeta(packet.arg1, spec.key, spec.value)) {
                    send(sender, "Failed to set group meta");
                    return;
                }
                send(sender, "Set group meta: " + packet.arg1 + " [" + spec.key + "=" + spec.value + "]");
            }
        }
    }

    private static void send(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static ServerPlayer findOnlinePlayer(ServerPlayer requester, String nameRaw) {
        String targetName = nameRaw == null ? "" : nameRaw.trim();
        if (targetName.isBlank()) {
            return null;
        }
        if (!(requester.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.players().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TempSpec parseTempSpec(String raw) {
        String value = raw == null ? "" : raw.trim();
        int sep = value.indexOf('|');
        if (sep <= 0 || sep >= value.length() - 1) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value.substring(0, sep).trim());
            String node = value.substring(sep + 1).trim();
            if (seconds <= 0 || node.isBlank()) {
                return null;
            }
            return new TempSpec(seconds, node);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record TempSpec(long seconds, String node) {
    }

    private static ContextSpec parseContextSpec(String raw) {
        String value = raw == null ? "" : raw.trim();
        int sep = value.indexOf('|');
        if (sep <= 0 || sep >= value.length() - 1) {
            return null;
        }
        String selector = value.substring(0, sep).trim();
        String node = value.substring(sep + 1).trim();
        if (!selector.contains("=") || node.isBlank()) {
            return null;
        }
        return new ContextSpec(selector, node);
    }

    private record ContextSpec(String selector, String node) {
    }

    private static MetaSpec parseMetaSpec(String raw) {
        String value = raw == null ? "" : raw.trim();
        int sep = value.indexOf('|');
        if (sep <= 0 || sep >= value.length() - 1) {
            return null;
        }
        String key = value.substring(0, sep).trim();
        String val = value.substring(sep + 1).trim();
        if (key.isBlank()) {
            return null;
        }
        return new MetaSpec(key, val);
    }

    private record MetaSpec(String key, String value) {
    }
}
