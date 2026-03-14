package qorhvkdy.qorhvkdy.rpgmod.passive;

/**
 * 패시브 보너스 공통 모델.
 * - multiplier 계열은 곱연산
 * - bonus 계열은 가산연산
 */
public record PassiveBonus(
        double hpMultiplier,
        double attackDamageMultiplier,
        double moveSpeedBonusPercent,
        double armorBonus,
        double critChanceBonusPercent,
        double critDamageBonusPercent
) {
    public static PassiveBonus none() {
        return new PassiveBonus(1.0, 1.0, 0.0, 0.0, 0.0, 0.0);
    }

    public PassiveBonus combine(PassiveBonus other) {
        if (other == null) {
            return this;
        }
        return new PassiveBonus(
                this.hpMultiplier * other.hpMultiplier,
                this.attackDamageMultiplier * other.attackDamageMultiplier,
                this.moveSpeedBonusPercent + other.moveSpeedBonusPercent,
                this.armorBonus + other.armorBonus,
                this.critChanceBonusPercent + other.critChanceBonusPercent,
                this.critDamageBonusPercent + other.critDamageBonusPercent
        );
    }
}

