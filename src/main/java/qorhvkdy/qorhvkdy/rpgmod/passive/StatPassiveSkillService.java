package qorhvkdy.qorhvkdy.rpgmod.passive;

import qorhvkdy.qorhvkdy.rpgmod.passive.data.StatPassiveSkillJson;
import qorhvkdy.qorhvkdy.rpgmod.passive.data.StatPassiveSkillRepository;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

/**
 * 스탯 임계치 기반 패시브 스킬 계산기.
 */
public final class StatPassiveSkillService {
    private static volatile List<StatPassiveSkillJson.Entry> skills = List.of();

    private StatPassiveSkillService() {
    }

    public static void bootstrap() {
        StatPassiveSkillRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        StatPassiveSkillRepository.reload();
        skills = List.copyOf(StatPassiveSkillRepository.get().skills);
    }

    public static PassiveBonus compute(PlayerStats stats) {
        PassiveBonus total = PassiveBonus.none();
        for (StatPassiveSkillJson.Entry skill : skills) {
            StatType type = parseStat(skill.stat);
            if (type == null) {
                continue;
            }
            if (stats.get(type) < Math.max(0, skill.requiredStat)) {
                continue;
            }
            total = total.combine(new PassiveBonus(
                    clampMultiplier(skill.hpMultiplier),
                    clampMultiplier(skill.attackDamageMultiplier),
                    skill.moveSpeedBonusPercent,
                    skill.armorBonus,
                    skill.critChanceBonusPercent,
                    skill.critDamageBonusPercent
            ));
        }
        return total;
    }

    public static List<String> unlockedSkillIds(PlayerStats stats) {
        List<String> unlocked = new ArrayList<>();
        for (StatPassiveSkillJson.Entry skill : skills) {
            StatType type = parseStat(skill.stat);
            if (type == null) {
                continue;
            }
            if (stats.get(type) >= Math.max(0, skill.requiredStat)) {
                unlocked.add(skill.id);
            }
        }
        return unlocked;
    }

    private static StatType parseStat(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        for (StatType type : StatType.values()) {
            if (type.key().equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    private static double clampMultiplier(double value) {
        return value <= 0.0 ? 1.0 : value;
    }
}

