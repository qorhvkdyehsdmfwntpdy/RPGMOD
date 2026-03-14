package qorhvkdy.qorhvkdy.rpgmod.classes.seteffect;

import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;

/**
 * 클래스 세트 효과 정의.
 */
public record ClassSetEffectDescriptor(
        String id,
        String category,
        String requiredClass,
        String requiredAdvancement,
        int requiredPieces,
        String requiredPassiveId,
        PassiveBonus bonus
) {
}
