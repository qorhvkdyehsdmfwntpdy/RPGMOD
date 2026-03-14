package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 레벨업/전투 등 게임플레이 시점의 스탯 포인트 처리 로직입니다.
 * 수정 예시: 레벨당 지급 포인트를 바꾸려면 지급 상수값을 조정합니다.
 */


import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Config;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.SkillTreeSyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.skill.tree.SkillTreeService;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsGameplayEvents {
    private StatsGameplayEvents() {
    }

    @SubscribeEvent
    public static void onLevelChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
            int currentLevel = Math.max(0, serverPlayer.experienceLevel);
            int trackedLevel = stats.getLastTrackedLevel();

            if (currentLevel > trackedLevel) {
                StatsSnapshotService.snapshot(serverPlayer, stats, "before level up grant");
                int gainedLevels = currentLevel - trackedLevel;
                int grantedPoints = gainedLevels * Math.max(0, Config.levelUpStatPoints);
                stats.grantPoints(grantedPoints);

                // 스킬 트리 포인트는 5레벨 구간 진입마다 1 지급.
                int oldTier = Math.max(0, trackedLevel / 5);
                int newTier = Math.max(0, currentLevel / 5);
                int grantedSkillTreePoints = Math.max(0, newTier - oldTier);
                if (grantedSkillTreePoints > 0) {
                    SkillTreeService.grantPoints(serverPlayer, grantedSkillTreePoints);
                    SkillTreeSyncRequestC2SPacket.sync(serverPlayer);
                }

                stats.setLastTrackedLevel(currentLevel);
                StatsHistoryService.log(serverPlayer,
                        "Level up points granted: +" + grantedPoints
                                + ", skillTree=+" + grantedSkillTreePoints
                                + " (level " + trackedLevel + " -> " + currentLevel + ")");
            } else if (currentLevel < trackedLevel) {
                stats.setLastTrackedLevel(currentLevel);
                StatsHistoryService.log(serverPlayer, "Level decreased: " + trackedLevel + " -> " + currentLevel);
            }

            StatsAttributeService.apply(serverPlayer, stats);
            ClassResourceService.updateProfile(serverPlayer, stats, false);
            ModNetwork.syncToPlayer(serverPlayer, stats);
        });
    }
}
