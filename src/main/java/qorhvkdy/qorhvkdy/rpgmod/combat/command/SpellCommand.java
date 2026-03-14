package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.spell.SpellAffinityService;

/**
 * 스펠 친화도 확인/리로드 커맨드.
 */
public final class SpellCommand {
    private SpellCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgspell")
                .executes(context -> all(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("affinity")
                        .executes(context -> all(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("school", StringArgumentType.word())
                                .executes(context -> one(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "school")
                                ))))
                .then(Commands.literal("reload")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .executes(context -> reload(context.getSource()))));
    }

    private static int all(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("Spell affinities: " + SpellAffinityService.allAffinities(player)), false);
        return 1;
    }

    private static int one(CommandSourceStack source, ServerPlayer player, String school) {
        double value = SpellAffinityService.affinity(player, school);
        source.sendSuccess(() -> Component.literal("Affinity[" + school + "]=" + value), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        SpellAffinityService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded spell-schools.json"), true);
        return 1;
    }
}
