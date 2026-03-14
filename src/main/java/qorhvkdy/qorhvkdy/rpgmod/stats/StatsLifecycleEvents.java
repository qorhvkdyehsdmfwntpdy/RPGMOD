package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 접속/리스폰/클론 시점의 스탯 복사 및 초기화를 처리합니다.
 * 수정 예시: 사망 후 유지 규칙을 바꾸려면 클론 이벤트 복사 로직을 수정합니다.
 */


import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsLifecycleEvents {
    private StatsLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            try {
                PlayerStats oldStats = event.getOriginal().getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
                PlayerStats newStats = event.getEntity().getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
                if (oldStats != null && newStats != null) {
                    copyStats(oldStats, newStats);
                }
            } finally {
                event.getOriginal().invalidateCaps();
            }
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            event.getEntity().getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
                if (stats.getLastTrackedLevel() == 0 && serverPlayer.experienceLevel > 0) {
                    stats.setLastTrackedLevel(serverPlayer.experienceLevel);
                }
                ClassResourceService.updateProfile(serverPlayer, stats, false);
                StatsAttributeService.apply(serverPlayer, stats);
                ModNetwork.syncToPlayer(serverPlayer, stats);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            event.getEntity().getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
                if (stats.getLastTrackedLevel() == 0 && serverPlayer.experienceLevel > 0) {
                    stats.setLastTrackedLevel(serverPlayer.experienceLevel);
                }
                ClassResourceService.updateProfile(serverPlayer, stats, false);
                StatsAttributeService.apply(serverPlayer, stats);
                ModNetwork.syncToPlayer(serverPlayer, stats);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            event.getEntity().getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
                ClassResourceService.updateProfile(serverPlayer, stats, false);
                StatsAttributeService.apply(serverPlayer, stats);
                ModNetwork.syncToPlayer(serverPlayer, stats);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            event.getEntity().getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
                ClassResourceService.updateProfile(serverPlayer, stats, false);
                StatsAttributeService.apply(serverPlayer, stats);
                ModNetwork.syncToPlayer(serverPlayer, stats);
            });
        }
    }

    private static void copyStats(PlayerStats source, PlayerStats target) {
        for (StatType type : StatType.values()) {
            target.set(type, source.get(type));
        }
        target.setAvailableStatPoints(source.getAvailableStatPoints());
        target.setLastTrackedLevel(source.getLastTrackedLevel());
        target.setSelectedClass(source.getSelectedClass());
        target.setCurrentAdvancementId(source.getCurrentAdvancementId());
        target.setClassResourceType(source.getClassResourceType());
        target.setClassResourceMax(source.getClassResourceMax());
        target.setClassResourceCurrent(source.getClassResourceCurrent());
        target.applyPassiveSlots(source.getPassiveSlots());
    }
}
