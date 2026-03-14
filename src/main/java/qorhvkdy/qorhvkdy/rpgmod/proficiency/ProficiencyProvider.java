package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * PlayerProficiency capability 제공자.
 */
public class ProficiencyProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerProficiency proficiency = new PlayerProficiency();
    private final LazyOptional<PlayerProficiency> optional = LazyOptional.of(() -> proficiency);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ProficiencyCapability.PLAYER_PROFICIENCY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return ProficiencyStorage.save(proficiency, provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        ProficiencyStorage.load(proficiency, nbt, provider);
    }

    public void invalidate() {
        optional.invalidate();
    }
}

