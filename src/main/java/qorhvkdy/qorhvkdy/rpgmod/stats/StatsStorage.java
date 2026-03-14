package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: PlayerStats 데이터를 NBT로 읽고 쓰는 영속화 계층입니다.
 * 수정 예시: 새 필드 저장 시 write/read 둘 다 같은 키를 추가해야 합니다.
 */


import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;

public final class StatsStorage {
    public static final int DATA_VERSION = 6;

    private StatsStorage() {
    }

    public static CompoundTag save(PlayerStats stats, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);
        for (StatType type : StatType.values()) {
            tag.putInt(type.name(), stats.get(type));
        }
        tag.putInt("availablePoints", stats.getAvailableStatPoints());
        tag.putInt("lastTrackedLevel", stats.getLastTrackedLevel());
        tag.putString("selectedClass", stats.getSelectedClass().id());
        tag.putString("currentAdvancementId", stats.getCurrentAdvancementId());
        tag.putString("classResourceType", stats.getClassResourceType());
        tag.putDouble("classResourceCurrent", stats.getClassResourceCurrent());
        tag.putDouble("classResourceMax", stats.getClassResourceMax());
        CompoundTag passiveTag = new CompoundTag();
        stats.getPassiveSlots().forEach(passiveTag::putString);
        tag.put("passiveSlots", passiveTag);
        return tag;
    }

    public static void load(PlayerStats stats, CompoundTag tag, HolderLookup.Provider provider) {
        int version = tag.getInt("dataVersion").orElse(1);
        migrate(tag, version);

        for (StatType type : StatType.values()) {
            tag.getInt(type.name()).ifPresent(value -> stats.set(type, value));
        }
        tag.getInt("availablePoints").ifPresent(stats::setAvailableStatPoints);
        tag.getInt("lastTrackedLevel").ifPresent(stats::setLastTrackedLevel);
        tag.getString("selectedClass")
                .flatMap(PlayerClassType::fromId)
                .ifPresent(stats::setSelectedClass);
        tag.getString("currentAdvancementId").ifPresent(stats::setCurrentAdvancementId);
        tag.getString("classResourceType").ifPresent(stats::setClassResourceType);
        tag.getDouble("classResourceMax").ifPresent(stats::setClassResourceMax);
        tag.getDouble("classResourceCurrent").ifPresent(stats::setClassResourceCurrent);
        tag.getCompound("passiveSlots").ifPresent(passiveTag -> {
            java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
            for (String key : passiveTag.keySet()) {
                values.put(key, passiveTag.getString(key).orElse(""));
            }
            stats.applyPassiveSlots(values);
        });
    }

    private static void migrate(CompoundTag tag, int version) {
        if (version >= DATA_VERSION) {
            return;
        }

        // v1 -> v2: map historical keys if present.
        if (version == 1) {
            remapIfPresent(tag, "STR", StatType.STR.name());
            remapIfPresent(tag, "AGI", StatType.AGI.name());
            remapIfPresent(tag, "WIS", StatType.WIS.name());
            remapIfPresent(tag, "LUK", StatType.LUK.name());
            tag.putInt("dataVersion", 2);
            version = 2;
        }

        // v2 -> v3: add selectedClass with explicit default.
        if (version == 2) {
            if (tag.getString("selectedClass").isEmpty()) {
                tag.putString("selectedClass", PlayerClassType.NONE.id());
            }
            tag.putInt("dataVersion", 3);
            version = 3;
        }

        // v3 -> v4: add current advancement id.
        if (version == 3) {
            if (tag.getString("currentAdvancementId").isEmpty()) {
                tag.putString("currentAdvancementId", "none");
            }
            tag.putInt("dataVersion", 4);
            version = 4;
        }

        // v4 -> v5: add passive slot map.
        if (version == 4) {
            if (!tag.contains("passiveSlots")) {
                tag.put("passiveSlots", new CompoundTag());
            }
            tag.putInt("dataVersion", 5);
            version = 5;
        }

        // v5 -> v6: add class resource fields.
        if (version == 5) {
            if (tag.getString("classResourceType").isEmpty()) {
                tag.putString("classResourceType", "none");
            }
            if (!tag.contains("classResourceCurrent")) {
                tag.putDouble("classResourceCurrent", 0.0);
            }
            if (!tag.contains("classResourceMax")) {
                tag.putDouble("classResourceMax", 0.0);
            }
            tag.putInt("dataVersion", 6);
        }
    }

    private static void remapIfPresent(CompoundTag tag, String oldKey, String newKey) {
        tag.getInt(oldKey).ifPresent(value -> tag.putInt(newKey, value));
    }
}
