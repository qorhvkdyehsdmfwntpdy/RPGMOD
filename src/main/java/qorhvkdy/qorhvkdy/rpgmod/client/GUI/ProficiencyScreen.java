package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.client.ProficiencyLayoutConfig;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.ProficiencySyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.PlayerProficiency;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyCapability;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyType;

/**
 * 숙련도 확인 화면.
 */
public class ProficiencyScreen extends Screen {
    private static final int BG_COLOR = 0xD0141B26;
    private static final int PANEL_COLOR = 0xB01D2A39;
    private static final int BORDER_COLOR = 0x90CFE8FF;
    private static final int TITLE_COLOR = 0xFFFFD166;
    private static final int TEXT_COLOR = 0xFFE8F1FF;

    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private ProficiencyLayoutConfig.LayoutData layout;
    private long appliedVersion = -1L;

    public ProficiencyScreen(Screen parent) {
        super(Component.translatable("screen.rpgmod.proficiency.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        applyLayout(true);
        ModNetwork.sendToServer(new ProficiencySyncRequestC2SPacket());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyLayout(false);
        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        guiGraphics.fill(panelX + 8, panelY + 28, panelX + panelW - 8, panelY + panelH - 8, PANEL_COLOR);
        guiGraphics.renderOutline(panelX, panelY, panelW, panelH, BORDER_COLOR);
        guiGraphics.drawString(this.font, this.title, panelX + layout.panel.titleLeft, panelY + layout.panel.titleTop, TITLE_COLOR, false);

        drawRows(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawRows(GuiGraphics gg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            gg.drawString(this.font, Component.translatable("screen.rpgmod.common.player_not_found").getString(), panelX + 14, panelY + 34, TEXT_COLOR, false);
            return;
        }
        PlayerProficiency proficiency = player.getCapability(ProficiencyCapability.PLAYER_PROFICIENCY).resolve().orElse(null);
        if (proficiency == null) {
            gg.drawString(this.font, Component.translatable("screen.rpgmod.proficiency.not_available").getString(), panelX + 14, panelY + 34, TEXT_COLOR, false);
            return;
        }

        int y = panelY + layout.table.top;
        for (ProficiencyType type : ProficiencyType.values()) {
            int level = proficiency.getLevel(type);
            int current = proficiency.getExpIntoCurrentLevel(type);
            int next = proficiency.getExpForNextLevel(type);
            gg.drawString(this.font, Component.translatable("screen.rpgmod.proficiency.type." + type.key()).getString(), panelX + layout.table.left, y, TEXT_COLOR, false);
            gg.drawString(this.font, "Lv." + level, panelX + layout.table.levelCol, y, 0xFFFFD166, false);
            gg.drawString(this.font, current + " / " + next, panelX + layout.table.expCol, y, 0xFFB8C0CC, false);
            y += layout.table.rowStep;
        }
    }

    private void applyLayout(boolean forceRebuild) {
        ProficiencyLayoutConfig.Snapshot snapshot = ProficiencyLayoutConfig.snapshot();
        this.layout = snapshot.layout();
        long version = snapshot.version();
        panelW = layout.panel.width;
        panelH = layout.panel.height;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        if (forceRebuild || version != appliedVersion) {
            clearWidgets();
            addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class.button.back"), b -> minecraft.setScreen(parent))
                    .bounds(
                            panelX + panelW - layout.buttons.backRightPadding - layout.buttons.backWidth,
                            panelY + panelH - layout.buttons.backBottomPadding - layout.buttons.backHeight,
                            layout.buttons.backWidth,
                            layout.buttons.backHeight
                    ).build());
            appliedVersion = version;
        }
    }
}

