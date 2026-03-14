package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * 숙련도 Capability 키.
 */
public final class ProficiencyCapability {
    public static final Capability<PlayerProficiency> PLAYER_PROFICIENCY = CapabilityManager.get(new CapabilityToken<>() {});

    private ProficiencyCapability() {
    }
}

