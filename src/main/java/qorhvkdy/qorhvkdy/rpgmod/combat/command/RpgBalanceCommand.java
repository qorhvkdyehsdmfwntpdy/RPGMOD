package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.classes.balance.ClassBalanceCoefficientService;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.classes.synergy.ClassPartySynergyService;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.data.ClassResourceProfileRepository;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.ClassSetEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.sim.ClassBalanceSimulatorService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.data.ClassSkillRepository;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

/**
 * 밸런스 운영 명령.
 * 실서버 운영에서 수치 튜닝 -> reload -> 시뮬레이션 확인 루프를 빠르게 돌리기 위한 도구.
 */
public final class RpgBalanceCommand {
    private RpgBalanceCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgbalance")
                .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.BALANCE_ADMIN))
                .then(Commands.literal("reload")
                        .then(Commands.literal("class").executes(context -> reloadClass(context.getSource())))
                        .then(Commands.literal("all").executes(context -> reloadAll(context.getSource()))))
                .then(Commands.literal("undo")
                        .executes(context -> undo(context.getSource())))
                .then(Commands.literal("history")
                        .executes(context -> history(context.getSource())))
                .then(Commands.literal("class")
                        .then(Commands.literal("skill")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("skillId", StringArgumentType.word())
                                                .then(Commands.argument("field", StringArgumentType.word())
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                                .executes(context -> setSkillValue(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "skillId"),
                                                                        StringArgumentType.getString(context, "field"),
                                                                        DoubleArgumentType.getDouble(context, "value")
                                                                )))))))
                        .then(Commands.literal("resource")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("classId", StringArgumentType.word())
                                                .then(Commands.argument("field", StringArgumentType.word())
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                                .executes(context -> setResourceValue(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "classId"),
                                                                        StringArgumentType.getString(context, "field"),
                                                                        DoubleArgumentType.getDouble(context, "value")
                                                                ))))))))
                .then(Commands.literal("simulate")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> simulate(context.getSource(), EntityArgument.getPlayer(context, "target"), 30))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 300))
                                        .executes(context -> simulate(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "seconds")
                                        ))))));
    }

    private static int reloadClass(CommandSourceStack source) {
        ClassBalanceCoefficientService.reload();
        ClassPartySynergyService.reload();
        ClassResourceService.reload();
        ClassSkillService.reload();
        ClassSetEffectService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded class balance data."), true);
        return 1;
    }

    private static int reloadAll(CommandSourceStack source) {
        return reloadClass(source);
    }

    private static int setSkillValue(CommandSourceStack source, String skillId, String field, double value) {
        Double before = ClassSkillRepository.getSkillNumeric(skillId, field);
        BalanceMutationHistoryService.pushSnapshot("setSkillValue");
        if (!ClassSkillRepository.setSkillNumeric(skillId, field, value)) {
            source.sendFailure(Component.literal("Failed to set skill value. check skillId/field."));
            return 0;
        }
        Double after = ClassSkillRepository.getSkillNumeric(skillId, field);
        ClassSkillService.reload();
        RpgAuditLogService.progression("balance_skill_set actor=" + source.getTextName()
                + ", skill=" + skillId + ", field=" + field
                + ", before=" + round3(before == null ? 0.0 : before)
                + ", after=" + round3(after == null ? 0.0 : after));
        source.sendSuccess(() -> Component.literal(
                "Skill updated: " + skillId + " " + field + " " + round3(before == null ? 0.0 : before) + " -> " + round3(after == null ? 0.0 : after)
        ), true);
        return 1;
    }

    private static int setResourceValue(CommandSourceStack source, String classId, String field, double value) {
        Double before = ClassResourceProfileRepository.getProfileNumeric(classId, field);
        BalanceMutationHistoryService.pushSnapshot("setResourceValue");
        if (!ClassResourceProfileRepository.setProfileNumeric(classId, field, value)) {
            source.sendFailure(Component.literal("Failed to set resource value. check classId/field."));
            return 0;
        }
        Double after = ClassResourceProfileRepository.getProfileNumeric(classId, field);
        ClassResourceService.reload();
        RpgAuditLogService.progression("balance_resource_set actor=" + source.getTextName()
                + ", class=" + classId + ", field=" + field
                + ", before=" + round3(before == null ? 0.0 : before)
                + ", after=" + round3(after == null ? 0.0 : after));
        source.sendSuccess(() -> Component.literal(
                "Resource updated: " + classId + " " + field + " " + round3(before == null ? 0.0 : before) + " -> " + round3(after == null ? 0.0 : after)
        ), true);
        return 1;
    }

    private static int undo(CommandSourceStack source) {
        if (!BalanceMutationHistoryService.undo()) {
            source.sendFailure(Component.literal("No balance history to undo."));
            return 0;
        }
        ClassSkillService.reload();
        ClassResourceService.reload();
        source.sendSuccess(() -> Component.literal("Balance undo complete. Remaining history=" + BalanceMutationHistoryService.size()), true);
        return 1;
    }

    private static int history(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Balance history entries=" + BalanceMutationHistoryService.size()), false);
        return 1;
    }

    private static int simulate(CommandSourceStack source, ServerPlayer target, int seconds) {
        ClassBalanceSimulatorService.SimulationResult result = ClassBalanceSimulatorService.simulate(target, seconds);
        source.sendSuccess(() -> Component.literal("=== Balance Simulation: " + target.getName().getString() + " ==="), false);
        source.sendSuccess(() -> Component.literal("duration=" + result.durationSeconds() + "s"), false);
        source.sendSuccess(() -> Component.literal("baselineDps=" + round3(result.baselineDps())), false);
        source.sendSuccess(() -> Component.literal("estimatedSkillDps=" + round3(result.estimatedSkillDps())), false);
        source.sendSuccess(() -> Component.literal("effectiveHpScore=" + round3(result.effectiveHpScore())), false);
        source.sendSuccess(() -> Component.literal("bestSkill=" + result.bestSkillId()), false);
        return 1;
    }

    private static String round3(double value) {
        return String.valueOf(Math.round(value * 1000.0) / 1000.0);
    }
}
