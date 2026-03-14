package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 스탯 관련 공통 이벤트 핸들러를 등록/중계합니다.
 * 수정 예시: 새 이벤트 처리기를 추가할 때 버스 타입(FORGE/MOD)을 정확히 맞춥니다.
 */


import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsEvents {
    private static final Identifier STATS_ID = Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":stats"));

    private StatsEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(STATS_ID, new StatsProvider());
        }
    }
}
