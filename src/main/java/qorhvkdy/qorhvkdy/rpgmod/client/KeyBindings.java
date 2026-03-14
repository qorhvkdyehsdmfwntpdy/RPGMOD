package qorhvkdy.qorhvkdy.rpgmod.client;

/*
 * [RPGMOD 파일 설명]
 * 역할: 모드 키 바인딩 정의와 등록 헬퍼를 제공합니다.
 * 수정 예시: 기본 단축키를 바꾸려면 키 매핑 기본 키코드를 수정합니다.
 */


import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

import java.util.Objects;

public final class KeyBindings {
    private static final KeyMapping.Category RPGMOD_CATEGORY =
            KeyMapping.Category.register(Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":keys")));

    public static final KeyMapping OPEN_RPG_HUB = new KeyMapping(
            "key.rpgmod.open_rpg_hub",
            GLFW.GLFW_KEY_F,
            RPGMOD_CATEGORY
    );
    public static final KeyMapping OPEN_CONTENT_BOARD = new KeyMapping(
            "key.rpgmod.open_content_board",
            GLFW.GLFW_KEY_B,
            RPGMOD_CATEGORY
    );
    public static final KeyMapping OPEN_KEYMAP_CENTER = new KeyMapping(
            "key.rpgmod.open_keymap_center",
            GLFW.GLFW_KEY_K,
            RPGMOD_CATEGORY
    );

    private KeyBindings() {
    }
}
