package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.ClassSetEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.passive.StatPassiveSkillService;
import qorhvkdy.qorhvkdy.rpgmod.skill.passive.PassiveSkillProgramService;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

/**
 * 전투 보너스 통합 계산.
 */
public final class ClassCombatBonusService {
    private ClassCombatBonusService() {
    }

    public static PassiveBonus compute(Player player, PlayerStats stats) {
        PassiveBonus total = ClassPassiveEffectService.compute(stats).combine(StatPassiveSkillService.compute(stats));
        if (player instanceof ServerPlayer serverPlayer) {
            total = total.combine(PassiveSkillProgramService.bonusOf(serverPlayer, stats));
            total = total.combine(ClassSetEffectService.compute(serverPlayer, stats));
            total = total.combine(ClassSkillService.activeBonus(serverPlayer));
        }
        return total;
    }
}
