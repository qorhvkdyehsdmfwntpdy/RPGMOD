package qorhvkdy.qorhvkdy.rpgmod.client;

/*
 * [RPGMOD 파일 설명]
 * 역할: 인게임 HUD 패널(이름/HP/SPD/EXP) 렌더링을 담당합니다.
 * 수정 예시: HUD를 핫바에 더 붙이려면 hotbarGap 값을 줄이거나 0으로 둡니다.
 */


import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsCapability;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsNumberFormat;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsHudOverlay {
    private static final Identifier HUD_LAYER_ID =
            Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":stats_hud"));
    private static final int VANILLA_HOTBAR_HEIGHT = 22;
    private static final int BG_COLOR = 0xC0181E2B;
    private static final int PANEL_EDGE = 0x90CFE8FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int EMP_COLOR = 0xFFFFD166;

    private StatsHudOverlay() {
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(HUD_LAYER_ID, StatsHudOverlay::renderHud);
    }

    private static void renderHud(GuiGraphics gg, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        PlayerStats stats = player.getCapability(StatsCapability.PLAYER_STATS).resolve().orElse(null);
        if (stats == null) {
            return;
        }

        UiLayoutConfig.LayoutData layout = UiLayoutConfig.get();
        int screenWidth = gg.guiWidth();
        int screenHeight = gg.guiHeight();
        int panelWidth = layout.hud.panelWidth;
        int panelHeight = layout.hud.panelHeight;
        int x = layout.hud.leftMargin;
        int bottomMargin = layout.hud.bottomMargin;
        int y = layout.hud.alignToHotbar
                ? screenHeight - VANILLA_HOTBAR_HEIGHT - panelHeight - layout.hud.hotbarGap
                : screenHeight - panelHeight - bottomMargin;
        x = Math.max(0, Math.min(x, Math.max(0, screenWidth - panelWidth)));
        y = Math.max(0, Math.min(y, Math.max(0, screenHeight - panelHeight)));

        gg.fill(x, y, x + panelWidth, y + panelHeight, BG_COLOR);
        gg.renderOutline(x, y, panelWidth, panelHeight, PANEL_EDGE);

        float currentHp = player.getHealth();
        float maxHp = stats.getMaxHP(player.experienceLevel);
        int expPercent = Math.round(player.experienceProgress * 100.0f);
        String line1 = player.getName().getString() + "  HP " + (int) currentHp + "/" + (int) maxHp;
        String spdText = "SPD " + format(stats.getMoveSpeedPercent()) + "%";
        String expText = "EXP Lv" + player.experienceLevel + " " + expPercent + "%";
        float fontScale = (float) layout.hud.fontScale;
        int textX = x + layout.hud.textLeftPadding;
        int lineHeight = Math.max(1, Math.round(minecraft.font.lineHeight * fontScale));
        int lineGap = Math.max(1, Math.round(1.0f * fontScale));
        int textBlockHeight = (lineHeight * 2) + lineGap;
        // Left aligned text block, vertically centered inside the HUD panel.
        int textTop = y + (panelHeight - textBlockHeight) / 2;

        drawScaledString(gg, minecraft, line1, textX, textTop, EMP_COLOR, fontScale);
        int line2Y = textTop + lineHeight + lineGap;
        drawScaledString(gg, minecraft, spdText, textX, line2Y, TEXT_COLOR, fontScale);
        int expX = textX + Math.round(minecraft.font.width(spdText + "  ") * fontScale);
        drawScaledString(gg, minecraft, expText, expX, line2Y, layout.hud.expColor, fontScale);
    }

    private static String format(double value) {
        return StatsNumberFormat.format3(value);
    }

    private static void drawScaledString(GuiGraphics gg, Minecraft minecraft, String text, int x, int y, int color, float scale) {
        gg.pose().pushMatrix();
        gg.pose().scale(scale, scale);
        int sx = Math.round(x / scale);
        int sy = Math.round(y / scale);
        gg.drawString(minecraft.font, text, sx, sy, color, false);
        gg.pose().popMatrix();
    }
}
