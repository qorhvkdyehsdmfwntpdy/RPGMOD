package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 파생 스탯을 실제 Minecraft Attribute에 적용하는 서비스입니다.
 * 수정 예시: 최대 체력 상한을 조정하려면 HP attribute 적용 클램프 범위를 수정합니다.
 */


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassCombatBonusService;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;

public final class StatsAttributeService {
    private static final double MAX_HEALTH_ATTRIBUTE_CAP = 1024.0;
    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double DEFAULT_ATTACK_SPEED = 4.0;
    private static final double DEFAULT_ATTACK_DAMAGE = 1.0;

    private StatsAttributeService() {
    }

    public static void apply(ServerPlayer player, PlayerStats stats) {
        PassiveBonus passiveBonus = ClassCombatBonusService.compute(player, stats);
        applyHealth(player, stats, passiveBonus);
        applyMoveSpeed(player, stats, passiveBonus);
        applyAttackSpeed(player, stats);
        applyAttackDamage(player, stats, passiveBonus);
        applyDefense(player, stats, passiveBonus);
    }

    private static void applyHealth(ServerPlayer player, PlayerStats stats, PassiveBonus passiveBonus) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }

        double oldMax = player.getMaxHealth();
        float oldHealth = player.getHealth();
        double desiredMaxHealth = stats.getMaxHP(player.experienceLevel) * passiveBonus.hpMultiplier();
        double appliedMaxHealth = Math.min(MAX_HEALTH_ATTRIBUTE_CAP, desiredMaxHealth);

        maxHealthAttr.setBaseValue(appliedMaxHealth);

        double newMax = player.getMaxHealth();
        float scaledHealth = oldMax > 0.0 ? (float) (oldHealth * (newMax / oldMax)) : (float) newMax;
        player.setHealth(Math.max(1.0f, Math.min((float) newMax, scaledHealth)));

        float overflowHealth = (float) Math.max(0.0, desiredMaxHealth - appliedMaxHealth);
        player.setAbsorptionAmount(overflowHealth);
    }

    private static void applyMoveSpeed(ServerPlayer player, PlayerStats stats, PassiveBonus passiveBonus) {
        AttributeInstance moveSpeedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeedAttr == null) {
            return;
        }

        double totalMoveSpeedPercent = stats.getMoveSpeedPercent() + passiveBonus.moveSpeedBonusPercent();
        double multiplier = 1.0 + (totalMoveSpeedPercent / 100.0);
        moveSpeedAttr.setBaseValue(clamp(DEFAULT_MOVE_SPEED * multiplier, 0.02, 1.2));
    }

    private static void applyAttackSpeed(ServerPlayer player, PlayerStats stats) {
        AttributeInstance attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr == null) {
            return;
        }

        double multiplier = 1.0 + (stats.getAttackSpeedPercent() / 100.0);
        attackSpeedAttr.setBaseValue(clamp(DEFAULT_ATTACK_SPEED * multiplier, 1.0, 20.0));
    }

    private static void applyAttackDamage(ServerPlayer player, PlayerStats stats, PassiveBonus passiveBonus) {
        AttributeInstance attackDamageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr == null) {
            return;
        }

        double value = DEFAULT_ATTACK_DAMAGE + (stats.getAttackPower() * 0.12);
        value *= passiveBonus.attackDamageMultiplier();
        attackDamageAttr.setBaseValue(clamp(value, 1.0, 200.0));
    }

    private static void applyDefense(ServerPlayer player, PlayerStats stats, PassiveBonus passiveBonus) {
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr == null) {
            return;
        }

        double value = (stats.getDefense() * 0.20) + passiveBonus.armorBonus();
        armorAttr.setBaseValue(clamp(value, 0.0, 30.0));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
