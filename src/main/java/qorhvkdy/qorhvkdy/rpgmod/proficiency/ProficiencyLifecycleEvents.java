package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;

/**
 * 접속/리스폰 시 숙련도 동기화 이벤트.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProficiencyLifecycleEvents {
    private ProficiencyLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            try {
                PlayerProficiency oldData = event.getOriginal().getCapability(ProficiencyCapability.PLAYER_PROFICIENCY).resolve().orElse(null);
                PlayerProficiency newData = event.getEntity().getCapability(ProficiencyCapability.PLAYER_PROFICIENCY).resolve().orElse(null);
                if (oldData != null && newData != null) {
                    newData.loadSnapshot(oldData.snapshotExp());
                }
            } finally {
                event.getOriginal().invalidateCaps();
            }
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            event.getEntity().getCapability(ProficiencyCapability.PLAYER_PROFICIENCY)
                    .ifPresent(data -> ModNetwork.syncProficiencyToPlayer(serverPlayer, data));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            event.getEntity().getCapability(ProficiencyCapability.PLAYER_PROFICIENCY)
                    .ifPresent(data -> ModNetwork.syncProficiencyToPlayer(serverPlayer, data));
        }
    }
}
