package qorhvkdy.qorhvkdy.rpgmod.weapon;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 런타임 무기 설명 모델.
 */
public record WeaponDescriptor(
        String itemId,
        String displayName,
        String category,
        String grade,
        String rarity,
        String requiredClass,
        String requiredAdvancement,
        List<WeaponOptionDescriptor> options,
        List<WeaponAffixDescriptor> affixPool,
        WeaponSocketDescriptor socket,
        List<String> obtainMethods,
        boolean dropEnabled,
        double baseDropChance,
        double dropWeight,
        double minMobMaxHealth
) {
    public Component displayNameComponent() {
        if (displayName != null && displayName.startsWith("weapon.rpgmod.")) {
            return Component.translatable(displayName);
        }
        return Component.literal(displayName == null ? itemId : displayName);
    }
}
