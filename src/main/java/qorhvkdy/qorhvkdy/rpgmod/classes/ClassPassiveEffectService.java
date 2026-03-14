package qorhvkdy.qorhvkdy.rpgmod.classes;

import qorhvkdy.qorhvkdy.rpgmod.classes.data.ClassPassiveEffectJson;
import qorhvkdy.qorhvkdy.rpgmod.classes.data.ClassPassiveEffectRepository;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 패시브 슬롯의 실제 전투 보정치 계산기.
 * 단순 규칙 기반으로 시작하고, 이후 JSON 테이블 기반으로 교체하기 쉽도록 분리했다.
 */
public final class ClassPassiveEffectService {
    private static volatile Map<String, ClassPassiveEffectJson.BonusEntry> table = Map.of();

    private ClassPassiveEffectService() {
    }

    public static void bootstrap() {
        ClassPassiveEffectRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        ClassPassiveEffectRepository.reload();
        ClassPassiveEffectJson json = ClassPassiveEffectRepository.get();
        Map<String, ClassPassiveEffectJson.BonusEntry> next = new LinkedHashMap<>();
        for (Map.Entry<String, ClassPassiveEffectJson.BonusEntry> entry : json.passives.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            next.put(entry.getKey().trim().toLowerCase(), entry.getValue());
        }
        table = Map.copyOf(next);
    }

    public static PassiveBonus compute(PlayerStats stats) {
        Map<String, String> slots = stats.getPassiveSlots();
        PassiveBonus total = PassiveBonus.none();
        for (String passiveId : slots.values()) {
            if (passiveId == null || passiveId.isBlank()) {
                continue;
            }
            ClassPassiveEffectJson.BonusEntry bonus = table.get(passiveId.trim().toLowerCase());
            if (bonus == null) {
                continue;
            }
            total = total.combine(new PassiveBonus(
                    clampMultiplier(bonus.hpMultiplier),
                    clampMultiplier(bonus.attackDamageMultiplier),
                    bonus.moveSpeedBonusPercent,
                    bonus.armorBonus,
                    bonus.critChanceBonusPercent,
                    bonus.critDamageBonusPercent
            ));
        }
        return total;
    }

    private static double clampMultiplier(double value) {
        if (value <= 0.0) {
            return 1.0;
        }
        return value;
    }
}
