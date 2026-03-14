package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionContextRuleService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

import java.util.UUID;

/**
 * 경량 권한 커맨드.
 * 실시간 운영 편의성을 위해 서버 재시작 없이 수정/리로드가 가능하다.
 */
public final class PermissionCommand {
    private static final SuggestionProvider<CommandSourceStack> GROUP_SUGGESTIONS = (context, builder) -> {
        for (String group : RpgPermissionService.listGroups()) {
            builder.suggest(group);
        }
        return builder.buildFuture();
    };

    private PermissionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgperm")
                .requires(PermissionCommand::isPermissionAdmin)
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("check")
                        .then(Commands.literal("self")
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> checkSelf(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "node")
                                        ))))
                        .then(Commands.literal("self_explain")
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> checkSelfExplain(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "node")
                                        ))))
                        .then(Commands.literal("user")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> checkTarget(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "node")
                                                )))))
                        .then(Commands.literal("user_explain")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> checkTargetExplain(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "node")
                                                ))))))
                .then(groupBranch())
                .then(userBranch())
                .then(partyBranch())
                .then(Commands.literal("undo")
                        .executes(context -> undo(context.getSource()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> groupBranch() {
        return Commands.literal("group")
                .then(Commands.literal("list")
                        .executes(context -> listGroups(context.getSource())))
                .then(Commands.literal("addnode")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> addGroupNode(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "group"),
                                                StringArgumentType.getString(context, "node")
                                        )))))
                .then(Commands.literal("addcontextnode")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("selector", StringArgumentType.word())
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> addGroupContextNode(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "group"),
                                                        StringArgumentType.getString(context, "selector"),
                                                        StringArgumentType.getString(context, "node")
                                                ))))))
                .then(Commands.literal("removecontextnode")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("selector", StringArgumentType.word())
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> removeGroupContextNode(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "group"),
                                                        StringArgumentType.getString(context, "selector"),
                                                        StringArgumentType.getString(context, "node")
                                                ))))))
                .then(Commands.literal("setweight")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("weight", IntegerArgumentType.integer(-1000, 1000))
                                        .executes(context -> setGroupWeight(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "group"),
                                                IntegerArgumentType.getInteger(context, "weight")
                                        )))))
                .then(Commands.literal("setprefix")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("prefix", StringArgumentType.greedyString())
                                        .executes(context -> setGroupPrefix(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "group"),
                                                StringArgumentType.getString(context, "prefix")
                                        )))))
                .then(Commands.literal("setmeta")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(context -> setGroupMeta(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "group"),
                                                        StringArgumentType.getString(context, "key"),
                                                        StringArgumentType.getString(context, "value")
                                                ))))))
                .then(Commands.literal("removenode")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(GROUP_SUGGESTIONS)
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> removeGroupNode(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "group"),
                                                StringArgumentType.getString(context, "node")
                                        )))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> userBranch() {
        return Commands.literal("user")
                .then(Commands.literal("setgroup")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(GROUP_SUGGESTIONS)
                                        .executes(context -> setUserGroup(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "group")
                                        )))))
                .then(Commands.literal("addnode")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> addUserNode(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "node")
                                        )))))
                .then(Commands.literal("addtempnode")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 604800))
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> addUserTempNode(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "seconds"),
                                                        StringArgumentType.getString(context, "node")
                                                ))))))
                .then(Commands.literal("removenode")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> removeUserNode(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "node")
                                        )))))
                .then(Commands.literal("setgroup_uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(GROUP_SUGGESTIONS)
                                        .executes(context -> setUserGroupByUuid(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "uuid"),
                                                StringArgumentType.getString(context, "group")
                                        )))))
                .then(Commands.literal("addnode_uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> addUserNodeByUuid(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "uuid"),
                                                StringArgumentType.getString(context, "node")
                                        )))))
                .then(Commands.literal("removenode_uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .executes(context -> removeUserNodeByUuid(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "uuid"),
                                                StringArgumentType.getString(context, "node")
                                        )))))
                .then(Commands.literal("addcontextnode_uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .then(Commands.argument("selector", StringArgumentType.word())
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> addUserContextNodeByUuid(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "uuid"),
                                                        StringArgumentType.getString(context, "selector"),
                                                        StringArgumentType.getString(context, "node")
                                                ))))))
                .then(Commands.literal("removecontextnode_uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .then(Commands.argument("selector", StringArgumentType.word())
                                        .then(Commands.argument("node", StringArgumentType.greedyString())
                                                .executes(context -> removeUserContextNodeByUuid(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "uuid"),
                                                        StringArgumentType.getString(context, "selector"),
                                                        StringArgumentType.getString(context, "node")
                                                ))))))
                .then(Commands.literal("purge_expired")
                        .executes(context -> purgeExpired(context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> partyBranch() {
        return Commands.literal("party")
                .then(Commands.literal("show")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> partyShow(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("partyId", StringArgumentType.word())
                                        .executes(context -> partySet(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "partyId")
                                        )))))
                .then(Commands.literal("forcekick")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> partyForceKick(context.getSource(), EntityArgument.getPlayer(context, "target")))));
    }

    private static int reload(CommandSourceStack source) {
        PermissionContextRuleService.reload();
        RpgPermissionService.reload();
        PartyService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded permissions + permission-context-rules.json"), true);
        return 1;
    }

    private static int checkSelf(CommandSourceStack source, String node) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) {
            source.sendSuccess(() -> Component.literal("Console has all permissions."), false);
            return 1;
        }
        boolean allowed = RpgPermissionService.has(actor, node);
        source.sendSuccess(() -> Component.literal(actor.getName().getString() + " has(" + node + ")=" + allowed), false);
        return 1;
    }

    private static int checkSelfExplain(CommandSourceStack source, String node) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) {
            source.sendSuccess(() -> Component.literal("Console has all permissions."), false);
            return 1;
        }
        RpgPermissionService.PermissionEvaluation eval = RpgPermissionService.explain(
                actor.getUUID(),
                node,
                RpgPermissionService.PermissionContext.fromPlayer(actor)
        );
        source.sendSuccess(() -> Component.literal(
                actor.getName().getString()
                        + " has(" + node + ")=" + eval.allowed()
                        + " | matched=" + eval.matchedCandidate()
                        + " | reason=" + eval.reason()
        ), false);
        return 1;
    }

    private static int checkTarget(CommandSourceStack source, ServerPlayer target, String node) {
        boolean allowed = RpgPermissionService.has(target, node);
        source.sendSuccess(() -> Component.literal(target.getName().getString() + " has(" + node + ")=" + allowed), false);
        return 1;
    }

    private static int checkTargetExplain(CommandSourceStack source, ServerPlayer target, String node) {
        RpgPermissionService.PermissionEvaluation eval = RpgPermissionService.explain(
                target.getUUID(),
                node,
                RpgPermissionService.PermissionContext.fromPlayer(target)
        );
        source.sendSuccess(() -> Component.literal(
                target.getName().getString()
                        + " has(" + node + ")=" + eval.allowed()
                        + " | matched=" + eval.matchedCandidate()
                        + " | reason=" + eval.reason()
        ), false);
        return 1;
    }

    private static int listGroups(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Groups: " + RpgPermissionService.listGroups()), false);
        return 1;
    }

    private static int addGroupContextNode(CommandSourceStack source, String groupId, String selector, String node) {
        if (!RpgPermissionService.addGroupContextNode(groupId, selector, node)) {
            source.sendFailure(Component.literal("Failed to add group context node."));
            return 0;
        }
        RpgAuditLogService.permission("add_group_context_node actor=" + source.getTextName()
                + ", group=" + groupId + ", selector=" + selector + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Added group context node: " + groupId + " [" + selector + "] + " + node), true);
        return 1;
    }

    private static int removeGroupContextNode(CommandSourceStack source, String groupId, String selector, String node) {
        if (!RpgPermissionService.removeGroupContextNode(groupId, selector, node)) {
            source.sendFailure(Component.literal("Failed to remove group context node."));
            return 0;
        }
        RpgAuditLogService.permission("remove_group_context_node actor=" + source.getTextName()
                + ", group=" + groupId + ", selector=" + selector + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Removed group context node: " + groupId + " [" + selector + "] - " + node), true);
        return 1;
    }

    private static int setGroupWeight(CommandSourceStack source, String groupId, int weight) {
        if (!RpgPermissionService.setGroupWeight(groupId, weight)) {
            source.sendFailure(Component.literal("Failed to set group weight."));
            return 0;
        }
        RpgAuditLogService.permission("set_group_weight actor=" + source.getTextName() + ", group=" + groupId + ", weight=" + weight);
        source.sendSuccess(() -> Component.literal("Set group weight: " + groupId + " -> " + weight), true);
        return 1;
    }

    private static int setGroupPrefix(CommandSourceStack source, String groupId, String prefix) {
        if (!RpgPermissionService.setGroupPrefix(groupId, prefix)) {
            source.sendFailure(Component.literal("Failed to set group prefix."));
            return 0;
        }
        RpgAuditLogService.permission("set_group_prefix actor=" + source.getTextName() + ", group=" + groupId + ", prefix=" + prefix);
        source.sendSuccess(() -> Component.literal("Set group prefix: " + groupId + " -> " + prefix), true);
        return 1;
    }

    private static int setGroupMeta(CommandSourceStack source, String groupId, String key, String value) {
        if (!RpgPermissionService.setGroupMeta(groupId, key, value)) {
            source.sendFailure(Component.literal("Failed to set group meta."));
            return 0;
        }
        RpgAuditLogService.permission("set_group_meta actor=" + source.getTextName()
                + ", group=" + groupId + ", key=" + key + ", value=" + value);
        source.sendSuccess(() -> Component.literal("Set group meta: " + groupId + " [" + key + "=" + value + "]"), true);
        return 1;
    }

    private static int setUserGroup(CommandSourceStack source, ServerPlayer target, String groupId) {
        if (!RpgPermissionService.setUserGroup(target.getUUID(), groupId)) {
            source.sendFailure(Component.literal("Invalid group: " + groupId));
            return 0;
        }
        RpgAuditLogService.permission("set_group actor=" + source.getTextName() + ", target=" + target.getName().getString() + ", group=" + groupId);
        source.sendSuccess(() -> Component.literal("Set group: " + target.getName().getString() + " -> " + groupId), true);
        return 1;
    }

    private static int addUserNode(CommandSourceStack source, ServerPlayer target, String node) {
        if (!RpgPermissionService.addUserNode(target.getUUID(), node)) {
            source.sendFailure(Component.literal("Failed to add user node (already exists or invalid)."));
            return 0;
        }
        RpgAuditLogService.permission("add_user_node actor=" + source.getTextName() + ", target=" + target.getName().getString() + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Added user node: " + target.getName().getString() + " + " + node), true);
        return 1;
    }

    private static int addUserTempNode(CommandSourceStack source, ServerPlayer target, int seconds, String node) {
        if (!RpgPermissionService.addUserTempNode(target.getUUID(), node, seconds)) {
            source.sendFailure(Component.literal("Failed to add temporary user node (invalid input)."));
            return 0;
        }
        RpgAuditLogService.permission("add_user_temp_node actor=" + source.getTextName()
                + ", target=" + target.getName().getString()
                + ", node=" + node
                + ", seconds=" + seconds);
        source.sendSuccess(() -> Component.literal(
                "Added temporary user node: " + target.getName().getString() + " + " + node + " (" + seconds + "s)"
        ), true);
        return 1;
    }

    private static int removeUserNode(CommandSourceStack source, ServerPlayer target, String node) {
        if (!RpgPermissionService.removeUserNode(target.getUUID(), node)) {
            source.sendFailure(Component.literal("Failed to remove user node."));
            return 0;
        }
        RpgAuditLogService.permission("remove_user_node actor=" + source.getTextName() + ", target=" + target.getName().getString() + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Removed user node: " + target.getName().getString() + " - " + node), true);
        return 1;
    }

    private static int addGroupNode(CommandSourceStack source, String groupId, String node) {
        if (!RpgPermissionService.addGroupNode(groupId, node)) {
            source.sendFailure(Component.literal("Failed to add group node (group not found or duplicate)."));
            return 0;
        }
        RpgAuditLogService.permission("add_group_node actor=" + source.getTextName() + ", group=" + groupId + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Added group node: " + groupId + " + " + node), true);
        return 1;
    }

    private static int removeGroupNode(CommandSourceStack source, String groupId, String node) {
        if (!RpgPermissionService.removeGroupNode(groupId, node)) {
            source.sendFailure(Component.literal("Failed to remove group node."));
            return 0;
        }
        RpgAuditLogService.permission("remove_group_node actor=" + source.getTextName() + ", group=" + groupId + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Removed group node: " + groupId + " - " + node), true);
        return 1;
    }

    private static int setUserGroupByUuid(CommandSourceStack source, String uuidRaw, String groupId) {
        UUID userId = parseUuid(source, uuidRaw);
        if (userId == null) {
            return 0;
        }
        if (!RpgPermissionService.setUserGroup(userId, groupId)) {
            source.sendFailure(Component.literal("Invalid group: " + groupId));
            return 0;
        }
        RpgAuditLogService.permission("set_group_uuid actor=" + source.getTextName() + ", targetUuid=" + userId + ", group=" + groupId);
        source.sendSuccess(() -> Component.literal("Set group by uuid: " + userId + " -> " + groupId), true);
        return 1;
    }

    private static int addUserNodeByUuid(CommandSourceStack source, String uuidRaw, String node) {
        UUID userId = parseUuid(source, uuidRaw);
        if (userId == null) {
            return 0;
        }
        if (!RpgPermissionService.addUserNode(userId, node)) {
            source.sendFailure(Component.literal("Failed to add user node (already exists or invalid)."));
            return 0;
        }
        RpgAuditLogService.permission("add_user_node_uuid actor=" + source.getTextName() + ", targetUuid=" + userId + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Added user node by uuid: " + userId + " + " + node), true);
        return 1;
    }

    private static int removeUserNodeByUuid(CommandSourceStack source, String uuidRaw, String node) {
        UUID userId = parseUuid(source, uuidRaw);
        if (userId == null) {
            return 0;
        }
        if (!RpgPermissionService.removeUserNode(userId, node)) {
            source.sendFailure(Component.literal("Failed to remove user node."));
            return 0;
        }
        RpgAuditLogService.permission("remove_user_node_uuid actor=" + source.getTextName() + ", targetUuid=" + userId + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Removed user node by uuid: " + userId + " - " + node), true);
        return 1;
    }

    private static int addUserContextNodeByUuid(CommandSourceStack source, String uuidRaw, String selector, String node) {
        UUID userId = parseUuid(source, uuidRaw);
        if (userId == null) {
            return 0;
        }
        if (!RpgPermissionService.addUserContextNode(userId, selector, node)) {
            source.sendFailure(Component.literal("Failed to add user context node."));
            return 0;
        }
        RpgAuditLogService.permission("add_user_context_node_uuid actor=" + source.getTextName()
                + ", targetUuid=" + userId + ", selector=" + selector + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Added user context node by uuid: " + userId + " [" + selector + "] + " + node), true);
        return 1;
    }

    private static int removeUserContextNodeByUuid(CommandSourceStack source, String uuidRaw, String selector, String node) {
        UUID userId = parseUuid(source, uuidRaw);
        if (userId == null) {
            return 0;
        }
        if (!RpgPermissionService.removeUserContextNode(userId, selector, node)) {
            source.sendFailure(Component.literal("Failed to remove user context node."));
            return 0;
        }
        RpgAuditLogService.permission("remove_user_context_node_uuid actor=" + source.getTextName()
                + ", targetUuid=" + userId + ", selector=" + selector + ", node=" + node);
        source.sendSuccess(() -> Component.literal("Removed user context node by uuid: " + userId + " [" + selector + "] - " + node), true);
        return 1;
    }

    private static int purgeExpired(CommandSourceStack source) {
        int removed = RpgPermissionService.purgeExpiredTempNodes(true);
        source.sendSuccess(() -> Component.literal("Purged expired temp nodes: " + removed), true);
        return 1;
    }

    private static int undo(CommandSourceStack source) {
        if (!RpgPermissionService.undoLastMutation()) {
            source.sendFailure(Component.literal("No permission mutation history to undo."));
            return 0;
        }
        RpgAuditLogService.permission("undo_permission_mutation actor=" + source.getTextName()
                + ", remainingHistory=" + RpgPermissionService.mutationHistorySize());
        source.sendSuccess(() -> Component.literal("Undo complete. Remaining history: " + RpgPermissionService.mutationHistorySize()), true);
        return 1;
    }

    private static int partyShow(CommandSourceStack source, ServerPlayer target) {
        String party = PartyService.getPartyId(target.getUUID()).orElse("-");
        source.sendSuccess(() -> Component.literal(target.getName().getString() + " party=" + party), false);
        return 1;
    }

    private static int partySet(CommandSourceStack source, ServerPlayer target, String partyId) {
        if (!RpgPermissionService.hasOrOp(source, PermissionNodes.PARTY_FORCE_KICK)) {
            source.sendFailure(Component.literal("No permission: " + PermissionNodes.PARTY_FORCE_KICK));
            return 0;
        }
        PartyService.assignParty(target.getUUID(), partyId);
        RpgAuditLogService.permission("party_set actor=" + source.getTextName() + ", target=" + target.getName().getString() + ", party=" + partyId);
        source.sendSuccess(() -> Component.literal("Assigned party: " + target.getName().getString() + " -> " + partyId), true);
        return 1;
    }

    private static int partyForceKick(CommandSourceStack source, ServerPlayer target) {
        if (!RpgPermissionService.hasOrOp(source, PermissionNodes.PARTY_FORCE_KICK)) {
            source.sendFailure(Component.literal("No permission: " + PermissionNodes.PARTY_FORCE_KICK));
            return 0;
        }
        String result = PartyService.forceKick(target);
        RpgAuditLogService.permission("party_forcekick actor=" + source.getTextName() + ", target=" + target.getName().getString());
        source.sendSuccess(() -> Component.literal(result), true);
        target.sendSystemMessage(Component.literal("You were removed from party by admin."));
        return 1;
    }

    private static UUID parseUuid(CommandSourceStack source, String uuidRaw) {
        try {
            return UUID.fromString(uuidRaw);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid uuid: " + uuidRaw));
            return null;
        }
    }

    private static boolean isPermissionAdmin(CommandSourceStack source) {
        return RpgPermissionService.hasOrOp(source, PermissionNodes.PERMISSION_ADMIN);
    }
}
