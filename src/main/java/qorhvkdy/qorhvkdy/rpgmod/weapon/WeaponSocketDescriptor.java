package qorhvkdy.qorhvkdy.rpgmod.weapon;

import java.util.List;

/**
 * 무기 소켓 정보 템플릿.
 */
public record WeaponSocketDescriptor(
        int minSockets,
        int maxSockets,
        List<String> allowedGems
) {
}
