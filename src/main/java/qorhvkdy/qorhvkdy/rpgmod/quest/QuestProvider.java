package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 플레이어 퀘스트 진행도 capability provider.
 */
public class QuestProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerQuestProgress progress = new PlayerQuestProgress();
    private final LazyOptional<PlayerQuestProgress> optional = LazyOptional.of(() -> progress);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == QuestCapability.PLAYER_QUEST_PROGRESS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return QuestStorage.save(progress, provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        QuestStorage.load(progress, nbt, provider);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
