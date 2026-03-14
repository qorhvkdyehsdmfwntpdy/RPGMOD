package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 기본 스탯/포인트/캐시된 파생 스탯을 보관하는 핵심 상태 모델입니다.
 * 수정 예시: 새 파생 능력치를 추가하려면 캐시 필드와 recompute 로직을 같이 추가합니다.
 */


import qorhvkdy.qorhvkdy.rpgmod.Config;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancement;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancementRegistry;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassProfile;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassProfileRegistry;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerStats {
    private final EnumMap<StatType, Integer> stats = new EnumMap<>(StatType.class);
    private double cachedCritChancePercent;
    private double cachedCritDamagePercent;
    private double cachedMoveSpeedPercent;
    private double cachedAttackSpeedPercent;
    private double cachedAttackPower;
    private double cachedMagicPower;
    private double cachedDefense;
    private double cachedHpRegenPercent;
    private int availableStatPoints;
    private int lastTrackedLevel;
    private PlayerClassType selectedClass = PlayerClassType.NONE;
    private String currentAdvancementId = "none";
    private String classResourceType = "none";
    private double classResourceCurrent = 0.0;
    private double classResourceMax = 0.0;
    private final Map<String, String> passiveSlots = new LinkedHashMap<>();

    public PlayerStats() {
        for (StatType type : StatType.values()) {
            stats.put(type, Config.getDefaultStat(type));
        }
        recomputeCachedDerivedStats();
    }

    public int get(StatType type) {
        return stats.get(type);
    }

    public int getStr() {
        return get(StatType.STR);
    }

    public int getAgi() {
        return get(StatType.AGI);
    }

    public int getWis() {
        return get(StatType.WIS);
    }

    public int getLuk() {
        return get(StatType.LUK);
    }

    public void set(StatType type, int value) {
        int clamped = StatFormulas.clampPoints(type, value);
        Integer previous = stats.put(type, clamped);
        if (previous == null || previous != clamped) {
            recomputeCachedDerivedStats();
        }
    }

    public int getAvailableStatPoints() {
        return availableStatPoints;
    }

    public void setAvailableStatPoints(int points) {
        availableStatPoints = Math.max(0, points);
    }

    public int getLastTrackedLevel() {
        return lastTrackedLevel;
    }

    public void setLastTrackedLevel(int level) {
        lastTrackedLevel = Math.max(0, level);
    }

    public void grantPoints(int amount) {
        if (amount > 0) {
            availableStatPoints += amount;
        }
    }

    public PlayerClassType getSelectedClass() {
        return selectedClass;
    }

    public void setSelectedClass(PlayerClassType selectedClass) {
        this.selectedClass = selectedClass == null ? PlayerClassType.NONE : selectedClass;
        this.currentAdvancementId = ClassAdvancementRegistry.defaultBaseAdvancement(this.selectedClass).id();
    }

    public ClassProfile getClassProfile() {
        return ClassProfileRegistry.get(selectedClass);
    }

    public String getCurrentAdvancementId() {
        return currentAdvancementId;
    }

    public ClassAdvancement getCurrentAdvancement() {
        return ClassAdvancementRegistry.get(currentAdvancementId)
                .orElseGet(() -> ClassAdvancementRegistry.defaultBaseAdvancement(selectedClass));
    }

    public void setCurrentAdvancementId(String advancementId) {
        String fallback = ClassAdvancementRegistry.defaultBaseAdvancement(selectedClass).id();
        this.currentAdvancementId = ClassAdvancementRegistry.get(advancementId)
                .filter(node -> node.baseClass() == selectedClass)
                .map(ClassAdvancement::id)
                .orElse(fallback);
    }

    public Map<String, String> getPassiveSlots() {
        return Map.copyOf(passiveSlots);
    }

    public void setPassiveSlot(String slot, String passiveId) {
        if (slot == null || slot.isBlank()) {
            return;
        }
        passiveSlots.put(slot.trim().toLowerCase(), passiveId == null ? "" : passiveId.trim().toLowerCase());
    }

    public void applyPassiveSlots(Map<String, String> values) {
        passiveSlots.clear();
        if (values == null) {
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            setPassiveSlot(entry.getKey(), entry.getValue());
        }
    }

    public boolean canPromoteTo(String advancementId, int playerLevel) {
        return ClassAdvancementRegistry.canPromote(currentAdvancementId, advancementId, playerLevel);
    }

    public boolean promoteTo(String advancementId, int playerLevel) {
        if (!canPromoteTo(advancementId, playerLevel)) {
            return false;
        }
        this.currentAdvancementId = ClassAdvancement.normalizeId(advancementId);
        return true;
    }

    public boolean canIncrease(StatType type) {
        if (availableStatPoints <= 0) {
            return false;
        }
        return get(type) < StatFormulas.rule(type).hardCap();
    }

    public boolean increaseStat(StatType type) {
        return increaseStatMany(type, 1) > 0;
    }

    /*
     * 한 번에 여러 포인트를 투자해 재계산 횟수를 줄입니다.
     * 반환값은 실제로 적용된 포인트 수입니다.
     */
    public int increaseStatMany(StatType type, int amount) {
        if (amount <= 0 || availableStatPoints <= 0) {
            return 0;
        }

        int current = get(type);
        int hardCap = StatFormulas.rule(type).hardCap();
        int maxByCap = Math.max(0, hardCap - current);
        int applied = Math.min(amount, Math.min(availableStatPoints, maxByCap));
        if (applied <= 0) {
            return 0;
        }

        stats.put(type, current + applied);
        availableStatPoints -= applied;
        recomputeCachedDerivedStats();
        return applied;
    }

    public boolean decreaseStat(StatType type) {
        return decreaseStatMany(type, 1) > 0;
    }

    /*
     * 한 번에 여러 포인트를 회수해 재계산 횟수를 줄입니다.
     * 반환값은 실제로 회수된 포인트 수입니다.
     */
    public int decreaseStatMany(StatType type, int amount) {
        if (amount <= 0) {
            return 0;
        }

        int min = Config.getDefaultStat(type);
        int current = get(type);
        int maxDecrease = Math.max(0, current - min);
        int applied = Math.min(amount, maxDecrease);
        if (applied <= 0) {
            return 0;
        }

        stats.put(type, current - applied);
        availableStatPoints += applied;
        recomputeCachedDerivedStats();
        return applied;
    }

    public void resetAllocatedStats() {
        int refund = 0;
        boolean changed = false;
        for (StatType type : StatType.values()) {
            int base = Config.getDefaultStat(type);
            int current = get(type);
            if (current > base) {
                refund += current - base;
            }
            if (current != base) {
                stats.put(type, base);
                changed = true;
            }
        }
        availableStatPoints += refund;
        if (changed) {
            recomputeCachedDerivedStats();
        }
    }

    /*
     * 롤백/복구 시 스냅샷을 한 번에 적용합니다.
     * set(...) 반복 호출 대신 단일 재계산으로 성능을 안정화합니다.
     */
    public void applySnapshot(Map<StatType, Integer> values, int points, int trackedLevel) {
        boolean changed = false;
        for (StatType type : StatType.values()) {
            int value = StatFormulas.clampPoints(type, values.getOrDefault(type, Config.getDefaultStat(type)));
            int current = get(type);
            if (current != value) {
                stats.put(type, value);
                changed = true;
            }
        }

        availableStatPoints = Math.max(0, points);
        lastTrackedLevel = Math.max(0, trackedLevel);
        if (changed) {
            recomputeCachedDerivedStats();
        }
    }

    public int getMaxHP(int level) {
        return StatFormulas.maxHp(this, level);
    }

    public int getMaxMP(int level) {
        return StatFormulas.maxMp(this, level);
    }

    public double getCritChance() {
        return cachedCritChancePercent;
    }

    public double getCritDamage() {
        return cachedCritDamagePercent;
    }

    public double getCritDamageMultiplier() {
        return cachedCritDamagePercent / 100.0;
    }

    public double getMoveSpeedPercent() {
        return cachedMoveSpeedPercent;
    }

    public double getAttackSpeedPercent() {
        return cachedAttackSpeedPercent;
    }

    public double getAttackPower() {
        return cachedAttackPower;
    }

    public double getMagicPower() {
        return cachedMagicPower;
    }

    public double getDefense() {
        return cachedDefense;
    }

    public double getHpRegenPercent() {
        return cachedHpRegenPercent;
    }

    public String getClassResourceType() {
        return classResourceType;
    }

    public void setClassResourceType(String type) {
        this.classResourceType = normalizeResourceType(type);
    }

    public double getClassResourceCurrent() {
        return classResourceCurrent;
    }

    public double getClassResourceMax() {
        return classResourceMax;
    }

    public void setClassResourceMax(double max) {
        this.classResourceMax = Math.max(0.0, max);
        if (classResourceCurrent > classResourceMax) {
            classResourceCurrent = classResourceMax;
        }
    }

    public void setClassResourceCurrent(double current) {
        this.classResourceCurrent = clampResource(current);
    }

    public void refillClassResource() {
        classResourceCurrent = classResourceMax;
    }

    public boolean consumeClassResource(double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (classResourceCurrent < amount) {
            return false;
        }
        classResourceCurrent -= amount;
        return true;
    }

    public void gainClassResource(double amount) {
        if (amount <= 0.0) {
            return;
        }
        classResourceCurrent = clampResource(classResourceCurrent + amount);
    }

    private void recomputeCachedDerivedStats() {
        cachedMoveSpeedPercent = StatFormulas.moveSpeedPercent(this);
        cachedAttackSpeedPercent = StatFormulas.attackSpeedPercent(this);
        cachedAttackPower = StatFormulas.attackPower(this);
        cachedMagicPower = StatFormulas.magicPower(this);
        cachedDefense = StatFormulas.defense(this);
        cachedHpRegenPercent = StatFormulas.hpRegenPercent(this);
        cachedCritChancePercent = StatFormulas.critChancePercent(this);
        cachedCritDamagePercent = StatFormulas.critDamagePercent(this);
    }

    private double clampResource(double value) {
        return Math.max(0.0, Math.min(classResourceMax, value));
    }

    private static String normalizeResourceType(String raw) {
        return raw == null ? "none" : raw.trim().toLowerCase();
    }
}
