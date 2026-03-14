package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDropService;

/**
 * 운영 디버그 커맨드.
 */
public final class RpgDebugCommand {
    private RpgDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgdebug")
                .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.DEBUG_ADMIN))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("loglevel")
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("off");
                                    builder.suggest("info");
                                    builder.suggest("debug");
                                    return builder.buildFuture();
                                })
                                .executes(context -> setLogLevel(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "level")
                                ))))
                .then(Commands.literal("drop")
                        .then(Commands.literal("simulate")
                                .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.DEBUG_DROP_SIMULATE))
                                .then(Commands.argument("kills", IntegerArgumentType.integer(1, 100000))
                                        .executes(context -> simulate(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "kills"),
                                                20.0
                                        ))
                                        .then(Commands.argument("mob_hp", DoubleArgumentType.doubleArg(1.0, 10000.0))
                                                .executes(context -> simulate(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "kills"),
                                                        DoubleArgumentType.getDouble(context, "mob_hp")
                                                )))))));
    }

    private static int reload(CommandSourceStack source) {
        RpgDebugSettings.reload();
        source.sendSuccess(() -> Component.literal("Reloaded debug-settings.json"), true);
        return 1;
    }

    private static int setLogLevel(CommandSourceStack source, String raw) {
        RpgDebugSettings.LogLevel level;
        switch (raw.toLowerCase()) {
            case "off" -> level = RpgDebugSettings.LogLevel.OFF;
            case "debug" -> level = RpgDebugSettings.LogLevel.DEBUG;
            case "info" -> level = RpgDebugSettings.LogLevel.INFO;
            default -> {
                source.sendFailure(Component.literal("Invalid level. Use: off|info|debug"));
                return 0;
            }
        }
        RpgDebugSettings.setProgressionLogLevel(level);
        source.sendSuccess(() -> Component.literal("Set progressionLogLevel=" + level.name().toLowerCase()), true);
        return 1;
    }

    private static int simulate(CommandSourceStack source, int kills, double mobHp) {
        WeaponDropService.DropSimulationResult result = WeaponDropService.simulateDrops(kills, mobHp, true);
        RpgAuditLogService.drop("simulate actor=" + source.getTextName()
                + ", kills=" + kills
                + ", mobHp=" + mobHp
                + ", totalDrops=" + result.totalDrops()
                + ", rate=" + round3(result.dropRatePerKill()));
        source.sendSuccess(() -> Component.literal(
                "Drop simulation: kills=" + result.kills()
                        + ", totalDrops=" + result.totalDrops()
                        + ", dropPerKill=" + round3(result.dropRatePerKill())
        ), false);

        int shown = 0;
        for (var entry : result.byItemId().entrySet()) {
            if (shown >= 10) {
                break;
            }
            shown++;
            source.sendSuccess(() -> Component.literal("- " + entry.getKey() + ": " + entry.getValue()), false);
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal("- no drops"), false);
        }
        return 1;
    }

    private static String round3(double value) {
        return String.valueOf(Math.round(value * 1000.0) / 1000.0);
    }
}
