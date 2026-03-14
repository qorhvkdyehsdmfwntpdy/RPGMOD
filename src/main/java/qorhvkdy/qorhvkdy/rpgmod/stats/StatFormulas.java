package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 기본 스탯을 이동속도/치명타/HP 등 파생 수치로 변환하는 공식 모음입니다.
 * 수정 예시: AGI 공속 기여도를 바꾸려면 attackSpeedPercent의 AGI 계수를 수정합니다.
 */


import java.util.EnumMap;
import java.util.Map;
import qorhvkdy.qorhvkdy.rpgmod.stats.data.StatsBalanceRepository;

public final class StatFormulas {
    public record Rule(int softCap, int hardCap, double valuePerPoint) {
    }

    private static final EnumMap<StatType, Rule> RULES = new EnumMap<>(StatType.class);
    private static final double SOFT_CAP_RATIO = 0.50;

    static {
        var caps = StatsBalanceRepository.get().primaryCaps;
        RULES.put(StatType.STR, toRule(caps.get("str"), 120, 500));
        RULES.put(StatType.AGI, toRule(caps.get("agi"), 120, 500));
        RULES.put(StatType.WIS, toRule(caps.get("wis"), 120, 500));
        RULES.put(StatType.LUK, toRule(caps.get("luk"), 100, 500));
        validateRules();
    }

    private StatFormulas() {
    }

    public static Rule rule(StatType type) {
        return RULES.get(type);
    }

    public static int clampPoints(StatType type, int points) {
        return Math.max(0, Math.min(rule(type).hardCap(), points));
    }

    public static double effectivePrimary(StatType type, int points) {
        Rule rule = rule(type);
        int clamped = clampPoints(type, points);
        int soft = rule.softCap();
        if (clamped <= soft) {
            return clamped * rule.valuePerPoint();
        }
        return (soft * rule.valuePerPoint()) + ((clamped - soft) * rule.valuePerPoint() * SOFT_CAP_RATIO);
    }

    public static double moveSpeedPercent(PlayerStats stats) {
        double agi = effectivePrimary(StatType.AGI, stats.getAgi());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return clamp(1.5 + (agi * 0.1) + (luk * 0.02), 0.0, 500.0);
    }

    public static double attackSpeedPercent(PlayerStats stats) {
        double agi = effectivePrimary(StatType.AGI, stats.getAgi());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return clamp((agi * 0.05) + (luk * 0.01), 0.0, 120.0);
    }

    public static double attackPower(PlayerStats stats) {
        double str = effectivePrimary(StatType.STR, stats.getStr());
        double agi = effectivePrimary(StatType.AGI, stats.getAgi());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return (str * 0.95) + (agi * 0.18) + (luk * 0.32);
    }

    public static double magicPower(PlayerStats stats) {
        double wis = effectivePrimary(StatType.WIS, stats.getWis());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return (wis * 1.65) + (luk * 0.22);
    }

    public static double defense(PlayerStats stats) {
        double str = effectivePrimary(StatType.STR, stats.getStr());
        double wis = effectivePrimary(StatType.WIS, stats.getWis());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return (str * 0.16) + (wis * 0.07) + (luk * 0.12);
    }

    public static double hpRegenPercent(PlayerStats stats) {
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        double str = effectivePrimary(StatType.STR, stats.getStr());
        double wis = effectivePrimary(StatType.WIS, stats.getWis());
        return clamp((luk * 0.12) + (str * 0.05) + (wis * 0.07), 0.0, 180.0);
    }

    public static double critChancePercent(PlayerStats stats) {
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        double agi = effectivePrimary(StatType.AGI, stats.getAgi());
        double str = effectivePrimary(StatType.STR, stats.getStr());
        return clamp(1.0 + (str * 0.01) + (luk * 0.020) + (agi * 0.035), 0.0, 72.0);
    }

    public static double critDamagePercent(PlayerStats stats) {
        double str = effectivePrimary(StatType.STR, stats.getStr());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        double agi = effectivePrimary(StatType.AGI, stats.getAgi());
        return clamp(100.0 + (str * 0.08) + (luk * 0.05) + (agi * 0.03), 100.0, 380.0);
    }

    public static int maxHp(PlayerStats stats, int level) {
        double str = effectivePrimary(StatType.STR, stats.getStr());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return (int) clamp(20.0 + (level * 2.0) + (str * 2.0) + (luk * 1.0), 20.0, 5000.0);
    }

    public static int maxMp(PlayerStats stats, int level) {
        double wis = effectivePrimary(StatType.WIS, stats.getWis());
        double luk = effectivePrimary(StatType.LUK, stats.getLuk());
        return (int) clamp(20.0 + (level * 2.0) + (wis * 6.0) + (luk * 0.1), 0.0, 5000.0);
    }

    public static String nextPointPreview(PlayerStats stats, StatType type, int level) {
        int sStr = stats.getStr();
        int sAgi = stats.getAgi();
        int sWis = stats.getWis();
        int sLuk = stats.getLuk();

        double atkNow = attackPowerRaw(sStr, sAgi, sWis, sLuk);
        double mAtkNow = magicPowerRaw(sStr, sAgi, sWis, sLuk);
        double hpNow = maxHpRaw(sStr, sAgi, sWis, sLuk, level);
        double mpNow = maxMpRaw(sStr, sAgi, sWis, sLuk, level);
        double critNow = critChanceRaw(sStr, sAgi, sWis, sLuk);
        double critDmgNow = critDamageRaw(sStr, sAgi, sWis, sLuk);
        double spdNow = moveSpeedRaw(sStr, sAgi, sWis, sLuk);
        double aspdNow = attackSpeedRaw(sStr, sAgi, sWis, sLuk);
        double defNow = defenseRaw(sStr, sAgi, sWis, sLuk);
        double regenNow = hpRegenRaw(sStr, sAgi, sWis, sLuk);

        switch (type) {
            case STR -> sStr++;
            case AGI -> sAgi++;
            case WIS -> sWis++;
            case LUK -> sLuk++;
        }

        double atkAfter = attackPowerRaw(sStr, sAgi, sWis, sLuk);
        double mAtkAfter = magicPowerRaw(sStr, sAgi, sWis, sLuk);
        double hpAfter = maxHpRaw(sStr, sAgi, sWis, sLuk, level);
        double mpAfter = maxMpRaw(sStr, sAgi, sWis, sLuk, level);
        double critAfter = critChanceRaw(sStr, sAgi, sWis, sLuk);
        double critDmgAfter = critDamageRaw(sStr, sAgi, sWis, sLuk);
        double spdAfter = moveSpeedRaw(sStr, sAgi, sWis, sLuk);
        double aspdAfter = attackSpeedRaw(sStr, sAgi, sWis, sLuk);
        double defAfter = defenseRaw(sStr, sAgi, sWis, sLuk);
        double regenAfter = hpRegenRaw(sStr, sAgi, sWis, sLuk);

        return type.displayName() + "+1 -> "
                + "ATK +" + fmt3(atkAfter - atkNow)
                + ", MATK +" + fmt3(mAtkAfter - mAtkNow)
                + ", HP +" + fmt3(hpAfter - hpNow)
                + ", MP +" + fmt3(mpAfter - mpNow)
                + ", CRIT +" + fmt3(critAfter - critNow) + "%"
                + ", CDMG +" + fmt3(critDmgAfter - critDmgNow) + "%"
                + ", SPD +" + fmt3(spdAfter - spdNow) + "%"
                + ", ASPD +" + fmt3(aspdAfter - aspdNow) + "%"
                + ", DEF +" + fmt3(defAfter - defNow)
                + ", REGEN +" + fmt3(regenAfter - regenNow) + "%";
    }

    private static double attackPowerRaw(int str, int agi, int wis, int luk) {
        return (effectivePrimary(StatType.STR, str) * 0.95)
                + (effectivePrimary(StatType.AGI, agi) * 0.18)
                + (effectivePrimary(StatType.LUK, luk) * 0.32);
    }

    private static double magicPowerRaw(int str, int agi, int wis, int luk) {
        return (effectivePrimary(StatType.WIS, wis) * 1.65)
                + (effectivePrimary(StatType.LUK, luk) * 0.22);
    }

    private static double maxHpRaw(int str, int agi, int wis, int luk, int level) {
        return clamp(20.0 + (level * 2.0)
                + (effectivePrimary(StatType.STR, str) * 2.0)
                + (effectivePrimary(StatType.LUK, luk) * 1.0), 20.0, 5000.0);
    }

    private static double maxMpRaw(int str, int agi, int wis, int luk, int level) {
        return clamp(20.0 + (level * 2.0)
                + (effectivePrimary(StatType.WIS, wis) * 6.0)
                + (effectivePrimary(StatType.LUK, luk) * 0.1), 0.0, 5000.0);
    }

    private static double critChanceRaw(int str, int agi, int wis, int luk) {
        return clamp(1.0
                + (effectivePrimary(StatType.STR, str) * 0.01)
                + (effectivePrimary(StatType.LUK, luk) * 0.020)
                + (effectivePrimary(StatType.AGI, agi) * 0.035), 0.0, 72.0);
    }

    private static double critDamageRaw(int str, int agi, int wis, int luk) {
        return clamp(100.0
                + (effectivePrimary(StatType.STR, str) * 0.08)
                + (effectivePrimary(StatType.LUK, luk) * 0.05)
                + (effectivePrimary(StatType.AGI, agi) * 0.03), 100.0, 380.0);
    }

    private static double moveSpeedRaw(int str, int agi, int wis, int luk) {
        return clamp(1.5
                + (effectivePrimary(StatType.AGI, agi) * 0.1)
                + (effectivePrimary(StatType.LUK, luk) * 0.02), 0.0, 500.0);
    }

    private static double attackSpeedRaw(int str, int agi, int wis, int luk) {
        return clamp((effectivePrimary(StatType.AGI, agi) * 0.05)
                + (effectivePrimary(StatType.LUK, luk) * 0.01), 0.0, 120.0);
    }

    private static double defenseRaw(int str, int agi, int wis, int luk) {
        return (effectivePrimary(StatType.STR, str) * 0.16)
                + (effectivePrimary(StatType.WIS, wis) * 0.07)
                + (effectivePrimary(StatType.LUK, luk) * 0.12);
    }

    private static double hpRegenRaw(int str, int agi, int wis, int luk) {
        return clamp((effectivePrimary(StatType.LUK, luk) * 0.12)
                + (effectivePrimary(StatType.STR, str) * 0.05)
                + (effectivePrimary(StatType.WIS, wis) * 0.07), 0.0, 180.0);
    }

    private static void validateRules() {
        if (RULES.size() != StatType.values().length) {
            throw new IllegalStateException("Rule definitions are missing for some primary stats.");
        }
        for (Map.Entry<StatType, Rule> entry : RULES.entrySet()) {
            Rule rule = entry.getValue();
            if (rule.softCap() <= 0 || rule.hardCap() <= 0 || rule.valuePerPoint() <= 0.0) {
                throw new IllegalStateException("Invalid cap/value setup for stat: " + entry.getKey());
            }
            if (rule.softCap() >= rule.hardCap()) {
                throw new IllegalStateException("softCap must be lower than hardCap for stat: " + entry.getKey());
            }
        }
    }

    private static Rule toRule(qorhvkdy.qorhvkdy.rpgmod.stats.data.StatsBalanceJson.CapEntry entry, int fallbackSoft, int fallbackHard) {
        if (entry == null) {
            return new Rule(fallbackSoft, fallbackHard, 1.0);
        }
        return new Rule(entry.softCap, entry.hardCap, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String fmt3(double value) {
        double normalized = Math.abs(value) < 0.0005 ? 0.0 : value;
        return StatsNumberFormat.formatFixed3(normalized);
    }
}
