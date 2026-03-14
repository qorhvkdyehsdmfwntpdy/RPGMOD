package qorhvkdy.qorhvkdy.rpgmod.client.gui;

/*
 * [RPGMOD 파일 설명]
 * 역할: 스탯 분배 GUI 렌더링과 버튼 입력/툴팁 표시를 처리합니다.
 * 수정 예시: 부가 스탯 줄 간격은 derivedRowStep 값을 조정하면 즉시 바뀝니다.
 */


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.ClassSetEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.StatChangeC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.network.StatsSyncRequestC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.passive.StatPassiveSkillService;
import qorhvkdy.qorhvkdy.rpgmod.client.UiLayoutConfig;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatFormulas;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsCapability;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsNumberFormat;

public class StatsScreen extends Screen {
    private static final int BG_COLOR = 0xE0121822;
    private static final int INNER_COLOR = 0xB0243142;
    private static final int BORDER_COLOR = 0xB0D8EEFF;
    private static final int TITLE_COLOR = 0xFFFFD166;
    private static final int LABEL_COLOR = 0xFF9AD1F5;
    private static final int VALUE_COLOR = 0xFFFFFFFF;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int primaryTop;
    private int primaryRowStep;
    private int buttonRightOffset;
    private int derivedTop;
    private int derivedRowStep;
    private int derivedColumnGap;
    private long appliedLayoutVersion = -1L;

    public StatsScreen() {
        super(Component.translatable("screen.rpgmod.stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        applyLayout(true);

        // Pull fresh full data only when GUI opens.
        ModNetwork.sendToServer(new StatsSyncRequestC2SPacket());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyLayout(false);

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG_COLOR);
        guiGraphics.fill(panelX + 8, panelY + 30, panelX + panelWidth - 8, panelY + 122, INNER_COLOR);
        guiGraphics.fill(panelX + 8, panelY + 126, panelX + panelWidth - 8, panelY + panelHeight - 8, INNER_COLOR);
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, BORDER_COLOR);
        guiGraphics.drawString(this.font, this.title, panelX + 12, panelY + 9, TITLE_COLOR, true);

        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.common.player_not_found").getString(), panelX + 10, panelY + 32, VALUE_COLOR, false);
        } else {
            PlayerStats stats = player.getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
            if (stats == null) {
                guiGraphics.drawString(this.font, Component.translatable("screen.rpgmod.common.stats_not_available").getString(), panelX + 10, panelY + 32, VALUE_COLOR, false);
            } else {
                String pointsText = Component.translatable("screen.rpgmod.stats.points", stats.getAvailableStatPoints()).getString();
                int pointsX = panelX + panelWidth - 12 - this.font.width(pointsText);
                guiGraphics.drawString(this.font, pointsText, pointsX, panelY + 9, 0xFFFFD166, true);
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.rpgmod.stats.base_class", stats.getClassProfile().type().displayNameComponent()).getString(),
                        panelX + 12,
                        panelY + 22,
                        0xFFB8C0CC,
                        true
                );
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.rpgmod.stats.advancement", stats.getCurrentAdvancement().displayNameComponent()).getString(),
                        panelX + 12,
                        panelY + 32,
                        0xFFB8C0CC,
                        true
                );

                int rowY = panelY + primaryTop;
                for (StatType type : StatType.values()) {
                    drawPrimaryRow(guiGraphics, stats, type, rowY);
                    rowY += primaryRowStep;
                }

                int derivedY = panelY + derivedTop;
                drawDerivedPairCentered(
                        guiGraphics, derivedY,
                        Component.translatable("screen.rpgmod.stats.derived.move_speed", format(stats.getMoveSpeedPercent())).getString(),
                        Component.translatable("screen.rpgmod.stats.derived.attack_speed", format(stats.getAttackSpeedPercent())).getString(),
                        VALUE_COLOR
                );
                drawDerivedPairCentered(
                        guiGraphics, derivedY + derivedRowStep,
                        Component.translatable("screen.rpgmod.stats.derived.attack_power", format(stats.getAttackPower())).getString(),
                        Component.translatable("screen.rpgmod.stats.derived.magic_power", format(stats.getMagicPower())).getString(),
                        VALUE_COLOR
                );
                drawDerivedPairCentered(
                        guiGraphics, derivedY + (derivedRowStep * 2),
                        Component.translatable("screen.rpgmod.stats.derived.hp", stats.getMaxHP(player.experienceLevel)).getString(),
                        Component.translatable("screen.rpgmod.stats.derived.mp", stats.getMaxMP(player.experienceLevel)).getString(),
                        VALUE_COLOR
                );
                drawDerivedPairCentered(
                        guiGraphics, derivedY + (derivedRowStep * 3),
                        Component.translatable("screen.rpgmod.stats.derived.crit_damage", format(stats.getCritDamage())).getString(),
                        Component.translatable("screen.rpgmod.stats.derived.crit_chance", format(stats.getCritChance())).getString(),
                        VALUE_COLOR
                );
                drawDerivedPairCentered(
                        guiGraphics, derivedY + (derivedRowStep * 4),
                        Component.translatable("screen.rpgmod.stats.derived.defense", format(stats.getDefense())).getString(),
                        Component.translatable("screen.rpgmod.stats.derived.hp_regen", format(stats.getHpRegenPercent())).getString(),
                        VALUE_COLOR
                );
                drawClassEnhancementSummary(guiGraphics, player, stats);
                drawPassiveSummary(guiGraphics, stats);

                super.render(guiGraphics, mouseX, mouseY, partialTick);

                rowY = panelY + primaryTop;
                for (StatType type : StatType.values()) {
                    drawPreviewTooltip(guiGraphics, mouseX, mouseY, stats, type, rowY, player.experienceLevel);
                    rowY += primaryRowStep;
                }
                return;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawPrimaryRow(GuiGraphics gg, PlayerStats stats, StatType type, int y) {
        gg.drawString(this.font, type.displayName(), panelX + 12, y, LABEL_COLOR, true);
        gg.drawString(this.font, String.valueOf(stats.get(type)), panelX + 88, y, VALUE_COLOR, true);

        StatFormulas.Rule rule = StatFormulas.rule(type);
        String cap = "S" + rule.softCap() + "/H" + rule.hardCap();
        gg.drawString(this.font, cap, panelX + 130, y, 0xFFB8C0CC, true);
    }

    private void drawDerivedPairCentered(GuiGraphics gg, int y, String leftText, String rightText, int color) {
        int leftWidth = this.font.width(leftText);
        int rightWidth = this.font.width(rightText);
        int totalWidth = leftWidth + derivedColumnGap + rightWidth;
        int startX = panelX + (panelWidth - totalWidth) / 2;

        gg.drawString(this.font, leftText, startX, y, color, true);
        gg.drawString(this.font, rightText, startX + leftWidth + derivedColumnGap, y, color, true);
    }

    private void drawPreviewTooltip(GuiGraphics gg, int mouseX, int mouseY, PlayerStats stats, StatType type, int rowY, int level) {
        int rowX = panelX + 10;
        int rowW = panelWidth - 76;
        int rowH = 14;
        if (mouseX < rowX || mouseX > rowX + rowW || mouseY < rowY || mouseY > rowY + rowH) {
            return;
        }

        String preview = StatFormulas.nextPointPreview(stats, type, level);
        int textWidth = this.font.width(preview);
        int textHeight = this.font.lineHeight;
        int tx = mouseX + 8;
        int ty = mouseY + 8;
        gg.fill(tx - 3, ty - 3, tx + textWidth + 3, ty + textHeight + 2, 0xD0101010);
        gg.renderOutline(tx - 3, ty - 3, textWidth + 6, textHeight + 5, 0x90FFFFFF);
        gg.drawString(this.font, preview, tx, ty, VALUE_COLOR, false);
    }

    private void addStatButtons(int x, int y, StatType statType) {
        addRenderableWidget(Button.builder(Component.literal("+"), button -> changeStat(statType, 1))
                .bounds(x, y - 4, 24, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("-"), button -> changeStat(statType, -1))
                .bounds(x + 28, y - 4, 24, 18)
                .build());
    }

    private void changeStat(StatType statType, int delta) {
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            return;
        }
        ModNetwork.sendToServer(new StatChangeC2SPacket(statType, delta));
    }

    private void applyLayout(boolean forceRebuild) {
        UiLayoutConfig.Snapshot snapshot = UiLayoutConfig.snapshot();
        UiLayoutConfig.LayoutData layout = snapshot.layout();
        long version = snapshot.version();

        this.panelWidth = layout.gui.panelWidth;
        this.panelHeight = layout.gui.panelHeight;
        this.primaryTop = layout.gui.primaryTop;
        this.primaryRowStep = layout.gui.primaryRowStep;
        this.buttonRightOffset = layout.gui.buttonRightOffset;
        this.derivedTop = layout.gui.derivedTop;
        this.derivedRowStep = layout.gui.derivedRowStep;
        this.derivedColumnGap = layout.gui.derivedColumnGap;

        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;

        if (forceRebuild || version != appliedLayoutVersion) {
            clearWidgets();
            for (int i = 0; i < StatType.values().length; i++) {
                int rowY = panelY + primaryTop + (i * primaryRowStep);
                addStatButtons(panelX + panelWidth - buttonRightOffset, rowY, StatType.values()[i]);
            }
            addHubButton();
            appliedLayoutVersion = version;
        }
    }

    private void addHubButton() {
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.stats.button.hub"), button -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new RpgHubScreen());
                    }
                })
                .bounds(panelX + panelWidth - 56, panelY + panelHeight - 24, 42, 16)
                .build());
    }

    private void drawPassiveSummary(GuiGraphics gg, PlayerStats stats) {
        // 한글 주석: 하단 2줄만 사용해 정보량을 제한하고 렌더 비용/가독성 균형을 맞춘다.
        String unlocked = String.join(", ", StatPassiveSkillService.unlockedSkillIds(stats));
        if (unlocked.isBlank()) {
            unlocked = "-";
        }
        String unlockedText = Component.translatable("screen.rpgmod.stats.passive.unlocked", unlocked).getString();
        gg.drawString(this.font, unlockedText, panelX + 12, panelY + panelHeight - 34, 0xFFB7F59E, true);

        String slotsText = Component.translatable("screen.rpgmod.stats.passive.slots", stats.getPassiveSlots()).getString();
        gg.drawString(this.font, slotsText, panelX + 12, panelY + panelHeight - 22, 0xFFB8C0CC, true);
    }

    private void drawClassEnhancementSummary(GuiGraphics gg, Player player, PlayerStats stats) {
        // 한글 주석: 하단 요약 영역에 클래스 리소스/스킬/세트효과 핵심만 압축해서 표시한다.
        String resourceText = Component.translatable(
                "screen.rpgmod.stats.class_resource",
                stats.getClassResourceType(),
                format(stats.getClassResourceCurrent()),
                format(stats.getClassResourceMax())
        ).getString();
        gg.drawString(this.font, resourceText, panelX + 12, panelY + panelHeight - 58, 0xFF9AD1F5, true);

        int unlockedCount = ClassSkillService.unlockedFor(player.experienceLevel, stats).size();
        String skillText = Component.translatable("screen.rpgmod.stats.active_skills", unlockedCount).getString();
        gg.drawString(this.font, skillText, panelX + 12, panelY + panelHeight - 46, 0xFFB7F59E, true);

        int activeSetCount = ClassSetEffectService.activeDescriptors(player, stats).size();
        String setText = Component.translatable("screen.rpgmod.stats.set_bonus", activeSetCount).getString();
        gg.drawString(this.font, setText, panelX + 220, panelY + panelHeight - 46, 0xFFFFD166, true);
    }

    private static String format(double value) {
        return StatsNumberFormat.format3(value);
    }
}
