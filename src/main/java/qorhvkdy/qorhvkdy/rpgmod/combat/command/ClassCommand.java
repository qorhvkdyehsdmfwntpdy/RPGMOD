package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancement;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancementRegistry;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassOperationResult;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveTemplateService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassProfile;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassProfileRegistry;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassProgressionService;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.ClassSetEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.passive.StatPassiveSkillService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBalanceRepository;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyRewardRepository;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyBlockRuleService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyCapability;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsAttributeService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDataService;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDropService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class + promotion command.
 *
 * Flow:
 * 1) /class set <base>              : pick base class
 * 2) /class options                 : check unlocked/locked promotion choices
 * 3) /class promote <advancementId> : execute promotion when level condition is met
 */
public final class ClassCommand {
    private static final long RESET_CONFIRM_WINDOW_MS = 15_000L;
    private static final Map<UUID, PendingReset> PENDING_RESETS = new ConcurrentHashMap<>();

    private record PendingReset(UUID requesterId, boolean keepLevel, boolean keepProficiency, long expiresAt) {
    }

    private static final SuggestionProvider<CommandSourceStack> BASE_CLASS_SUGGESTIONS = (context, builder) -> {
        for (PlayerClassType value : PlayerClassType.values()) {
            if (value != PlayerClassType.NONE) {
                builder.suggest(value.id());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> ADVANCEMENT_ID_SUGGESTIONS = (context, builder) -> {
        for (String id : ClassAdvancementRegistry.ids()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    };

    private ClassCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("class")
                .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("show")
                        .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("list")
                        .executes(context -> listBaseClasses(context.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("base", StringArgumentType.word())
                                .suggests(BASE_CLASS_SUGGESTIONS)
                                .executes(context -> setBaseClass(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "base")
                                ))))
                .then(Commands.literal("options")
                        .executes(context -> showPromotionOptions(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("promote")
                        .then(Commands.argument("advancementId", StringArgumentType.word())
                                .suggests(ADVANCEMENT_ID_SUGGESTIONS)
                                .executes(context -> promote(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "advancementId")
                                ))))
                .then(Commands.literal("reset")
                        .executes(context -> requestReset(context.getSource(), context.getSource().getPlayerOrException(), false, false))
                        .then(Commands.literal("keep_level")
                                .executes(context -> requestReset(context.getSource(), context.getSource().getPlayerOrException(), true, false)))
                        .then(Commands.literal("keep_proficiency")
                                .executes(context -> requestReset(context.getSource(), context.getSource().getPlayerOrException(), false, true)))
                        .then(Commands.literal("keep_all")
                                .executes(context -> requestReset(context.getSource(), context.getSource().getPlayerOrException(), true, true)))
                        .then(Commands.literal("confirm")
                                .executes(context -> confirmReset(context.getSource(), context.getSource().getPlayerOrException()))))
                .then(adminBranch()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminBranch() {
        return Commands.literal("admin")
                .requires(ClassCommand::isAdmin)
                .then(Commands.literal("show")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> show(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("base", StringArgumentType.word())
                                        .suggests(BASE_CLASS_SUGGESTIONS)
                                        .executes(context -> setBaseClass(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "base")
                                        )))))
                .then(Commands.literal("options")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showPromotionOptions(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("promote")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("advancementId", StringArgumentType.word())
                                        .suggests(ADVANCEMENT_ID_SUGGESTIONS)
                                        .executes(context -> promote(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "advancementId")
                                        )))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> requestReset(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        false,
                                        false
                                ))
                                .then(Commands.literal("keep_level")
                                        .executes(context -> requestReset(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                true,
                                                false
                                        )))
                                .then(Commands.literal("keep_proficiency")
                                        .executes(context -> requestReset(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                false,
                                                true
                                        )))
                                .then(Commands.literal("keep_all")
                                        .executes(context -> requestReset(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                true,
                                                true
                                        )))
                                .then(Commands.literal("confirm")
                                        .executes(context -> confirmReset(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target")
                                        )))))
                .then(Commands.literal("reload")
                        .executes(context -> reloadAdvancementConfig(context.getSource())));
    }

    private static int listBaseClasses(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.list.header"), false);
        for (PlayerClassType type : PlayerClassType.values()) {
            if (type == PlayerClassType.NONE) {
                continue;
            }
            ClassProfile profile = ClassProfileRegistry.get(type);
            ClassAdvancement root = ClassAdvancementRegistry.defaultBaseAdvancement(type);
            source.sendSuccess(() -> Component.translatable(
                    "command.rpgmod.class.list.entry",
                    type.displayNameComponent(),
                    profile.summary(),
                    root.id()
            ), false);
        }
        return 1;
    }

    private static int show(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        ClassProfile profile = stats.getClassProfile();
        ClassAdvancement current = stats.getCurrentAdvancement();
        int level = target.experienceLevel;
        boolean self = source.getPlayer() != null && source.getPlayer().getUUID().equals(target.getUUID());
        String prefix = self ? "" : target.getName().getString() + " | ";

        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.show.base", prefix, profile.type().displayNameComponent(), profile.type().id()), false);
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.show.current", prefix, current.displayNameComponent(), current.id()), false);
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.show.tier", prefix, current.tier().name(), current.requiredLevel(), level), false);
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.show.summary", prefix, current.summary()), false);
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.show.weights", prefix, current.statWeights()), false);
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.show.requirements", prefix, current.requirements()), false);
        source.sendSuccess(() -> Component.literal(prefix + "Passive slots: " + stats.getPassiveSlots()), false);
        source.sendSuccess(() -> Component.literal(prefix + "Stat passives: " + StatPassiveSkillService.unlockedSkillIds(stats)), false);
        PassiveBonus classBonus = ClassPassiveEffectService.compute(stats);
        PassiveBonus statBonus = StatPassiveSkillService.compute(stats);
        PassiveBonus total = classBonus.combine(statBonus);
        source.sendSuccess(() -> Component.literal(prefix + "Passive bonus total: " + formatPassiveBonus(total)), false);
        return 1;
    }

    private static int setBaseClass(CommandSourceStack source, ServerPlayer target, String baseClassId) {
        PlayerClassType type = parseType(baseClassId);
        if (type == null || type == PlayerClassType.NONE) {
            source.sendFailure(Component.translatable("command.rpgmod.class.error.invalid_base"));
            return 0;
        }

        ServerPlayer actor = source.getPlayer() == null ? target : source.getPlayer();
        ClassOperationResult result = ClassProgressionService.setBaseClass(actor, target, type);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.set.success", target.getName().getString(), type.displayNameComponent()), false);
        if (source.getPlayer() == null || !source.getPlayer().getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal(result.message()));
        }
        return 1;
    }

    private static int showPromotionOptions(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        ClassAdvancement current = stats.getCurrentAdvancement();
        int level = target.experienceLevel;

        List<ClassAdvancement> unlocked = ClassAdvancementRegistry.nextOptions(current.id(), level);
        List<ClassAdvancement> locked = ClassAdvancementRegistry.lockedNextOptions(current.id(), level);

        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.options.header", target.getName().getString()), false);
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.options.current", current.displayNameComponent(), current.id(), level), false);

        if (unlocked.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.rpgmod.class.options.unlocked.none"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.rpgmod.class.options.unlocked"), false);
            for (ClassAdvancement node : unlocked) {
                source.sendSuccess(() -> Component.translatable(
                        "command.rpgmod.class.options.entry",
                        node.id(),
                        node.displayNameComponent(),
                        node.requiredLevel()
                ), false);
            }
        }

        if (!locked.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.rpgmod.class.options.locked"), false);
            for (ClassAdvancement node : locked) {
                source.sendSuccess(() -> Component.translatable(
                        "command.rpgmod.class.options.entry",
                        node.id(),
                        node.displayNameComponent(),
                        node.requiredLevel()
                ), false);
            }
        }
        return 1;
    }

    private static int promote(CommandSourceStack source, ServerPlayer target, String targetAdvancementId) {
        PlayerStats stats = StatsUtil.get(target);
        ClassAdvancement before = stats.getCurrentAdvancement();

        ServerPlayer actor = source.getPlayer() == null ? target : source.getPlayer();
        ClassOperationResult result = ClassProgressionService.promote(actor, target, targetAdvancementId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            for (String detail : result.details()) {
                source.sendFailure(Component.translatable("command.rpgmod.class.promote.detail", detail));
            }
            return 0;
        }

        source.sendSuccess(() -> Component.translatable(
                "command.rpgmod.class.promote.success",
                target.getName().getString(),
                before.displayNameComponent(),
                stats.getCurrentAdvancement().displayNameComponent()
        ), false);
        if (source.getPlayer() == null || !source.getPlayer().getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal(result.message()));
        }
        return 1;
    }

    private static PlayerClassType parseType(String input) {
        return PlayerClassType.fromId(input).orElse(null);
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN);
    }

    private static int reloadAdvancementConfig(CommandSourceStack source) {
        ClassAdvancementRegistry.reload();
        ClassPassiveTemplateService.reload();
        ClassPassiveEffectService.reload();
        ClassResourceService.reload();
        ClassSkillService.reload();
        ClassSetEffectService.reload();
        StatPassiveSkillService.reload();
        WeaponDataService.reload();
        WeaponDropService.reload();
        RpgPermissionService.reload();
        RpgDebugSettings.reload();
        PartyService.reload();
        ProficiencyBalanceRepository.reload();
        ProficiencyRewardRepository.reload();
        ProficiencyBlockRuleService.reload();
        source.sendSuccess(() -> Component.translatable("command.rpgmod.class.reload.success"), true);
        return 1;
    }

    private static int requestReset(CommandSourceStack source, ServerPlayer target, boolean keepLevel, boolean keepProficiency) {
        UUID requesterId = source.getPlayer() == null ? null : source.getPlayer().getUUID();
        long expiresAt = System.currentTimeMillis() + RESET_CONFIRM_WINDOW_MS;
        PENDING_RESETS.put(target.getUUID(), new PendingReset(requesterId, keepLevel, keepProficiency, expiresAt));
        String mode = keepLevel ? "keep_level" : (keepProficiency ? "keep_proficiency" : "");
        String confirmHint = buildConfirmHint(source, target, mode);
        source.sendSuccess(() -> Component.literal(
                "Reset pending for " + target.getName().getString()
                        + ". Run " + confirmHint + " within 15s."
        ), false);
        return 1;
    }

    private static int confirmReset(CommandSourceStack source, ServerPlayer target) {
        PendingReset pending = PENDING_RESETS.get(target.getUUID());
        if (pending == null || System.currentTimeMillis() > pending.expiresAt()) {
            PENDING_RESETS.remove(target.getUUID());
            source.sendFailure(Component.literal("No pending reset or reset confirmation expired."));
            return 0;
        }

        Player requester = source.getPlayer();
        if (pending.requesterId() != null && (requester == null || !pending.requesterId().equals(requester.getUUID()))) {
            source.sendFailure(Component.literal("Only the original requester can confirm this reset."));
            return 0;
        }
        PENDING_RESETS.remove(target.getUUID());
        return resetProgression(source, target, pending.keepLevel(), pending.keepProficiency());
    }

    private static int resetProgression(CommandSourceStack source, ServerPlayer target, boolean keepLevel, boolean keepProficiency) {
        PlayerStats stats = StatsUtil.get(target);

        // 한글 주석: 전직 초기화 시 직업/전직 상태를 기본 상태로 되돌린다.
        stats.setSelectedClass(PlayerClassType.NONE);
        stats.setCurrentAdvancementId("novice_base");
        stats.setLastTrackedLevel(keepLevel ? Math.max(0, target.experienceLevel) : 0);

        if (!keepLevel) {
            target.experienceLevel = 0;
            target.totalExperience = 0;
            target.experienceProgress = 0.0F;
            target.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));
        }

        // 한글 주석: 초기화 커맨드에서는 숙련도도 0으로 되돌려 신규 캐릭터 상태를 보장한다.
        if (!keepProficiency) {
            target.getCapability(ProficiencyCapability.PLAYER_PROFICIENCY).ifPresent(data -> {
                for (ProficiencyType type : ProficiencyType.values()) {
                    data.setExp(type, 0);
                }
                qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork.syncProficiencyToPlayer(target, data);
            });
        }

        StatsAttributeService.apply(target, stats);
        qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork.syncToPlayer(target, stats);

        source.sendSuccess(() -> Component.literal(
                "Class progression reset complete for " + target.getName().getString()
                        + " (keepLevel=" + keepLevel + ", keepProficiency=" + keepProficiency + ")"
        ), true);
        RpgAuditLogService.progression("reset actor=" + source.getTextName()
                + ", target=" + target.getName().getString()
                + ", keepLevel=" + keepLevel
                + ", keepProficiency=" + keepProficiency);
        return 1;
    }

    private static String formatPassiveBonus(PassiveBonus bonus) {
        return "hpX" + round3(bonus.hpMultiplier())
                + ", atkX" + round3(bonus.attackDamageMultiplier())
                + ", move+" + round3(bonus.moveSpeedBonusPercent()) + "%"
                + ", armor+" + round3(bonus.armorBonus())
                + ", critChance+" + round3(bonus.critChanceBonusPercent()) + "%"
                + ", critDamage+" + round3(bonus.critDamageBonusPercent()) + "%";
    }

    private static String round3(double value) {
        return String.valueOf(Math.round(value * 1000.0) / 1000.0);
    }

    private static String buildConfirmHint(CommandSourceStack source, ServerPlayer target, String mode) {
        ServerPlayer actor = source.getPlayer();
        boolean selfTarget = actor != null && actor.getUUID().equals(target.getUUID());
        String mid = mode.isBlank() ? "" : (" " + mode);
        if (selfTarget) {
            return "/class reset" + mid + " confirm";
        }
        return "/class admin reset " + target.getName().getString() + mid + " confirm";
    }
}
