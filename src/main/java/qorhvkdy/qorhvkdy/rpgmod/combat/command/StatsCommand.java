package qorhvkdy.qorhvkdy.rpgmod.combat.command;

/*
 * [RPGMOD 파일 설명]
 * 역할: 플레이어/관리자용 /stats 명령어 트리와 실행 로직을 제공합니다.
 * 수정 예시: /stats admin 하위 기능을 늘리려면 adminBranch 체인에 literal을 추가합니다.
 */

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsAdminLockService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsAttributeService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsHistoryService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsSnapshotService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

import java.util.List;
import java.util.Locale;

public final class StatsCommand {
    private static final SimpleCommandExceptionType INVALID_STAT =
            new SimpleCommandExceptionType(Component.literal("Invalid stat. Use: str, agi, wis, luk"));

    private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (context, builder) -> {
        for (StatType type : StatType.values()) {
            builder.suggest(type.key());
        }
        return builder.buildFuture();
    };

    private StatsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stats")
                .executes(context -> showStats(context.getSource().getPlayerOrException()))
                .then(Commands.literal("show")
                        .executes(context -> showStats(context.getSource().getPlayerOrException())))
                .then(Commands.literal("set")
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests(STAT_SUGGESTIONS)
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            StatType stat = parseStat(StringArgumentType.getString(context, "stat"));
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            return setStatWithPoints(player, stat, value);
                                        }))))
                .then(Commands.literal("add")
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests(STAT_SUGGESTIONS)
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            StatType stat = parseStat(StringArgumentType.getString(context, "stat"));
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            return addStatWithPoints(player, stat, amount);
                                        }))))
                .then(Commands.literal("reset")
                        .executes(context -> resetStats(context.getSource().getPlayerOrException())))
                .then(adminBranch()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminBranch() {
        return Commands.literal("admin")
                .requires(StatsCommand::isAdmin)

                // 관리자: 특정 플레이어의 스탯 값을 강제로 설정
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests(STAT_SUGGESTIONS)
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(context -> adminSetStat(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        parseStat(StringArgumentType.getString(context, "stat")),
                                                        IntegerArgumentType.getInteger(context, "value")
                                                ))))))

                // 관리자: 스탯 포인트를 증감
                .then(Commands.literal("points")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> adminAddPoints(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "amount")
                                        )))))

                // 관리자: 스탯 포인트를 절대값으로 설정
                .then(Commands.literal("setpoints")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> adminSetPoints(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "value")
                                        )))))

                // 관리자: 대상 플레이어 경험치를 완전 초기화(레벨/총경험치/진행도 = 0)
                .then(Commands.literal("resetxp")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> adminResetXp(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))

                // 관리자: 대상 플레이어 사용 가능 스탯 포인트를 0으로 초기화
                .then(Commands.literal("resetpoints")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> adminResetPoints(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))

                // 관리자: 대상 플레이어 스탯 변경 잠금 on/off/status
                .then(Commands.literal("lock")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("on")
                                        .executes(context -> adminSetLock(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                true
                                        )))
                                .then(Commands.literal("off")
                                        .executes(context -> adminSetLock(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                false
                                        )))
                                .then(Commands.literal("status")
                                        .executes(context -> adminLockStatus(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target")
                                        )))))

                // 관리자: 대상 플레이어 수치를 강제로 재계산/재동기화
                .then(Commands.literal("recalc")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> adminRecalc(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))

                // 관리자: 현재 상태를 스냅샷으로 저장
                .then(Commands.literal("snapshot")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> adminSnapshot(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        "manual"
                                ))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> adminSnapshot(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "reason")
                                        )))))

                // 관리자: 마지막 스냅샷으로 롤백
                .then(Commands.literal("rollback")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> adminRollback(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))

                // 관리자: 스탯 변경 히스토리 조회
                .then(Commands.literal("history")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showHistory(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        20
                                ))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> showHistory(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "limit")
                                        )))));
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return RpgPermissionService.hasOrOp(source, PermissionNodes.STATS_ADMIN);
    }

    private static int showStats(ServerPlayer player) {
        PlayerStats stats = StatsUtil.get(player);
        StringBuilder builder = new StringBuilder("=== Stats ===\n");
        for (StatType type : StatType.values()) {
            builder.append(type.key()).append(": ").append(stats.get(type)).append("\n");
        }
        builder.append("points: ").append(stats.getAvailableStatPoints()).append("\n")
                .append("move_speed: ").append(round2(stats.getMoveSpeedPercent())).append("%\n")
                .append("attack_speed: ").append(round2(stats.getAttackSpeedPercent())).append("%\n")
                .append("attack_power: ").append(round2(stats.getAttackPower())).append("\n")
                .append("magic_power: ").append(round2(stats.getMagicPower())).append("\n")
                .append("max_hp: ").append(stats.getMaxHP(player.experienceLevel)).append("\n")
                .append("max_mp: ").append(stats.getMaxMP(player.experienceLevel)).append("\n")
                .append("defense: ").append(round2(stats.getDefense())).append("\n")
                .append("hp_regen: ").append(round2(stats.getHpRegenPercent())).append("%\n")
                .append("crit_chance: ").append(round2(stats.getCritChance())).append("%\n")
                .append("crit_damage: ").append(round2(stats.getCritDamage())).append("%\n")
                .append("class_resource: ").append(stats.getClassResourceType()).append(" ")
                .append(round2(stats.getClassResourceCurrent())).append("/").append(round2(stats.getClassResourceMax())).append("\n")
                .append("locked: ").append(StatsAdminLockService.isLocked(player));

        player.sendSystemMessage(Component.literal(builder.toString()));
        return 1;
    }

    private static int setStatWithPoints(ServerPlayer player, StatType stat, int value) {
        if (isLockedForPlayerChange(player)) {
            return 0;
        }

        PlayerStats stats = StatsUtil.get(player);
        StatsSnapshotService.snapshot(player, stats, "before /stats set " + stat.key());

        int target = Math.max(0, value);
        int current = stats.get(stat);

        if (target > current) {
            int need = target - current;
            if (stats.getAvailableStatPoints() < need) {
                player.sendSystemMessage(Component.literal("Not enough points. Need " + need + ", have " + stats.getAvailableStatPoints()));
                return 0;
            }
            int applied = stats.increaseStatMany(stat, need);
            if (applied != need) {
                player.sendSystemMessage(Component.literal("Could not apply full amount due to cap. Applied " + applied + "/" + need));
            }
        } else if (target < current) {
            int decrease = current - target;
            int reduced = stats.decreaseStatMany(stat, decrease);
            if (reduced != decrease) {
                player.sendSystemMessage(Component.literal("Could not decrease full amount. Applied " + reduced + "/" + decrease));
            }
        }

        applyAndSync(player, stats);
        StatsHistoryService.log(player, "Command set: " + stat.key() + " -> " + stats.get(stat) + ", points=" + stats.getAvailableStatPoints());
        player.sendSystemMessage(Component.literal(stat.key() + " set to " + stats.get(stat) + " | points " + stats.getAvailableStatPoints()));
        return 1;
    }

    private static int addStatWithPoints(ServerPlayer player, StatType stat, int amount) {
        if (isLockedForPlayerChange(player)) {
            return 0;
        }

        PlayerStats stats = StatsUtil.get(player);
        StatsSnapshotService.snapshot(player, stats, "before /stats add " + stat.key());

        if (amount > 0) {
            if (stats.getAvailableStatPoints() < amount) {
                player.sendSystemMessage(Component.literal("Not enough points. Need " + amount + ", have " + stats.getAvailableStatPoints()));
                return 0;
            }
            int applied = stats.increaseStatMany(stat, amount);
            if (applied != amount) {
                player.sendSystemMessage(Component.literal("Could not apply full amount due to cap. Applied " + applied + "/" + amount));
            }
        } else if (amount < 0) {
            int toDecrease = -amount;
            int reduced = stats.decreaseStatMany(stat, toDecrease);
            if (reduced != toDecrease) {
                player.sendSystemMessage(Component.literal("Could not decrease full amount. Applied " + reduced + "/" + toDecrease));
            }
        }

        applyAndSync(player, stats);
        StatsHistoryService.log(player, "Command add: " + stat.key() + " by " + amount + " -> " + stats.get(stat) + ", points=" + stats.getAvailableStatPoints());
        player.sendSystemMessage(Component.literal(stat.key() + " changed by " + amount + " -> " + stats.get(stat) + " | points " + stats.getAvailableStatPoints()));
        return 1;
    }

    private static int resetStats(ServerPlayer player) {
        if (isLockedForPlayerChange(player)) {
            return 0;
        }

        PlayerStats stats = StatsUtil.get(player);
        StatsSnapshotService.snapshot(player, stats, "before /stats reset");
        stats.resetAllocatedStats();

        applyAndSync(player, stats);
        StatsHistoryService.log(player, "Command reset all stats. points=" + stats.getAvailableStatPoints());
        player.sendSystemMessage(Component.literal("Stats reset complete. points: " + stats.getAvailableStatPoints()));
        return 1;
    }

    private static int adminSetStat(CommandSourceStack source, ServerPlayer target, StatType stat, int value) {
        PlayerStats stats = StatsUtil.get(target);
        StatsSnapshotService.snapshot(target, stats, "before admin set " + stat.key());
        stats.set(stat, Math.max(0, value));

        applyAndSync(target, stats);
        StatsHistoryService.log(target, "Admin set by " + source.getTextName() + ": " + stat.key() + " -> " + stats.get(stat));
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " " + stat.key() + "=" + stats.get(stat)), false);
        return 1;
    }

    private static int adminAddPoints(CommandSourceStack source, ServerPlayer target, int amount) {
        PlayerStats stats = StatsUtil.get(target);
        StatsSnapshotService.snapshot(target, stats, "before admin points change");
        stats.setAvailableStatPoints(Math.max(0, stats.getAvailableStatPoints() + amount));

        applyAndSync(target, stats);
        StatsHistoryService.log(target, "Admin points by " + source.getTextName() + ": delta=" + amount + ", now=" + stats.getAvailableStatPoints());
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " points=" + stats.getAvailableStatPoints()), false);
        return 1;
    }

    private static int adminSetPoints(CommandSourceStack source, ServerPlayer target, int value) {
        PlayerStats stats = StatsUtil.get(target);
        StatsSnapshotService.snapshot(target, stats, "before admin setpoints");
        stats.setAvailableStatPoints(value);

        applyAndSync(target, stats);
        StatsHistoryService.log(target, "Admin setpoints by " + source.getTextName() + ": now=" + stats.getAvailableStatPoints());
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " points=" + stats.getAvailableStatPoints()), false);
        return 1;
    }

    private static int adminResetXp(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);

        int oldLevel = target.experienceLevel;
        int oldTotal = target.totalExperience;
        float oldProgress = target.experienceProgress;

        target.experienceLevel = 0;
        target.totalExperience = 0;
        target.experienceProgress = 0.0F;
        stats.setLastTrackedLevel(0);

        target.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));
        ModNetwork.syncToPlayer(target, stats);

        StatsHistoryService.log(target,
                "Admin resetxp by " + source.getTextName()
                        + " (level=" + oldLevel
                        + ", totalXp=" + oldTotal
                        + ", progress=" + oldProgress + ")");

        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " xp reset to 0"), false);
        target.sendSystemMessage(Component.literal("경험치가 0으로 초기화되었습니다."));
        return 1;
    }

    private static int adminResetPoints(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        StatsSnapshotService.snapshot(target, stats, "before admin resetpoints");

        stats.setAvailableStatPoints(0);
        applyAndSync(target, stats);

        StatsHistoryService.log(target, "Admin resetpoints by " + source.getTextName() + " -> points=0");
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " stat points reset to 0"), false);
        target.sendSystemMessage(Component.literal("사용 가능 스탯 포인트가 0으로 초기화되었습니다."));
        return 1;
    }

    private static int adminSetLock(CommandSourceStack source, ServerPlayer target, boolean locked) {
        boolean changed = StatsAdminLockService.setLocked(target, locked);
        String state = locked ? "ON" : "OFF";

        if (changed) {
            StatsHistoryService.log(target, "Admin lock " + state + " by " + source.getTextName());
        }

        source.sendSuccess(() -> Component.literal("[admin] lock " + state + " for " + target.getName().getString()), false);
        target.sendSystemMessage(Component.literal(locked
                ? "관리자가 스탯 변경을 잠금했습니다."
                : "스탯 변경 잠금이 해제되었습니다."));
        return 1;
    }

    private static int adminLockStatus(CommandSourceStack source, ServerPlayer target) {
        boolean locked = StatsAdminLockService.isLocked(target);
        source.sendSuccess(() -> Component.literal("[admin] " + target.getName().getString() + " lock=" + locked), false);
        return 1;
    }

    private static int adminRecalc(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        applyAndSync(target, stats);
        StatsHistoryService.log(target, "Admin recalc by " + source.getTextName());
        source.sendSuccess(() -> Component.literal("[admin] recalculated " + target.getName().getString()), false);
        return 1;
    }

    private static int adminSnapshot(CommandSourceStack source, ServerPlayer target, String reason) {
        PlayerStats stats = StatsUtil.get(target);
        StatsSnapshotService.snapshot(target, stats, "manual: " + reason);
        StatsHistoryService.log(target, "Snapshot created by " + source.getTextName() + ": " + reason);
        source.sendSuccess(() -> Component.literal("[admin] snapshot saved for " + target.getName().getString() + " (" + StatsSnapshotService.count(target) + ")"), false);
        return 1;
    }

    private static int adminRollback(CommandSourceStack source, ServerPlayer target) {
        PlayerStats stats = StatsUtil.get(target);
        var snapshot = StatsSnapshotService.popLast(target);
        if (snapshot.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[admin] no snapshot to rollback for " + target.getName().getString()), false);
            return 0;
        }

        var state = snapshot.get();
        stats.applySnapshot(state.values(), state.points(), state.trackedLevel());

        applyAndSync(target, stats);
        StatsHistoryService.log(target, "Rollback by " + source.getTextName() + " -> snapshot " + state.timestamp() + " reason=" + state.reason());
        source.sendSuccess(() -> Component.literal("[admin] rolled back " + target.getName().getString() + " to snapshot " + state.timestamp()), false);
        return 1;
    }

    private static int showHistory(CommandSourceStack source, ServerPlayer target, int limit) {
        List<String> logs = StatsHistoryService.getRecent(target.getUUID(), limit);
        if (logs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No stat history for " + target.getName().getString()), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("=== Stat History: " + target.getName().getString() + " ==="), false);
        for (String line : logs) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return logs.size();
    }

    /*
     * 플레이어 스탯이 잠겨 있을 때 일반 변경 명령을 막습니다.
     * 관리자 명령(admin branch)은 운영 편의를 위해 잠금과 무관하게 실행됩니다.
     */
    private static boolean isLockedForPlayerChange(ServerPlayer player) {
        if (!StatsAdminLockService.isLocked(player)) {
            return false;
        }
        player.sendSystemMessage(Component.literal("현재 스탯 변경이 잠겨 있습니다. 관리자에게 문의하세요."));
        return true;
    }

    private static void applyAndSync(ServerPlayer player, PlayerStats stats) {
        StatsAttributeService.apply(player, stats);
        ModNetwork.syncToPlayer(player, stats);
    }

    private static StatType parseStat(String input) throws CommandSyntaxException {
        String normalized = input.toUpperCase(Locale.ROOT);
        for (StatType type : StatType.values()) {
            if (type.name().equals(normalized) || type.key().equalsIgnoreCase(input)) {
                return type;
            }
        }
        throw INVALID_STAT.create();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
