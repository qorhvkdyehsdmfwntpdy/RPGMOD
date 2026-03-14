package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.npc.NpcDialogueService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

/**
 * NPC 대화 테스트 커맨드.
 */
public final class NpcCommand {
    private NpcCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgnpc")
                .then(Commands.literal("talk")
                        .then(Commands.argument("npcId", StringArgumentType.word())
                                .executes(context -> talk(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "npcId")
                                ))))
                .then(Commands.literal("choose")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> choose(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "index")
                                ))))
                .then(Commands.literal("exit")
                        .executes(context -> exit(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("reload")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.CLASS_ADMIN))
                        .executes(context -> reload(context.getSource()))));
    }

    private static int talk(CommandSourceStack source, ServerPlayer player, String npcId) {
        var view = NpcDialogueService.talk(player, npcId).orElse(null);
        if (view == null) {
            source.sendFailure(Component.literal("Unknown npc dialogue: " + npcId));
            return 0;
        }
        printView(source, view);
        return 1;
    }

    private static int choose(CommandSourceStack source, ServerPlayer player, int index) {
        var view = NpcDialogueService.choose(player, index).orElse(null);
        if (view == null) {
            source.sendFailure(Component.literal("Invalid dialogue choice."));
            return 0;
        }
        printView(source, view);
        return 1;
    }

    private static int exit(CommandSourceStack source, ServerPlayer player) {
        NpcDialogueService.exit(player);
        source.sendSuccess(() -> Component.literal("Dialogue session closed."), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        NpcDialogueService.reload();
        source.sendSuccess(() -> Component.literal("Reloaded npc-dialogues.json"), true);
        return 1;
    }

    private static void printView(CommandSourceStack source, NpcDialogueService.DialogueView view) {
        source.sendSuccess(() -> Component.literal("[NPC:" + view.npcId() + "] " + view.text()), false);
        for (String option : view.options()) {
            source.sendSuccess(() -> Component.literal(option), false);
        }
    }
}
