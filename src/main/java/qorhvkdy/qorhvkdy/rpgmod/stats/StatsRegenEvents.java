package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 체력 회복 증가 수치를 실제 회복 틱에 반영하는 이벤트 처리 클래스입니다.
 * 수정 예시: 회복 빈도를 줄이려면 틱 주기 조건을 더 크게 설정합니다.
 */


import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.combat.CombatTelemetryService;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsRegenEvents {
    private StatsRegenEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        CombatTelemetryService.tick();

        player.getCapability(StatsCapability.PLAYER_STATS).ifPresent(stats -> {
            ClassResourceService.regenerate(player, stats);
            float current = player.getHealth();
            float max = player.getMaxHealth();
            if (current >= max) {
                return;
            }

            float healAmount = (float) (0.2 + (stats.getHpRegenPercent() * 0.01));
            player.heal(healAmount);
        });
    }
}
