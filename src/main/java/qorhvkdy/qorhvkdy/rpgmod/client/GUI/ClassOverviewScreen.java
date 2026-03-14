package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancement;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancementRegistry;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.client.ClassOverviewLayoutConfig;
import qorhvkdy.qorhvkdy.rpgmod.client.UiGridLayout;
import qorhvkdy.qorhvkdy.rpgmod.network.ClassActionC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.StatsSyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsCapability;

import java.util.List;

/**
 * Class control panel.
 * Supports promotion actions directly from GUI.
 */
public class ClassOverviewScreen extends Screen {
    private static final int BG_COLOR = 0xE0151D29;
    private static final int PANEL_COLOR = 0xB8263850;
    private static final int BORDER_COLOR = 0xB0D8EEFF;
    private static final int TITLE_COLOR = 0xFFFFD166;
    private static final int TEXT_COLOR = 0xFFE8F1FF;

    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int titleX;
    private int titleY;
    private int innerPadding;
    private int infoX;
    private int infoY;
    private int lineStep;
    private int headerGap;
    private int promoteX;
    private int promoteY;
    private int promoteWidth;
    private int promoteHeight;
    private int promoteGap;
    private int backWidth;
    private int backHeight;
    private int backRightPadding;
    private int backBottomPadding;
    private long appliedLayoutVersion = -1L;
    private String lastAdvancementId = "";
    private int lastLevel = -1;

    public ClassOverviewScreen(Screen parent) {
        super(Component.translatable("screen.rpgmod.class.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        applyLayout(true);
        ModNetwork.sendToServer(new StatsSyncRequestC2SPacket());
    }

    private void rebuildButtons() {
        clearWidgets();

        UiGridLayout footerGrid = new UiGridLayout(
                panelX + panelW - backRightPadding - (backWidth * 2 + 6),
                panelY + panelH - backBottomPadding - backHeight,
                backWidth,
                backHeight,
                6,
                0
        );
        UiGridLayout.Rect refreshRect = footerGrid.rect(0, 0);
        UiGridLayout.Rect backRect = footerGrid.rect(1, 0);

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class.button.back"), button -> minecraft.setScreen(parent))
                .bounds(backRect.x(), backRect.y(), backRect.width(), backRect.height())
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class.button.refresh"), button -> ModNetwork.sendToServer(new StatsSyncRequestC2SPacket()))
                .bounds(refreshRect.x(), refreshRect.y(), refreshRect.width(), refreshRect.height())
                .build());

        addPromotionButtons();
    }

    private void addPromotionButtons() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        PlayerStats stats = player.getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
        if (stats == null) {
            return;
        }

        ClassAdvancement current = stats.getCurrentAdvancement();
        List<ClassAdvancement> unlocked = ClassAdvancementRegistry.nextOptions(current.id(), player.experienceLevel);
        int y = panelY + promoteY;
        int x = panelX + promoteX;
        int width = Math.min(promoteWidth, panelW - (promoteX * 2));
        UiGridLayout promoteGrid = new UiGridLayout(x, y, width, promoteHeight, 0, promoteGap);

        if (unlocked.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.class.button.no_unlocked"), button -> {
            }).bounds(promoteGrid.rect(0, 0).x(), promoteGrid.rect(0, 0).y(), width, promoteHeight).build()).active = false;
            return;
        }

        int row = 0;
        for (ClassAdvancement node : unlocked) {
            UiGridLayout.Rect rect = promoteGrid.rect(0, row);
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.rpgmod.class.button.promote_to", node.displayNameComponent(), node.requiredLevel()),
                            button -> promote(node.id())
                    ).bounds(rect.x(), rect.y(), rect.width(), rect.height()).build());
            row++;
        }
    }

    private void promote(String advancementId) {
        ModNetwork.sendToServer(new ClassActionC2SPacket(ClassActionC2SPacket.Action.PROMOTE, advancementId));
        ModNetwork.sendToServer(new StatsSyncRequestC2SPacket());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyLayout(false);
        if (needsWidgetRefresh()) {
            rebuildButtons();
        }

        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        guiGraphics.fill(panelX + innerPadding, panelY + 28, panelX + panelW - innerPadding, panelY + panelH - innerPadding, PANEL_COLOR);
        guiGraphics.renderOutline(panelX, panelY, panelW, panelH, BORDER_COLOR);
        guiGraphics.drawString(this.font, this.title, panelX + titleX, panelY + titleY, TITLE_COLOR, true);

        drawClassInfo(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawClassInfo(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.common.player_not_found").getString(), panelX + infoX, panelY + infoY, TEXT_COLOR, true);
            return;
        }

        PlayerStats stats = player.getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
        if (stats == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.common.stats_not_available").getString(), panelX + infoX, panelY + infoY, TEXT_COLOR, true);
            return;
        }

        ClassAdvancement current = stats.getCurrentAdvancement();
        List<ClassAdvancement> unlocked = ClassAdvancementRegistry.nextOptions(current.id(), player.experienceLevel);

        int y = panelY + infoY;
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.class.info.base_class", stats.getSelectedClass().displayNameComponent()).getString(), panelX + infoX, y, TEXT_COLOR, true);
        y += lineStep;
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.class.info.current_advancement", current.displayNameComponent(), current.id()).getString(), panelX + infoX, y, TEXT_COLOR, true);
        y += lineStep;
        Component tierComponent = Component.translatable("screen.rpgmod.class.tier." + current.tier().name().toLowerCase());
        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.class.info.tier_level", tierComponent, player.experienceLevel).getString(), panelX + infoX, y, TEXT_COLOR, true);
        y += headerGap;
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        "screen.rpgmod.class.info.class_resource",
                        stats.getClassResourceType(),
                        round1(stats.getClassResourceCurrent()),
                        round1(stats.getClassResourceMax())
                ).getString(),
                panelX + infoX,
                y,
                0xFF9AD1F5,
                true
        );
        y += lineStep;
        int unlockedSkillCount = ClassSkillService.unlockedFor(player.experienceLevel, stats).size();
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.rpgmod.class.info.active_skills", unlockedSkillCount).getString(),
                panelX + infoX,
                y,
                0xFFB7F59E,
                true
        );
        y += headerGap;

        guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.class.section.unlocked").getString(), panelX + infoX, y, 0xFFB7F59E, true);
        y += lineStep;
        if (unlocked.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.class.none").getString(), panelX + infoX, y, TEXT_COLOR, true);
            y += lineStep;
        } else {
            for (ClassAdvancement node : unlocked) {
                guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.class.entry.unlocked", node.displayNameComponent(), node.requiredLevel()).getString(), panelX + infoX, y, TEXT_COLOR, true);
                y += lineStep;
            }
        }
    }

    private static String round1(double value) {
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }

    private boolean needsWidgetRefresh() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        PlayerStats stats = minecraft.player.getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
        if (stats == null) {
            return false;
        }

        String currentAdv = stats.getCurrentAdvancementId();
        int currentLevel = minecraft.player.experienceLevel;
        boolean changed = !currentAdv.equals(lastAdvancementId) || currentLevel != lastLevel;
        if (changed) {
            lastAdvancementId = currentAdv;
            lastLevel = currentLevel;
        }
        return changed;
    }

    private void applyLayout(boolean forceRebuild) {
        ClassOverviewLayoutConfig.Snapshot snapshot = ClassOverviewLayoutConfig.snapshot();
        ClassOverviewLayoutConfig.LayoutData layout = snapshot.layout();
        long version = snapshot.version();

        this.panelW = layout.panel.width;
        this.panelH = layout.panel.height;
        this.panelX = (this.width - panelW) / 2;
        this.panelY = (this.height - panelH) / 2;
        this.titleX = layout.panel.titleLeft;
        this.titleY = layout.panel.titleTop;
        this.innerPadding = layout.panel.innerPadding;

        this.infoX = layout.info.left;
        this.infoY = layout.info.top;
        this.lineStep = layout.info.lineStep;
        this.headerGap = layout.info.headerGap;

        this.backWidth = layout.buttons.backWidth;
        this.backHeight = layout.buttons.backHeight;
        this.backRightPadding = layout.buttons.backRightPadding;
        this.backBottomPadding = layout.buttons.backBottomPadding;
        this.promoteX = layout.buttons.promoteStartLeft;
        this.promoteY = layout.buttons.promoteStartY;
        this.promoteWidth = layout.buttons.promoteWidth;
        this.promoteHeight = layout.buttons.promoteHeight;
        this.promoteGap = layout.buttons.promoteGap;

        if (forceRebuild || appliedLayoutVersion != version) {
            rebuildButtons();
            appliedLayoutVersion = version;
        }
    }
}
