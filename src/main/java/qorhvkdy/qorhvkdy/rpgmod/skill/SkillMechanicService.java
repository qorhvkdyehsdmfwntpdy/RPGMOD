package qorhvkdy.qorhvkdy.rpgmod.skill;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.skill.runtime.SkillRuntimeService;

import java.util.List;

/**
 * 하위 호환 래퍼.
 * 기존 호출부는 유지하고, 내부는 SkillRuntimeService를 사용한다.
 */
public final class SkillMechanicService {
    private SkillMechanicService() {
    }

    public static synchronized void bootstrap() {
        SkillRuntimeService.bootstrap();
    }

    public static synchronized void reload() {
        SkillRuntimeService.reload();
    }

    public static CastResult cast(ServerPlayer player, String skillId, String trigger) {
        SkillRuntimeService.CastResult result = SkillRuntimeService.cast(player, skillId, trigger);
        return new CastResult(result.success(), result.reason(), result.executedActions());
    }

    public static TriggerResult trigger(ServerPlayer player, String trigger) {
        SkillRuntimeService.TriggerResult result = SkillRuntimeService.trigger(player, trigger);
        return new TriggerResult(result.successCount(), result.failedCount());
    }

    public static List<String> listSkills() {
        return SkillRuntimeService.listSkills();
    }

    public record CastResult(boolean success, String reason, int executedActions) {
    }

    public record TriggerResult(int successCount, int failedCount) {
    }
}

