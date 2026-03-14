package qorhvkdy.qorhvkdy.rpgmod.weapon;

/**
 * 무기 접두/접미 수식어 템플릿.
 */
public record WeaponAffixDescriptor(
        String type,
        String id,
        String display,
        double optionMultiplier
) {
}
