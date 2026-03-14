package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

import java.util.Objects;

/**
 * 플레이어 숙련도 capability 부착 이벤트.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProficiencyEvents {
    private static final Identifier PROFICIENCY_ID = Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":proficiency"));

    private ProficiencyEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PROFICIENCY_ID, new ProficiencyProvider());
        }
    }
}

