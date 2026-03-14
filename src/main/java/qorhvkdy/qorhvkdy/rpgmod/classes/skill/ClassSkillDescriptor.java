package qorhvkdy.qorhvkdy.rpgmod.classes.skill;

import net.minecraft.network.chat.Component;

/**
 * 클래스 액티브 스킬 정의.
 */
public record ClassSkillDescriptor(
        String id,
        String displayName,
        String requiredAdvancementId,
        int requiredLevel,
        int minStr,
        int minAgi,
        int minWis,
        int minLuk,
        double resourceCost,
        long cooldownMs,
        int durationTicks,
        double bonusAttackMultiplier,
        double bonusArmor,
        double bonusCritChance,
        double bonusCritDamage
) {
    public Component displayNameComponent() {
        if (displayName != null && displayName.startsWith("skill.rpgmod.")) {
            return Component.translatable(displayName);
        }
        return Component.literal(displayName == null ? id : displayName);
    }
}
