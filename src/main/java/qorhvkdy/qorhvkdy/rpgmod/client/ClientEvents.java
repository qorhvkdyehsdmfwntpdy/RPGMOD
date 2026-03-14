package qorhvkdy.qorhvkdy.rpgmod.client;

/*
 * [RPGMOD 파일 설명]
 * 역할: 클라이언트 이벤트 버스 구독 및 화면 관련 이벤트 연결을 담당합니다.
 * 수정 예시: 새 렌더 이벤트가 필요하면 이 클래스에 핸들러를 추가합니다.
 */


import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.client.gui.ContentBoardScreen;
import qorhvkdy.qorhvkdy.rpgmod.client.gui.KeymapCenterScreen;
import qorhvkdy.qorhvkdy.rpgmod.client.gui.RpgHubScreen;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if ((event.getModifiers() & GLFW.GLFW_MOD_CONTROL) == 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (KeyBindings.OPEN_RPG_HUB.consumeClick()) {
            minecraft.setScreen(new RpgHubScreen());
            return;
        }
        if (KeyBindings.OPEN_CONTENT_BOARD.consumeClick()) {
            minecraft.setScreen(new ContentBoardScreen(null));
            return;
        }
        if (KeyBindings.OPEN_KEYMAP_CENTER.consumeClick()) {
            minecraft.setScreen(new KeymapCenterScreen(null));
        }
    }
}
