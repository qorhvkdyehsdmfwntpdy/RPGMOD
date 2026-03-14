package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: PlayerStats capability 객체 제공과 NBT 직렬화를 담당합니다.
 * 수정 예시: 저장 필드가 늘면 serializeNBT/deserializeNBT 키를 함께 추가합니다.
 */


import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public class StatsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerStats stats = new PlayerStats();
    private final LazyOptional<PlayerStats> optional = LazyOptional.of(() -> stats);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == StatsCapability.PLAYER_STATS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return StatsStorage.save(stats, provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        StatsStorage.load(stats, nbt, provider);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
