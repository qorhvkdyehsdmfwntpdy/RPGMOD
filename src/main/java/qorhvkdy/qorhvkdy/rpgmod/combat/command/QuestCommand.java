package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.quest.QuestService;

/**
 * 퀘스트 운영/테스트 커맨드.
 */
public final class QuestCommand {
    private QuestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgquest")
                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("accept")
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(context -> accept(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "questId")
                                ))))
                .then(Commands.literal("complete")
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(context -> complete(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "questId")
                                ))))
                .then(Commands.literal("reload")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("admin")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .executes(context -> adminComplete(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "questId"),
                                                        true
                                                )))))
                        .then(Commands.literal("complete_no_reward")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .executes(context -> adminComplete(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "questId"),
                                                        false
                                                )))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .executes(context -> adminReset(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "questId")
                                                )))))
                        .then(Commands.literal("resetcooldown")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .executes(context -> adminResetCooldown(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "questId")
                                                )))))
                        .then(Commands.literal("resetall")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> adminResetAll(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target")
                                        ))))
                        .then(Commands.literal("sync")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> adminSync(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target")
                                        ))))));
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        var available = QuestService.listAvailable(player);
        sync(player);
        source.sendSuccess(() -> Component.literal("Available quests: " + available), false);
        return 1;
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        sync(player);
        source.sendSuccess(() -> Component.literal("Accepted quests: " + QuestService.acceptedList(player)), false);
        source.sendSuccess(() -> Component.literal("Accepted progress: " + QuestService.acceptedProgressSummaries(player)), false);
        source.sendSuccess(() -> Component.literal("Objectives: " + QuestService.acceptedObjectiveLines(player, 12)), false);
        source.sendSuccess(() -> Component.literal("Completed quests: " + QuestService.completedList(player)), false);
        return 1;
    }

    private static int accept(CommandSourceStack source, ServerPlayer player, String questId) {
        QuestService.CheckResult check = QuestService.canAccept(player, questId);
        if (!check.passed()) {
            source.sendFailure(Component.literal("Cannot accept quest: " + check.reason()));
            return 0;
        }
        if (!QuestService.accept(player, questId)) {
            source.sendFailure(Component.literal("Failed to accept quest."));
            return 0;
        }
        sync(player);
        source.sendSuccess(() -> Component.literal("Quest accepted: " + questId), true);
        return 1;
    }

    private static int complete(CommandSourceStack source, ServerPlayer player, String questId) {
        if (!QuestService.complete(player, questId)) {
            source.sendFailure(Component.literal("Cannot complete quest: not accepted or invalid."));
            return 0;
        }
        sync(player);
        source.sendSuccess(() -> Component.literal("Quest completed: " + questId), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        QuestService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded quests.json"), true);
        return 1;
    }

    private static int adminComplete(CommandSourceStack source, ServerPlayer target, String questId, boolean grantRewards) {
        if (!QuestService.adminComplete(target, questId, grantRewards)) {
            source.sendFailure(Component.literal("Admin complete failed. unknown quest or invalid state: " + questId));
            return 0;
        }
        sync(target);
        source.sendSuccess(() -> Component.literal("Admin complete success: target=" + target.getName().getString()
                + ", quest=" + questId + ", rewards=" + grantRewards), true);
        return 1;
    }

    private static int adminReset(CommandSourceStack source, ServerPlayer target, String questId) {
        if (!QuestService.adminResetQuest(target, questId)) {
            source.sendFailure(Component.literal("Admin reset failed. unknown quest: " + questId));
            return 0;
        }
        sync(target);
        source.sendSuccess(() -> Component.literal("Admin reset success: target=" + target.getName().getString()
                + ", quest=" + questId), true);
        return 1;
    }

    private static int adminResetCooldown(CommandSourceStack source, ServerPlayer target, String questId) {
        if (!QuestService.adminResetCooldown(target, questId)) {
            source.sendFailure(Component.literal("Admin resetcooldown failed. unknown quest: " + questId));
            return 0;
        }
        sync(target);
        source.sendSuccess(() -> Component.literal("Admin cooldown reset success: target=" + target.getName().getString()
                + ", quest=" + questId), true);
        return 1;
    }

    private static int adminResetAll(CommandSourceStack source, ServerPlayer target) {
        int resetCount = QuestService.adminResetAll(target);
        sync(target);
        source.sendSuccess(() -> Component.literal("Admin resetall success: target=" + target.getName().getString()
                + ", count=" + resetCount), true);
        return 1;
    }

    private static int adminSync(CommandSourceStack source, ServerPlayer target) {
        sync(target);
        source.sendSuccess(() -> Component.literal("Admin sync success: target=" + target.getName().getString()), false);
        return 1;
    }

    private static void sync(ServerPlayer player) {
        QuestService.sync(player);
    }
}
