package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.client.PermissionClientState;
import qorhvkdy.qorhvkdy.rpgmod.client.RpgHubLayoutConfig;
import qorhvkdy.qorhvkdy.rpgmod.client.UiGridLayout;
import qorhvkdy.qorhvkdy.rpgmod.client.KeyBindings;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.PermissionSyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.network.StatsSyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsCapability;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main RPG hub screen.
 * Keep this as the entry screen for future modules (quest, inventory, skill, etc).
 */
public class RpgHubScreen extends Screen {
    private static final int BG_COLOR = 0xD0141B26;
    private static final int PANEL_COLOR = 0xB01D2A39;
    private static final int SECTION_COLOR = 0x90344456;
    private static final int BORDER_COLOR = 0x90CFE8FF;
    private static final int TITLE_COLOR = 0xFFFFD166;
    private static final int TEXT_COLOR = 0xFFE8F1FF;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int titleX;
    private int titleY;
    private int summaryX;
    private int summaryY;
    private int summaryLineStep;
    private int summaryColGap;
    private int sectionTop;
    private int sectionBottomPadding;
    private int innerPadding;
    private long appliedLayoutVersion = -1L;
    private long appliedPermissionHash = 0L;

    public RpgHubScreen() {
        super(Component.translatable("screen.rpgmod.hub.title"));
    }

    @Override
    protected void init() {
        super.init();
        applyLayout(true);
        ModNetwork.sendToServer(new StatsSyncRequestC2SPacket());
        ModNetwork.sendToServer(new PermissionSyncRequestC2SPacket());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyLayout(false);
        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        guiGraphics.fill(panelX + innerPadding, panelY + 28, panelX + panelW - innerPadding, panelY + panelH - innerPadding, PANEL_COLOR);
        guiGraphics.fill(panelX + innerPadding + 4, panelY + sectionTop, panelX + panelW - innerPadding - 4, panelY + panelH - sectionBottomPadding, SECTION_COLOR);
        guiGraphics.renderOutline(panelX, panelY, panelW, panelH, BORDER_COLOR);

        guiGraphics.drawString(this.font, this.title, panelX + titleX, panelY + titleY, TITLE_COLOR, false);
        drawSummary(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawSummary(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.common.player_not_found").getString(), panelX + summaryX, panelY + summaryY, TEXT_COLOR, false);
            return;
        }

        PlayerStats stats = player.getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
        if (stats == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.common.stats_not_available").getString(), panelX + summaryX, panelY + summaryY, TEXT_COLOR, false);
            return;
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.hub.summary.name", player.getName().getString()).getString(), panelX + summaryX, panelY + summaryY, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.hub.summary.level", player.experienceLevel).getString(), panelX + summaryX + summaryColGap, panelY + summaryY, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.hub.summary.base_class", stats.getSelectedClass().displayNameComponent()).getString(), panelX + summaryX, panelY + summaryY + summaryLineStep, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.hub.summary.advancement", stats.getCurrentAdvancement().displayNameComponent()).getString(), panelX + summaryX + summaryColGap, panelY + summaryY + summaryLineStep, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.hub.summary.system_ready", systemReadySummary()).getString(), panelX + summaryX, panelY + summaryY + summaryLineStep * 2, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.hub.summary.keymap", keymapSummary()).getString(), panelX + summaryX + summaryColGap, panelY + summaryY + summaryLineStep * 2, TEXT_COLOR, false);
    }

    private void applyLayout(boolean forceRebuild) {
        RpgHubLayoutConfig.Snapshot snapshot = RpgHubLayoutConfig.snapshot();
        RpgHubLayoutConfig.LayoutData layout = snapshot.layout();
        long version = snapshot.version();
        long permissionHash = PermissionClientState.get().hashCode();

        this.panelW = layout.panel.width;
        this.panelH = layout.panel.height;
        this.panelX = (this.width - panelW) / 2;
        this.panelY = (this.height - panelH) / 2;

        this.titleX = layout.panel.titleLeft;
        this.titleY = layout.panel.titleTop;
        this.innerPadding = layout.panel.innerPadding;
        this.sectionTop = layout.panel.sectionTop;
        this.sectionBottomPadding = layout.panel.sectionBottomPadding;

        this.summaryX = layout.summary.left;
        this.summaryY = layout.summary.top;
        this.summaryLineStep = layout.summary.lineStep;
        this.summaryColGap = layout.summary.colGap;

        if (forceRebuild || appliedLayoutVersion != version || appliedPermissionHash != permissionHash) {
            rebuildButtons(layout);
            appliedLayoutVersion = version;
            appliedPermissionHash = permissionHash;
        }
    }

    private void rebuildButtons(RpgHubLayoutConfig.LayoutData layout) {
        clearWidgets();

        int buttonW = layout.buttons.width;
        int buttonH = layout.buttons.height;
        UiGridLayout grid = new UiGridLayout(
                panelX + layout.buttons.startLeft,
                panelY + layout.buttons.startTop,
                buttonW,
                buttonH,
                layout.buttons.colGap,
                layout.buttons.rowGap
        );

        UiGridLayout.Rect row1Left = grid.rect(0, 0);
        UiGridLayout.Rect row1Right = grid.rect(1, 0);
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.hub.button.open_stats"), button -> minecraft.setScreen(new StatsScreen()))
                .bounds(row1Left.x(), row1Left.y(), row1Left.width(), row1Left.height())
                .build());
        Button classButton = Button.builder(Component.translatable("screen.rpgmod.hub.button.open_class_module"), button -> minecraft.setScreen(new ClassModuleScreen(this)))
                .bounds(row1Right.x(), row1Right.y(), row1Right.width(), row1Right.height())
                .build();
        classButton.visible = PermissionClientState.get().classAdmin() || PermissionClientState.get().partyManage() || PermissionClientState.get().permUiOpen();
        addRenderableWidget(classButton);

        UiGridLayout.Rect row2Left = grid.rect(0, 1);
        UiGridLayout.Rect row2Right = grid.rect(1, 1);
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.hub.button.open_content_board"), button -> minecraft.setScreen(new ContentBoardScreen(this)))
                .bounds(row2Left.x(), row2Left.y(), row2Left.width(), row2Left.height())
                .build());
        Button permButton = Button.builder(Component.translatable("screen.rpgmod.hub.button.open_perm"), button -> minecraft.setScreen(new PermHubScreen()))
                .bounds(row2Right.x(), row2Right.y(), row2Right.width(), row2Right.height())
                .build();
        permButton.visible = PermissionClientState.get().permUiOpen();
        addRenderableWidget(permButton);

        UiGridLayout.Rect row3Left = grid.rect(0, 2);
        UiGridLayout.Rect row3Right = grid.rect(1, 2);
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.hub.button.open_keymap_center"), button -> minecraft.setScreen(new KeymapCenterScreen(this)))
                .bounds(row3Left.x(), row3Left.y(), row3Left.width(), row3Left.height())
                .build());
        Button skillTreeButton = Button.builder(Component.translatable("screen.rpgmod.hub.button.open_skill_tree"), button -> minecraft.setScreen(new SkillTreeScreen(this)))
                .bounds(row3Right.x(), row3Right.y(), row3Right.width(), row3Right.height())
                .build();
        skillTreeButton.visible = PermissionClientState.get().skillTreeUiOpen() || PermissionClientState.get().classAdmin();
        addRenderableWidget(skillTreeButton);

        UiGridLayout.Rect row4Left = grid.rect(0, 3);
        UiGridLayout.Rect row4Right = grid.rect(1, 3);
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.hub.button.refresh"), button -> ModNetwork.sendToServer(new StatsSyncRequestC2SPacket()))
                .bounds(row4Left.x(), row4Left.y(), row4Left.width(), row4Left.height())
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.hub.button.open_quest_board"), button -> minecraft.setScreen(new QuestBoardScreen(this)))
                .bounds(row4Right.x(), row4Right.y(), row4Right.width(), row4Right.height())
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.common.close"), button -> onClose())
                .bounds(
                        panelX + panelW - layout.buttons.closeRightPadding - layout.buttons.closeWidth,
                        panelY + panelH - layout.buttons.closeBottomPadding - layout.buttons.closeHeight,
                        layout.buttons.closeWidth,
                        layout.buttons.closeHeight
                )
                .build());
    }

    private String systemReadySummary() {
        int total = 10;
        int ok = 0;
        if (Files.exists(Path.of("config", "rpgmod", "quests.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "skill-runtime.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "skill-tree.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "skill-chains.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "skill-motions.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "proficiency-sources.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "npc-dialogues.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "spell-schools.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "combat-profiles.json"))) ok++;
        if (Files.exists(Path.of("config", "rpgmod", "permission-context-priority.json"))) ok++;
        return ok + "/" + total;
    }

    private String keymapSummary() {
        return "Ctrl+" + KeyBindings.OPEN_RPG_HUB.getTranslatedKeyMessage().getString()
                + ", Ctrl+" + KeyBindings.OPEN_CONTENT_BOARD.getTranslatedKeyMessage().getString()
                + ", Ctrl+" + KeyBindings.OPEN_KEYMAP_CENTER.getTranslatedKeyMessage().getString();
    }
}
