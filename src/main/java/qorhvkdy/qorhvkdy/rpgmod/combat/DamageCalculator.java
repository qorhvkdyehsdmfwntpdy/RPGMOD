package qorhvkdy.qorhvkdy.rpgmod.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassCombatBonusService;
import qorhvkdy.qorhvkdy.rpgmod.classes.balance.ClassBalanceCoefficient;
import qorhvkdy.qorhvkdy.rpgmod.classes.balance.ClassBalanceCoefficientService;
import qorhvkdy.qorhvkdy.rpgmod.classes.synergy.ClassPartySynergyService;
import qorhvkdy.qorhvkdy.rpgmod.combat.formula.CombatFormulaService;
import qorhvkdy.qorhvkdy.rpgmod.combat.formula.data.CombatFormulaJson;
import qorhvkdy.qorhvkdy.rpgmod.combat.profile.CombatProfileService;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.skill.tree.SkillTreeService;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDataService;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponOptionDescriptor;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Combat final damage formula.
 * Separates base power, mitigation, penetration, crit and skill-tree scaling.
 */
public final class DamageCalculator {
    public record DamageResult(float damage, boolean critical, double critChancePercent, double critMultiplier, double defenseUsed) {
    }

    private DamageCalculator() {
    }

    public static DamageResult calculateResult(Player attacker, LivingEntity target, float baseDamage, float weaponCritBonus) {
        CombatFormulaJson f = CombatFormulaService.data();
        PlayerStats attackerStats = StatsUtil.get(attacker);
        ClassBalanceCoefficient classCoefficient = ClassBalanceCoefficientService.resolve(attackerStats);
        PassiveBonus passiveBonus = ClassCombatBonusService.compute(attacker, attackerStats);
        WeaponRuntimeBonus weaponBonus = resolveWeaponBonus(attacker);

        int treeUnlocked = 0;
        double partySynergyMultiplier = 1.0;
        if (attacker instanceof ServerPlayer serverPlayer) {
            treeUnlocked = SkillTreeService.unlockedNodes(serverPlayer).size();
            partySynergyMultiplier = ClassPartySynergyService.outgoingDamageMultiplier(serverPlayer);
        }

        // 1) Raw attack power from base attack + primary stat powers.
        double scaledAttackPower = attackerStats.getAttackPower() * classCoefficient.attackPowerScale();
        double scaledMagicPower = attackerStats.getMagicPower() * classCoefficient.magicPowerScale();
        double rawPower = (baseDamage * clamp(f.baseDamageWeight, 0.1, 10.0))
                + (scaledAttackPower * clamp(f.attackPowerScale, 0.0, 10.0))
                + (scaledMagicPower * clamp(f.magicPowerScale, 0.0, 10.0))
                + weaponBonus.flatDamageBonus;

        // 2) Scalers (level + weapon/profile + passive + skill tree progression).
        double levelMul = 1.0 + (Math.max(0, attacker.experienceLevel) * clamp(f.levelBonusPerLevel, 0.0, 0.05));
        double treeMul = 1.0 + Math.min(clamp(f.maxTreeDamageBonus, 0.0, 3.0), treeUnlocked * clamp(f.treeDamagePerNode, 0.0, 1.0));
        double outgoingDamage = rawPower
                * levelMul
                * weaponBonus.damageMultiplier
                * passiveBonus.attackDamageMultiplier()
                * treeMul
                * classCoefficient.finalDamageMultiplier()
                * partySynergyMultiplier;

        // 3) Mitigation with penetration.
        double defense = resolveDefense(target);
        double penPercent = (attackerStats.getStr() * clamp(f.strPenPerPoint, 0.0, 0.1))
                + (attackerStats.getAgi() * clamp(f.agiPenPerPoint, 0.0, 0.1))
                + (treeUnlocked * clamp(f.treePenPerNode, 0.0, 0.3));
        penPercent = Math.min(clamp(f.maxDefensePenPercent, 0.0, 0.95), Math.max(0.0, penPercent));
        double effectiveDefense = Math.max(0.0, defense * (1.0 - penPercent));

        double mitigation = 1.0 / (1.0 + (effectiveDefense * clamp(f.defenseScale, 0.0001, 1.0)));
        mitigation = Math.max(clamp(f.minDefenseMultiplier, 0.01, 1.0), Math.min(1.0, mitigation));

        double finalBeforeCrit = Math.max(clamp(f.minFinalDamage, 0.0, 10000.0), outgoingDamage * mitigation);

        // 4) Critical.
        double critChance = attackerStats.getCritChance()
                + passiveBonus.critChanceBonusPercent()
                + weaponBonus.critChanceBonusPercent
                + classCoefficient.critChanceBonusPercent();
        critChance = Math.min(clamp(f.critChanceCap, 0.0, 100.0), Math.max(0.0, critChance));
        double chanceRatio = Math.max(0.0, Math.min(1.0, critChance / 100.0));
        boolean critical = ThreadLocalRandom.current().nextDouble() < chanceRatio;

        if (!critical) {
            return new DamageResult((float) finalBeforeCrit, false, critChance, 1.0, effectiveDefense);
        }

        double critDamagePercent = attackerStats.getCritDamage()
                + passiveBonus.critDamageBonusPercent()
                + weaponBonus.critDamageBonusPercent
                + classCoefficient.critDamageBonusPercent();
        critDamagePercent = Math.min(clamp(f.critDamageCap, 100.0, 3000.0), Math.max(100.0, critDamagePercent));
        double critMul = (critDamagePercent / 100.0) * (1.0 + Math.max(0.0f, weaponCritBonus));

        return new DamageResult((float) (finalBeforeCrit * critMul), true, critChance, critMul, effectiveDefense);
    }

    private static double resolveDefense(LivingEntity target) {
        if (target instanceof Player targetPlayer) {
            PlayerStats targetStats = StatsUtil.get(targetPlayer);
            ClassBalanceCoefficient coefficient = ClassBalanceCoefficientService.resolve(targetStats);
            return targetStats.getDefense() * coefficient.defenseMultiplier();
        }
        return target.getAttributeValue(Attributes.ARMOR);
    }

    private static WeaponRuntimeBonus resolveWeaponBonus(Player attacker) {
        WeaponRuntimeBonus total = new WeaponRuntimeBonus();
        CombatProfileService.find(attacker.getMainHandItem()).ifPresent(profile ->
                total.damageMultiplier *= profile.damageMultiplier()
        );
        WeaponDataService.find(attacker.getMainHandItem()).ifPresent(descriptor -> {
            for (WeaponOptionDescriptor option : descriptor.options()) {
                String stat = option.stat();
                String mode = option.mode();
                double value = option.value();
                boolean percent = "percent".equalsIgnoreCase(mode) || "pct".equalsIgnoreCase(mode);

                if ("attack_power".equals(stat) || "damage".equals(stat)) {
                    if (percent) {
                        total.damageMultiplier *= 1.0 + (value / 100.0);
                    } else {
                        total.flatDamageBonus += value;
                    }
                } else if ("crit_chance".equals(stat)) {
                    total.critChanceBonusPercent += value;
                } else if ("crit_damage".equals(stat)) {
                    total.critDamageBonusPercent += value;
                }
            }
        });
        return total;
    }

    private static final class WeaponRuntimeBonus {
        private double flatDamageBonus = 0.0;
        private double damageMultiplier = 1.0;
        private double critChanceBonusPercent = 0.0;
        private double critDamageBonusPercent = 0.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
