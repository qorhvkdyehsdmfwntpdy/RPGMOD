package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.classes.sim.ClassBalanceSimulatorService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

/**
 * 클래스 확장 기능 명령어.
 */
public final class ClassEnhancementCommand {
    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            PlayerStats stats = StatsUtil.get(player);
            for (var skill : ClassSkillService.unlocked(player, stats)) {
                builder.suggest(skill.id());
            }
        }
        return builder.buildFuture();
    };

    private ClassEnhancementCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("classutil")
                .executes(context -> showResource(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("resource")
                        .executes(context -> showResource(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.literal("sync")
                                .executes(context -> syncResource(context.getSource(), context.getSource().getPlayerOrException(), false)))
                        .then(Commands.literal("refill")
                                .executes(context -> refillResource(context.getSource(), context.getSource().getPlayerOrException()))))
                .then(Commands.literal("skill")
                        .then(Commands.literal("list")
                                .executes(context -> listSkills(context.getSource(), context.getSource().getPlayerOrException())))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("skillId", StringArgumentType.word())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(context -> castSkill(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "skillId")
                                        )))))
                .then(Commands.literal("simulate")
                        .executes(context -> simulate(context.getSource(), context.getSource().getPlayerOrException(), 30))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 300))
                                .executes(context -> simulate(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "seconds")
                                ))))
                .then(Commands.literal("admin")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .then(Commands.literal("simulate")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> simulate(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                30
                                        ))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 300))
                                                .executes(context -> simulate(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "seconds")
                                                )))))
                        .then(Commands.literal("resource")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.literal("sync")
                                                .executes(context -> syncResource(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        false
                                                )))
                                        .then(Commands.literal("refill")
                                                .executes(context -> refillResource(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target")
                                                )))))));
    }

    private static int showResource(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        source.sendSuccess(() -> Component.literal(
                target.getName().getString()
                        + " | resource=" + stats.getClassResourceType()
                        + " " + round2(stats.getClassResourceCurrent()) + "/" + round2(stats.getClassResourceMax())
        ), false);
        return 1;
    }

    private static int syncResource(CommandSourceStack source, ServerPlayer target, boolean refill) {
        PlayerStats stats = StatsUtil.get(target);
        ClassResourceService.syncProfile(target, stats, refill);
        source.sendSuccess(() -> Component.literal("Resource synced: " + target.getName().getString()), true);
        return 1;
    }

    private static int refillResource(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        stats.refillClassResource();
        ClassResourceService.syncProfile(target, stats, false);
        source.sendSuccess(() -> Component.literal("Resource refilled: " + target.getName().getString()), true);
        return 1;
    }

    private static int listSkills(CommandSourceStack source, ServerPlayer player) {
        PlayerStats stats = StatsUtil.get(player);
        var unlocked = ClassSkillService.unlocked(player, stats);
        source.sendSuccess(() -> Component.literal("Unlocked skills: " + unlocked.size()), false);
        for (var skill : unlocked) {
            double remain = ClassSkillService.remainingCooldownSeconds(player, skill.id());
            source.sendSuccess(() -> Component.literal(
                    "- " + skill.id()
                            + " | cost=" + round2(skill.resourceCost())
                            + " | cd=" + round2(skill.cooldownMs() / 1000.0)
                            + " | remain=" + round2(remain)
            ), false);
        }
        return unlocked.size();
    }

    private static int castSkill(CommandSourceStack source, ServerPlayer player, String skillId) {
        PlayerStats stats = StatsUtil.get(player);
        ClassSkillService.CastResult result = ClassSkillService.cast(player, stats, skillId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int simulate(CommandSourceStack source, ServerPlayer target, int seconds) {
        ClassBalanceSimulatorService.SimulationResult result = ClassBalanceSimulatorService.simulate(target, seconds);
        source.sendSuccess(() -> Component.literal("=== Class Simulation: " + target.getName().getString() + " ==="), false);
        source.sendSuccess(() -> Component.literal("duration=" + result.durationSeconds() + "s"), false);
        source.sendSuccess(() -> Component.literal("baselineDps=" + round2(result.baselineDps())), false);
        source.sendSuccess(() -> Component.literal("estimatedSkillDps=" + round2(result.estimatedSkillDps())), false);
        source.sendSuccess(() -> Component.literal("effectiveHpScore=" + round2(result.effectiveHpScore())), false);
        source.sendSuccess(() -> Component.literal("critFactor=" + round2(result.critFactor())), false);
        source.sendSuccess(() -> Component.literal("bestSkill=" + result.bestSkillId()), false);
        return 1;
    }

    private static String round2(double value) {
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }
}
