package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import qorhvkdy.qorhvkdy.rpgmod.client.ClassModuleLayoutConfig;
import qorhvkdy.qorhvkdy.rpgmod.client.PermissionClientState;
import qorhvkdy.qorhvkdy.rpgmod.client.UiGridLayout;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.PermissionSyncRequestC2SPacket;

/**
 * 클래스 관리 메뉴 허브.
 * 메인 허브에서 클래스/숙련도 영역을 분리해 화면 복잡도를 줄인다.
 */
public class ClassModuleScreen extends Screen {
    private static final int BG_COLOR = 0xD0151D29;
    private static final int PANEL_COLOR = 0xB0213043;
    private static final int BORDER_COLOR = 0x90CFE8FF;
    private static final int TITLE_COLOR = 0xFFFFD166;

    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private long appliedLayoutVersion = -1L;
    private long appliedPermissionHash = 0L;

    public ClassModuleScreen(Screen parent) {
        super(Component.translatable("screen.rpgmod.class_module.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        applyLayout(true);
        ModNetwork.sendToServer(new PermissionSyncRequestC2SPacket());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyLayout(false);
        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        guiGraphics.fill(panelX + 8, panelY + 28, panelX + panelW - 8, panelY + panelH - 8, PANEL_COLOR);
        guiGraphics.renderOutline(panelX, panelY, panelW, panelH, BORDER_COLOR);
        guiGraphics.drawString(this.font, this.title, panelX + 12, panelY + 10, TITLE_COLOR, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyLayout(boolean forceRebuild) {
        ClassModuleLayoutConfig.Snapshot snapshot = ClassModuleLayoutConfig.snapshot();
        ClassModuleLayoutConfig.LayoutData layout = snapshot.layout();
        long version = snapshot.version();
        long permissionHash = PermissionClientState.get().hashCode();

        panelW = layout.panel.width;
        panelH = layout.panel.height;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        if (forceRebuild || appliedLayoutVersion != version || appliedPermissionHash != permissionHash) {
            clearWidgets();
            UiGridLayout grid = new UiGridLayout(
                    panelX + layout.buttons.left,
                    panelY + layout.buttons.top,
                    layout.buttons.width,
                    layout.buttons.height,
                    layout.buttons.colGap,
                    layout.buttons.rowGap
            );

            UiGridLayout.Rect row1Left = grid.rect(0, 0);
            UiGridLayout.Rect row1Right = grid.rect(1, 0);
            UiGridLayout.Rect row2Left = grid.rect(0, 1);
            UiGridLayout.Rect row2Right = grid.rect(1, 1);

            addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class_module.button.open_class"), b -> minecraft.setScreen(new ClassOverviewScreen(this)))
                    .bounds(row1Left.x(), row1Left.y(), row1Left.width(), row1Left.height()).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class_module.button.open_proficiency"), b -> minecraft.setScreen(new ProficiencyScreen(this)))
                    .bounds(row1Right.x(), row1Right.y(), row1Right.width(), row1Right.height()).build());

            Button permButton = Button.builder(Component.translatable("screen.rpgmod.class_module.button.open_perm"), b -> minecraft.setScreen(new PermHubScreen()))
                    .bounds(row2Left.x(), row2Left.y(), row2Left.width(), row2Left.height()).build();
            permButton.visible = PermissionClientState.get().permUiOpen();
            addRenderableWidget(permButton);
            addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class_module.button.weapon_coming"), b -> {
            }).bounds(row2Right.x(), row2Right.y(), row2Right.width(), row2Right.height()).build()).active = false;

            addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class.button.back"), b -> minecraft.setScreen(parent))
                    .bounds(panelX + panelW - 78, panelY + panelH - 26, 64, 16)
                    .build());
            appliedLayoutVersion = version;
            appliedPermissionHash = permissionHash;
        }
    }
}
