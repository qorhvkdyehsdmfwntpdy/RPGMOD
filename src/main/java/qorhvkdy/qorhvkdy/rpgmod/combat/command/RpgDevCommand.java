package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.core.doctor.RpgDoctorService;
import qorhvkdy.qorhvkdy.rpgmod.core.module.RpgModuleRegistry;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionContextPriorityService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionContextResolver;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionNodes;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;

/**
 * 운영/개발 편의 커맨드.
 * 모듈 상태 조회, 리로드, 런타임 진단을 한곳에서 처리한다.
 */
public final class RpgDevCommand {
    private RpgDevCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpgdev")
                .requires(source -> RpgPermissionService.hasOrOp(source, PermissionNodes.DEV_ADMIN))
                .then(Commands.literal("modules")
                        .executes(context -> listModules(context.getSource()))
                        .then(Commands.literal("reload")
                                .then(Commands.literal("all")
                                        .executes(context -> reloadAll(context.getSource())))
                                .then(Commands.argument("moduleId", StringArgumentType.word())
                                        .executes(context -> reloadOne(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "moduleId")
                                        )))))
                .then(Commands.literal("doctor")
                        .executes(context -> doctor(context.getSource())))
                .then(Commands.literal("context")
                        .executes(context -> context(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("license")
                        .executes(context -> licenseGuide(context.getSource()))));
    }

    private static int listModules(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== RPG Modules ==="), false);
        for (RpgModuleRegistry.ModuleStatusView module : RpgModuleRegistry.statuses()) {
            source.sendSuccess(() -> Component.literal(
                    "- " + module.id()
                            + " | " + module.displayName()
                            + " | enabled=" + module.enabled()
                            + " | bootstrapped=" + module.bootstrapped()
                            + " | boot=" + module.lastBootstrapMs() + "ms"
                            + " | reload=" + module.lastReloadMs() + "ms"
                            + (module.errorMessage().isBlank() ? "" : " | error=" + module.errorMessage())
            ), false);
        }
        return 1;
    }

    private static int reloadAll(CommandSourceStack source) {
        RpgModuleRegistry.reloadAll();
        source.sendSuccess(() -> Component.literal("Reloaded all RPG modules."), true);
        return 1;
    }

    private static int reloadOne(CommandSourceStack source, String moduleId) {
        if (!RpgModuleRegistry.reload(moduleId)) {
            source.sendFailure(Component.literal("Unknown module id: " + moduleId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Reloaded module: " + moduleId), true);
        return 1;
    }

    private static int licenseGuide(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[License Safe]"), false);
        source.sendSuccess(() -> Component.literal("1) No direct code copy, reference structure/ideas only"), false);
        source.sendSuccess(() -> Component.literal("2) ARR/GPL sources must be reimplemented"), false);
        source.sendSuccess(() -> Component.literal("3) Keep config and UX compatible, not source code"), false);
        source.sendSuccess(() -> Component.literal("Doc: docs/architecture/license-safe-scope.md"), false);
        return 1;
    }

    private static int doctor(CommandSourceStack source) {
        RpgDoctorService.DoctorReport report = RpgDoctorService.runQuickChecks();
        source.sendSuccess(() -> Component.literal("=== RPG Doctor ==="), false);
        for (String line : report.infos()) {
            source.sendSuccess(() -> Component.literal("[INFO] " + line), false);
        }
        for (String line : report.warnings()) {
            source.sendSuccess(() -> Component.literal("[WARN] " + line), false);
        }
        for (String line : report.errors()) {
            source.sendSuccess(() -> Component.literal("[ERROR] " + line), false);
        }
        source.sendSuccess(() -> Component.literal(
                "Doctor result: " + (report.healthy() ? "HEALTHY" : "ISSUES")
                        + " | warnings=" + report.warnings().size()
                        + " | errors=" + report.errors().size()
        ), true);
        return report.healthy() ? 1 : 0;
    }

    private static int context(CommandSourceStack source, ServerPlayer player) {
        var resolved = PermissionContextResolver.resolve(player);
        var ordered = PermissionContextPriorityService.orderedPairs(resolved);
        source.sendSuccess(() -> Component.literal("Context ordered: " + ordered), false);
        return 1;
    }
}
