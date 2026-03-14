package qorhvkdy.qorhvkdy.rpgmod.client;

/*
 * [RPGMOD 파일 설명]
 * 역할: 클라이언트 초기 등록(키 바인딩/스크린 연동 등)을 처리합니다.
 * 수정 예시: GUI 단축키를 추가할 때 setup 단계 등록 코드를 넣습니다.
 */


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_RPG_HUB);
        event.register(KeyBindings.OPEN_CONTENT_BOARD);
        event.register(KeyBindings.OPEN_KEYMAP_CENTER);
    }
}
