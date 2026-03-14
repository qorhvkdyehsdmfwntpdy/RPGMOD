package qorhvkdy.qorhvkdy.rpgmod.classes.sim;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassCombatBonusService;
import qorhvkdy.qorhvkdy.rpgmod.classes.balance.ClassBalanceCoefficient;
import qorhvkdy.qorhvkdy.rpgmod.classes.balance.ClassBalanceCoefficientService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillDescriptor;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.classes.synergy.ClassPartySynergyService;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

import java.util.List;

/**
 * 클래스 밸런스 시뮬레이터.
 * 계산 비용을 낮추기 위해 폐회로 근사식을 사용한다.
 */
public final class ClassBalanceSimulatorService {
    private ClassBalanceSimulatorService() {
    }

    public record SimulationResult(
            int durationSeconds,
            double baselineDps,
            double estimatedSkillDps,
            double effectiveHpScore,
            double critFactor,
            String bestSkillId
    ) {
    }

    public static SimulationResult simulate(ServerPlayer player, int durationSeconds) {
        int duration = Math.max(10, durationSeconds);
        PlayerStats stats = StatsUtil.get(player);
        ClassBalanceCoefficient coefficient = ClassBalanceCoefficientService.resolve(stats);
        PassiveBonus baseBonus = ClassCombatBonusService.compute(player, stats);
        double partySynergy = ClassPartySynergyService.outgoingDamageMultiplier(player);

        double attackStat = (stats.getAttackPower() * coefficient.attackPowerScale())
                + ((stats.getMagicPower() * coefficient.magicPowerScale()) * 0.65);
        double speedFactor = 1.0 + (stats.getAttackSpeedPercent() / 100.0);
        double adjustedCritChance = stats.getCritChance() + coefficient.critChanceBonusPercent();
        double adjustedCritMultiplier = (stats.getCritDamage() + coefficient.critDamageBonusPercent()) / 100.0;
        double critFactor = 1.0 + (adjustedCritChance / 100.0) * Math.max(0.0, (adjustedCritMultiplier - 1.0));
        double baseline = attackStat
                * speedFactor
                * critFactor
                * baseBonus.attackDamageMultiplier()
                * coefficient.finalDamageMultiplier()
                * partySynergy;

        List<ClassSkillDescriptor> unlocked = ClassSkillService.unlocked(player, stats);
        double bestSkillGain = 1.0;
        String bestSkillId = "-";
        for (ClassSkillDescriptor skill : unlocked) {
            double uptime = Math.min(1.0, skill.durationTicks() / Math.max(1.0, skill.cooldownMs() / 50.0));
            double gain = 1.0 + ((skill.bonusAttackMultiplier() - 1.0) * uptime);
            if (gain > bestSkillGain) {
                bestSkillGain = gain;
                bestSkillId = skill.id();
            }
        }
        double estimatedSkillDps = baseline * bestSkillGain;

        double hp = stats.getMaxHP(player.experienceLevel) * baseBonus.hpMultiplier();
        double armorFactor = 1.0 + ((stats.getDefense() * coefficient.defenseMultiplier()) + baseBonus.armorBonus()) * 0.04;
        double effectiveHpScore = hp * armorFactor;
        return new SimulationResult(duration, baseline, estimatedSkillDps, effectiveHpScore, critFactor, bestSkillId);
    }
}
