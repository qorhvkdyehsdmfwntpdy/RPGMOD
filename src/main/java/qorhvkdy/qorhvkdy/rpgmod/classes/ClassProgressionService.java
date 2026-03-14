package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsAttributeService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsHistoryService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsSnapshotService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

import java.util.Objects;

/**
 * Single write path for class and promotion mutations.
 * Commands and GUI packets should both call this service.
 */
public final class ClassProgressionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassProgressionService.class);

    private ClassProgressionService() {
    }

    public static ClassOperationResult setBaseClass(ServerPlayer actor, ServerPlayer target, PlayerClassType type) {
        if (type == null || type == PlayerClassType.NONE) {
            return ClassOperationResult.fail("Invalid base class.");
        }

        PlayerStats stats = StatsUtil.get(target);
        ClassAdvancement beforeAdv = stats.getCurrentAdvancement();
        PlayerClassType beforeClass = stats.getSelectedClass();

        StatsSnapshotService.snapshot(target, stats, "before class set to " + type.id());
        stats.setSelectedClass(type);

        ClassAdvancement afterAdv = stats.getCurrentAdvancement();
        StatsHistoryService.log(target, "Base class set: " + beforeClass.id() + " -> " + type.id() + " by " + actor.getName().getString());
        ClassLifecycleHooks.fireClassChanged(target, stats, beforeClass, type, beforeAdv, afterAdv);
        StatsAttributeService.apply(target, stats);
        ModNetwork.syncToPlayer(target, stats);
        RpgAuditLogService.progression("set_base actor=" + actor.getName().getString()
                + ", target=" + target.getName().getString()
                + ", from=" + beforeClass.id()
                + ", to=" + type.id());
        return ClassOperationResult.ok("Base class set to " + type.id());
    }

    public static ClassOperationResult promote(ServerPlayer actor, ServerPlayer target, String targetAdvancementId) {
        PlayerStats stats = StatsUtil.get(target);
        ClassAdvancement before = stats.getCurrentAdvancement();
        ClassAdvancement targetNode = ClassAdvancementRegistry.get(targetAdvancementId).orElse(null);
        if (targetNode == null) {
            return ClassOperationResult.fail("Unknown advancement id: " + targetAdvancementId);
        }

        boolean firstPromotionFromNovice =
                stats.getSelectedClass() == PlayerClassType.NONE
                        && before.baseClass() == PlayerClassType.NONE
                        && targetNode.tier() == JobTier.FIRST;
        if (!firstPromotionFromNovice && targetNode.baseClass() != stats.getSelectedClass()) {
            return ClassOperationResult.fail("Advancement does not match selected base class.");
        }
        if (!Objects.equals(targetNode.parentId(), before.id())) {
            return ClassOperationResult.fail("Invalid promotion route. Target must be direct child of current advancement.");
        }

        ClassAdvancementRequirementEvaluator.PromotionValidationResult check = ClassAdvancementRequirementEvaluator.evaluate(
                new PromotionCheckContext(target, stats, before, targetNode)
        );
        if (!check.passed()) {
            RpgDebugSettings.LogLevel level = RpgDebugSettings.progressionLogLevel();
            if (level != RpgDebugSettings.LogLevel.OFF) {
                LOGGER.info(
                        "Promotion blocked: actor={}, target={}, from={}, to={}, errors={}",
                        actor.getName().getString(),
                        target.getName().getString(),
                        before.id(),
                        targetNode.id(),
                        check.errors().size()
                );
            }
            if (level == RpgDebugSettings.LogLevel.DEBUG) {
                LOGGER.debug("Promotion error details: {}", check.errors());
            }
            return ClassOperationResult.fail("Promotion requirements not met.", check.errors());
        }

        StatsSnapshotService.snapshot(target, stats, "before promotion to " + targetNode.id());
        if (firstPromotionFromNovice) {
            stats.setSelectedClass(targetNode.baseClass());
        }
        stats.setCurrentAdvancementId(targetNode.id());
        ClassAdvancement after = stats.getCurrentAdvancement();

        StatsHistoryService.log(target, "Promoted: " + before.id() + " -> " + after.id() + " by " + actor.getName().getString());
        ClassLifecycleHooks.firePromotion(target, stats, before, after);
        StatsAttributeService.apply(target, stats);
        ModNetwork.syncToPlayer(target, stats);
        RpgAuditLogService.progression("promote actor=" + actor.getName().getString()
                + ", target=" + target.getName().getString()
                + ", from=" + before.id()
                + ", to=" + after.id());
        if (RpgDebugSettings.progressionLogLevel() == RpgDebugSettings.LogLevel.DEBUG) {
            LOGGER.debug("Promotion success: actor={}, target={}, {} -> {}",
                    actor.getName().getString(),
                    target.getName().getString(),
                    before.id(),
                    after.id());
        }
        return ClassOperationResult.ok("Promoted to " + after.id());
    }
}
