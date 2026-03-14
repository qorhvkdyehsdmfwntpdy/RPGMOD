package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/**
 * 숙련도 NBT 직렬화.
 * 스탯과 분리해 저장하므로 추후 데이터 마이그레이션 영향 범위를 줄인다.
 */
public final class ProficiencyStorage {
    private static final int DATA_VERSION = 1;

    private ProficiencyStorage() {
    }

    public static CompoundTag save(PlayerProficiency proficiency, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);
        for (ProficiencyType type : ProficiencyType.values()) {
            tag.putInt(type.key(), proficiency.getExp(type));
        }
        return tag;
    }

    public static void load(PlayerProficiency proficiency, CompoundTag tag, HolderLookup.Provider provider) {
        EnumMap<ProficiencyType, Integer> values = new EnumMap<>(ProficiencyType.class);
        for (ProficiencyType type : ProficiencyType.values()) {
            values.put(type, tag.getInt(type.key()).orElse(0));
        }
        proficiency.loadSnapshot(values);
    }
}

