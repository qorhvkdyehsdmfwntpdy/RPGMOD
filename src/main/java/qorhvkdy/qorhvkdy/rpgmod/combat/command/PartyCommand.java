package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

/**
 * 정식 파티 커맨드.
 */
public final class PartyCommand {
    private PartyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .requires(PartyCommand::canUse)
                .executes(context -> info(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("create")
                        .executes(context -> create(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("invite")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> invite(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("accept")
                        .executes(context -> accept(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("leave")
                        .executes(context -> leave(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("kick")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> kick(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("disband")
                        .executes(context -> disband(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("admin")
                        .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.PARTY_FORCE_KICK))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("partyId", StringArgumentType.word())
                                                .executes(context -> adminSet(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "partyId")
                                                )))))
                        .then(Commands.literal("forcekick")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> adminForceKick(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target")
                                        ))))));
    }

    private static int info(CommandSourceStack source, ServerPlayer actor) {
        String partyId = PartyService.getPartyId(actor.getUUID()).orElse("-");
        boolean leader = PartyService.isLeader(actor.getUUID());
        source.sendSuccess(() -> Component.literal("party=" + partyId + ", leader=" + leader), false);
        return 1;
    }

    private static int create(CommandSourceStack source, ServerPlayer actor) {
        source.sendSuccess(() -> Component.literal(PartyService.createParty(actor.getUUID())), false);
        return 1;
    }

    private static int invite(CommandSourceStack source, ServerPlayer actor, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(PartyService.invite(actor.getUUID(), target.getUUID())), false);
        target.sendSystemMessage(Component.literal(actor.getName().getString() + " invited you. Use /party accept"));
        return 1;
    }

    private static int accept(CommandSourceStack source, ServerPlayer actor) {
        source.sendSuccess(() -> Component.literal(PartyService.acceptInvite(actor.getUUID())), false);
        return 1;
    }

    private static int leave(CommandSourceStack source, ServerPlayer actor) {
        source.sendSuccess(() -> Component.literal(PartyService.leave(actor.getUUID())), false);
        return 1;
    }

    private static int kick(CommandSourceStack source, ServerPlayer actor, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(PartyService.kick(actor.getUUID(), target.getUUID())), false);
        return 1;
    }

    private static int disband(CommandSourceStack source, ServerPlayer actor) {
        source.sendSuccess(() -> Component.literal(PartyService.disband(actor.getUUID())), false);
        return 1;
    }

    private static int adminSet(CommandSourceStack source, ServerPlayer target, String partyId) {
        PartyService.assignParty(target.getUUID(), partyId);
        source.sendSuccess(() -> Component.literal("Assigned party: " + target.getName().getString() + " -> " + partyId), true);
        return 1;
    }

    private static int adminForceKick(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(PartyService.forceKick(target)), true);
        return 1;
    }

    private static boolean canUse(CommandSourceStack source) {
        return RpgPermissionService.hasOrOp(source, PermissionNodes.PARTY_MANAGE);
    }
}
