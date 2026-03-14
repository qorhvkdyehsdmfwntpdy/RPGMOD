package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import qorhvkdy.qorhvkdy.rpgmod.client.QuestClientState;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.QuestSyncRequestC2SPacket;

import java.util.List;

/**
 * 퀘스트 현황 전용 화면.
 */
public class QuestBoardScreen extends Screen {
    private static final int BG_COLOR = 0xD0141B26;
    private static final int PANEL_COLOR = 0xB01D2A39;
    private static final int BORDER_COLOR = 0x90CFE8FF;
    private static final int TITLE_COLOR = 0xFFFFD166;
    private static final int TEXT_COLOR = 0xFFE8F1FF;

    private final Screen parent;

    public QuestBoardScreen(Screen parent) {
        super(Component.translatable("screen.rpgmod.quest_board.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int panelW = 520;
        int panelH = 300;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.quest_board.button.refresh"), b ->
                ModNetwork.sendToServer(new QuestSyncRequestC2SPacket()))
                .bounds(x + 14, y + panelH - 28, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.quest_board.button.pin_next"), b ->
                QuestClientState.pinNextAccepted())
                .bounds(x + 112, y + panelH - 28, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.quest_board.button.pin_clear"), b ->
                QuestClientState.clearPinnedQuest())
                .bounds(x + 210, y + panelH - 28, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.common.close"), b -> onClose())
                .bounds(x + panelW - 74, y + panelH - 28, 60, 18).build());

        ModNetwork.sendToServer(new QuestSyncRequestC2SPacket());
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg, mouseX, mouseY, partialTick);
        int panelW = 520;
        int panelH = 300;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        gg.fill(x, y, x + panelW, y + panelH, BG_COLOR);
        gg.fill(x + 8, y + 24, x + panelW - 8, y + panelH - 34, PANEL_COLOR);
        gg.renderOutline(x, y, panelW, panelH, BORDER_COLOR);
        gg.drawString(this.font, this.title, x + 12, y + 8, TITLE_COLOR, false);

        QuestClientState.Snapshot state = QuestClientState.get();
        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.section.available", state.available().size()).getString(), x + 14, y + 32, 0xFF9FE6A0, false);
        drawList(gg, state.available(), x + 14, y + 44, 6);

        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.section.accepted", state.accepted().size()).getString(), x + 175, y + 32, 0xFFFFD166, false);
        drawList(gg, state.accepted(), x + 175, y + 44, 6);

        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.section.completed", state.completed().size()).getString(), x + 340, y + 32, 0xFF6EC1FF, false);
        drawList(gg, state.completed(), x + 340, y + 44, 6);

        String pinned = QuestClientState.pinnedQuestId();
        if (pinned.isBlank()) {
            pinned = inferFirstAcceptedId(state.accepted());
        }
        Progress progress = progressFromAccepted(state.accepted(), pinned);
        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.section.tracked", pinned.isBlank() ? "-" : pinned).getString(), x + 14, y + 116, 0xFFFFD166, false);
        drawProgressBar(gg, x + 14, y + 128, 220, 10, progress.current, progress.total);

        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.section.objectives").getString(), x + 14, y + 144, 0xFFFFD166, false);
        drawList(gg, objectiveLinesForQuest(state.objectiveLines(), pinned), x + 14, y + 156, 7);

        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.section.rewards").getString(), x + 270, y + 116, 0xFF9FE6A0, false);
        drawList(gg, state.rewardPreviewLines(), x + 270, y + 128, 10);

        gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.tip").getString(), x + 14, y + panelH - 42, TEXT_COLOR, false);
        super.render(gg, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics gg, List<String> list, int x, int y, int maxRows) {
        if (list.isEmpty()) {
            gg.drawString(this.font, Component.translatable("screen.rpgmod.quest_board.none").getString(), x, y, 0xFFB8C0CC, false);
            return;
        }
        int row = 0;
        for (String value : list) {
            if (row >= maxRows) {
                gg.drawString(this.font, "...", x, y + row * 10, 0xFFB8C0CC, false);
                break;
            }
            gg.drawString(this.font, "- " + value, x, y + row * 10, TEXT_COLOR, false);
            row++;
        }
    }

    private void drawProgressBar(GuiGraphics gg, int x, int y, int width, int height, int current, int total) {
        int safeTotal = Math.max(1, total);
        int safeCurrent = Math.max(0, Math.min(current, safeTotal));
        int filled = (int) ((safeCurrent / (double) safeTotal) * width);
        gg.fill(x, y, x + width, y + height, 0xFF2A3648);
        gg.fill(x, y, x + filled, y + height, 0xFF6EC1FF);
        gg.renderOutline(x, y, width, height, 0xFFB8C0CC);
        gg.drawString(this.font, safeCurrent + "/" + safeTotal, x + width + 6, y + 1, TEXT_COLOR, false);
    }

    private List<String> objectiveLinesForQuest(List<String> allLines, String questId) {
        if (questId == null || questId.isBlank()) {
            return allLines;
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        boolean inTarget = false;
        for (String line : allLines) {
            String value = line == null ? "" : line.trim();
            if (value.startsWith("[") && value.endsWith("]")) {
                inTarget = value.equalsIgnoreCase("[" + questId + "]");
                continue;
            }
            if (inTarget) {
                out.add(value);
            }
        }
        return out.isEmpty() ? allLines : out;
    }

    private String inferFirstAcceptedId(List<String> accepted) {
        if (accepted == null || accepted.isEmpty()) {
            return "";
        }
        String first = accepted.get(0);
        int idx = first.indexOf(" (");
        return idx > 0 ? first.substring(0, idx).trim() : first.trim();
    }

    private Progress progressFromAccepted(List<String> accepted, String questId) {
        if (accepted == null || accepted.isEmpty()) {
            return new Progress(0, 1);
        }
        for (String line : accepted) {
            String value = line == null ? "" : line.trim();
            if (!questId.isBlank() && !value.toLowerCase().startsWith(questId.toLowerCase())) {
                continue;
            }
            int open = value.lastIndexOf('(');
            int slash = value.lastIndexOf('/');
            int close = value.lastIndexOf(')');
            if (open >= 0 && slash > open && close > slash) {
                try {
                    int cur = Integer.parseInt(value.substring(open + 1, slash).trim());
                    int total = Integer.parseInt(value.substring(slash + 1, close).trim());
                    return new Progress(cur, total);
                } catch (NumberFormatException ignored) {
                    return new Progress(0, 1);
                }
            }
        }
        return new Progress(0, 1);
    }

    private record Progress(int current, int total) {
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
