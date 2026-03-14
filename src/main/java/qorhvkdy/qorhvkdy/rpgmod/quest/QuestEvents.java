package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

import java.util.Objects;

/**
 * 플레이어 퀘스트 capability 부착 이벤트.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestEvents {
    private static final Identifier QUEST_ID = Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":quest_progress"));

    private QuestEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(QUEST_ID, new QuestProvider());
        }
    }
}
