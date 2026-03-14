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
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.PlayerProficiency;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencySourceService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyUtil;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyBlockRuleService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBalanceRepository;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyRewardRepository;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencySourceRepository;

/**
 * 숙련도 운영/디버그 명령어.
 * 목표: 밸런싱 테스트 속도를 높이기 위한 단순 조작 명령 제공.
 */
public final class ProficiencyCommand {
    private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGESTIONS = (context, builder) -> {
        for (ProficiencyType type : ProficiencyType.values()) {
            builder.suggest(type.key());
        }
        return builder.buildFuture();
    };

    private ProficiencyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("proficiency")
                .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("show")
                        .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException())))
                .then(adminBranch()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminBranch() {
        return Commands.literal("admin")
                .requires(ProficiencyCommand::isAdmin)
                .then(Commands.literal("show")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> show(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPE_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> add(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "type"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))))))
                .then(Commands.literal("grant_source")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("sourceId", StringArgumentType.word())
                                        .executes(context -> grantSource(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "sourceId")
                                        )))))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPE_SUGGESTIONS)
                                        .then(Commands.argument("exp", IntegerArgumentType.integer(0))
                                                .executes(context -> set(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "type"),
                                                        IntegerArgumentType.getInteger(context, "exp")
                                                ))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> reset(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())));
    }

    private static int show(CommandSourceStack source, ServerPlayer target) {
        PlayerProficiency data = ProficiencyUtil.get(target);
        source.sendSuccess(() -> Component.literal("=== Proficiency: " + target.getName().getString() + " ==="), false);
        for (ProficiencyType type : ProficiencyType.values()) {
            int level = data.getLevel(type);
            int into = data.getExpIntoCurrentLevel(type);
            int next = data.getExpForNextLevel(type);
            int exp = data.getExp(type);
            source.sendSuccess(() -> Component.literal(
                    type.key() + " | Lv." + level + " | exp=" + exp + " | " + into + "/" + next
            ), false);
        }
        return 1;
    }

    private static int add(CommandSourceStack source, ServerPlayer target, String typeRaw, int amount) {
        ProficiencyType type = parseType(typeRaw);
        if (type == null) {
            source.sendFailure(Component.literal("Invalid proficiency type."));
            return 0;
        }
        PlayerProficiency data = ProficiencyUtil.get(target);
        data.addExp(type, amount);
        ModNetwork.syncProficiencyToPlayer(target, data);
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " " + type.key() + " +" + amount), false);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer target, String typeRaw, int exp) {
        ProficiencyType type = parseType(typeRaw);
        if (type == null) {
            source.sendFailure(Component.literal("Invalid proficiency type."));
            return 0;
        }
        PlayerProficiency data = ProficiencyUtil.get(target);
        data.setExp(type, exp);
        ModNetwork.syncProficiencyToPlayer(target, data);
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " " + type.key() + " exp=" + exp), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer target) {
        PlayerProficiency data = ProficiencyUtil.get(target);
        for (ProficiencyType type : ProficiencyType.values()) {
            data.setExp(type, 0);
        }
        ModNetwork.syncProficiencyToPlayer(target, data);
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " proficiency reset"), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        ProficiencyBalanceRepository.reload();
        ProficiencyRewardRepository.reload();
        ProficiencySourceRepository.reload();
        ProficiencyBlockRuleService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded proficiency configs."), true);
        return 1;
    }

    private static int grantSource(CommandSourceStack source, ServerPlayer target, String sourceId) {
        int gained = ProficiencySourceService.grantBySource(target, sourceId, 1.0);
        if (gained <= 0) {
            source.sendFailure(Component.literal("Failed to grant source proficiency: " + sourceId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " source " + sourceId + " applied=" + gained), false);
        return 1;
    }

    private static ProficiencyType parseType(String raw) {
        return ProficiencyType.fromKey(raw).orElse(null);
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return RpgPermissionService.hasOrOp(source, PermissionNodes.PROFICIENCY_ADMIN);
    }
}
