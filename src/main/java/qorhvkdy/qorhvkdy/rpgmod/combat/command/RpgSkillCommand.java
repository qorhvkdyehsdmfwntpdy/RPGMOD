package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.network.SkillTreeSyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.skill.runtime.SkillRuntimeService;
import qorhvkdy.qorhvkdy.rpgmod.skill.tree.SkillTreeService;

/**
 * 스킬 메커닉 테스트/운영 커맨드.
 */
public final class RpgSkillCommand {
    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (context, builder) -> {
        for (String id : SkillRuntimeService.listSkills()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> TREE_SUGGESTIONS = (context, builder) -> {
        for (String id : SkillTreeService.listNodeIds()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    };

    private RpgSkillCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgskill")
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("unlocked")
                        .executes(context -> unlocked(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("cast")
                        .then(Commands.argument("skillId", StringArgumentType.word())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> cast(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "skillId")
                                ))))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("trigger", StringArgumentType.word())
                                .executes(context -> trigger(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "trigger")
                                ))))
                .then(Commands.literal("reload")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("tree")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("nodeId", StringArgumentType.word())
                                        .suggests(TREE_SUGGESTIONS)
                                        .executes(context -> unlockTree(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "nodeId")
                                        ))))
                        .then(Commands.literal("reset")
                                .executes(context -> resetTree(context.getSource(), context.getSource().getPlayerOrException()))))
                .then(Commands.literal("admin")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .then(Commands.literal("reload")
                                .executes(context -> reload(context.getSource())))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("skillId", StringArgumentType.word())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(context -> adminGrant(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "skillId")
                                        ))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("skillId", StringArgumentType.word())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(context -> adminRevoke(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "skillId")
                                        ))))
                        .then(Commands.literal("tree")
                                .then(Commands.literal("grant")
                                        .then(Commands.argument("points", IntegerArgumentType.integer(1))
                                                .executes(context -> adminGrantTreePoints(
                                                        context.getSource(),
                                                        context.getSource().getPlayerOrException(),
                                                        IntegerArgumentType.getInteger(context, "points")
                                                ))))
                                .then(Commands.literal("reset")
                                        .executes(context -> resetTree(context.getSource(), context.getSource().getPlayerOrException()))))));
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Skills: " + SkillRuntimeService.listSkills()), false);
        return 1;
    }

    private static int unlocked(CommandSourceStack source, ServerPlayer player) {
        java.util.ArrayList<String> unlocked = new java.util.ArrayList<>();
        java.util.ArrayList<String> locked = new java.util.ArrayList<>();
        for (String skillId : SkillRuntimeService.listSkills()) {
            String node = "rpg.skill.use." + skillId.trim().toLowerCase();
            if (RpgPermissionService.has(player, node) || SkillTreeService.hasUnlockedSkill(player, skillId)) {
                unlocked.add(skillId);
            } else {
                locked.add(skillId);
            }
        }
        source.sendSuccess(() -> Component.literal("Unlocked skills(" + unlocked.size() + "): " + unlocked), false);
        source.sendSuccess(() -> Component.literal("Locked skills(" + locked.size() + "): " + locked), false);
        source.sendSuccess(() -> Component.literal(
                "Tree points=" + SkillTreeService.pointsOf(player)
                        + " | unlocked nodes=" + SkillTreeService.unlockedNodes(player)
        ), false);
        return 1;
    }

    private static int cast(CommandSourceStack source, ServerPlayer player, String skillId) {
        SkillRuntimeService.CastResult result = SkillRuntimeService.cast(player, skillId, "manual");
        if (!result.success()) {
            source.sendFailure(Component.literal("Skill cast failed: " + result.reason()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Skill cast success: " + skillId + " | actions=" + result.executedActions()), true);
        return 1;
    }

    private static int trigger(CommandSourceStack source, ServerPlayer player, String trigger) {
        SkillRuntimeService.TriggerResult result = SkillRuntimeService.trigger(player, trigger);
        source.sendSuccess(() -> Component.literal(
                "Skill trigger result: " + trigger + " | success=" + result.successCount() + " | failed=" + result.failedCount()
        ), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        SkillRuntimeService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded skill runtime/motion/passive/tree/chain JSON"), true);
        return 1;
    }

    private static int unlockTree(CommandSourceStack source, ServerPlayer player, String nodeId) {
        SkillTreeService.UnlockResult result = SkillTreeService.unlock(player, nodeId);
        if (!result.success()) {
            source.sendFailure(Component.literal("Tree unlock failed: " + result.reason()));
            return 0;
        }
        SkillTreeSyncRequestC2SPacket.sync(player);
        source.sendSuccess(() -> Component.literal("Tree unlocked: " + nodeId), true);
        return 1;
    }

    private static int resetTree(CommandSourceStack source, ServerPlayer player) {
        SkillTreeService.resetTree(player);
        SkillTreeSyncRequestC2SPacket.sync(player);
        source.sendSuccess(() -> Component.literal("Skill tree reset complete."), true);
        return 1;
    }

    private static int adminGrant(CommandSourceStack source, ServerPlayer target, String skillId) {
        boolean ok = SkillRuntimeService.grantSkillUse(target, skillId);
        if (!ok) {
            source.sendFailure(Component.literal("Skill grant failed: " + skillId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Granted skill permission: " + skillId), true);
        return 1;
    }

    private static int adminRevoke(CommandSourceStack source, ServerPlayer target, String skillId) {
        boolean ok = SkillRuntimeService.revokeSkillUse(target, skillId);
        if (!ok) {
            source.sendFailure(Component.literal("Skill revoke failed: " + skillId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Revoked skill permission: " + skillId), true);
        return 1;
    }

    private static int adminGrantTreePoints(CommandSourceStack source, ServerPlayer target, int points) {
        boolean ok = SkillTreeService.grantPoints(target, points);
        if (!ok) {
            source.sendFailure(Component.literal("Tree point grant failed"));
            return 0;
        }
        SkillTreeSyncRequestC2SPacket.sync(target);
        source.sendSuccess(() -> Component.literal(
                "Granted tree points +" + points + " | current=" + SkillTreeService.pointsOf(target)
        ), true);
        return 1;
    }
}
