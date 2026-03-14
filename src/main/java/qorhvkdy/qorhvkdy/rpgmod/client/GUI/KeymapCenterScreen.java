package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import qorhvkdy.qorhvkdy.rpgmod.client.KeyBindings;

import java.util.List;

/**
 * 키매핑 센터.
 * 기본 프리셋 + 개별 키 변경을 빠르게 적용한다.
 */
public class KeymapCenterScreen extends Screen {
    private final Screen parent;
    private int hubIndex = 0;
    private int boardIndex = 0;
    private int centerIndex = 0;

    private static final List<Integer> HUB_KEYS = List.of(GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J);
    private static final List<Integer> BOARD_KEYS = List.of(GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_V);
    private static final List<Integer> CENTER_KEYS = List.of(GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_L);

    public KeymapCenterScreen(Screen parent) {
        super(Component.translatable("screen.rpgmod.keymap.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        hubIndex = 0;
        boardIndex = 0;
        centerIndex = 0;

        int panelW = 360;
        int panelH = 210;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.keymap.button.cycle_hub"), b -> {
            hubIndex = (hubIndex + 1) % HUB_KEYS.size();
            applyKey(KeyBindings.OPEN_RPG_HUB, HUB_KEYS.get(hubIndex));
        }).bounds(x + 14, y + 86, 106, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.keymap.button.cycle_board"), b -> {
            boardIndex = (boardIndex + 1) % BOARD_KEYS.size();
            applyKey(KeyBindings.OPEN_CONTENT_BOARD, BOARD_KEYS.get(boardIndex));
        }).bounds(x + 126, y + 86, 106, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.keymap.button.cycle_center"), b -> {
            centerIndex = (centerIndex + 1) % CENTER_KEYS.size();
            applyKey(KeyBindings.OPEN_KEYMAP_CENTER, CENTER_KEYS.get(centerIndex));
        }).bounds(x + 238, y + 86, 106, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.keymap.button.preset_default"), b -> applyPreset(GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_K))
                .bounds(x + 14, y + 110, 108, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.keymap.button.preset_alt"), b -> applyPreset(GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M))
                .bounds(x + 128, y + 110, 108, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.keymap.button.preset_onehand"), b -> applyPreset(GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_L))
                .bounds(x + 242, y + 110, 102, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.common.close"), b -> onClose())
                .bounds(x + panelW - 74, y + panelH - 28, 60, 18).build());
    }

    private void applyPreset(int hub, int board, int center) {
        applyKey(KeyBindings.OPEN_RPG_HUB, hub);
        applyKey(KeyBindings.OPEN_CONTENT_BOARD, board);
        applyKey(KeyBindings.OPEN_KEYMAP_CENTER, center);
    }

    private void applyKey(KeyMapping mapping, int keyCode) {
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        KeyMapping.resetMapping();
        if (this.minecraft != null) {
            this.minecraft.options.save();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x70000000);
        int panelW = 360;
        int panelH = 210;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        guiGraphics.fill(x, y, x + panelW, y + panelH, 0xD0141B26);
        guiGraphics.fill(x + 8, y + 24, x + panelW - 8, y + panelH - 34, 0xB01D2A39);
        guiGraphics.renderOutline(x, y, panelW, panelH, 0x90CFE8FF);

        guiGraphics.drawString(this.font, this.title, x + 12, y + 8, 0xFFFFD166, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.keymap.desc").getString(), x + 14, y + 30, 0xFFE8F1FF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.keymap.row.hub", "Ctrl+" + KeyBindings.OPEN_RPG_HUB.getTranslatedKeyMessage().getString()).getString(), x + 14, y + 52, 0xFFE8F1FF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.keymap.row.board", "Ctrl+" + KeyBindings.OPEN_CONTENT_BOARD.getTranslatedKeyMessage().getString()).getString(), x + 14, y + 64, 0xFFE8F1FF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.keymap.row.center", "Ctrl+" + KeyBindings.OPEN_KEYMAP_CENTER.getTranslatedKeyMessage().getString()).getString(), x + 14, y + 76, 0xFFE8F1FF, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
