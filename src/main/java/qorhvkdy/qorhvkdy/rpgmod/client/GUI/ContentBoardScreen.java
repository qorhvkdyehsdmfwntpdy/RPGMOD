package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import qorhvkdy.qorhvkdy.rpgmod.client.PermissionClientState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 콘텐츠 상태를 한눈에 보여주는 대시보드.
 */
public class ContentBoardScreen extends Screen {
    private final Screen parent;
    private final List<String> lines = new ArrayList<>();

    public ContentBoardScreen(Screen parent) {
        super(Component.translatable("screen.rpgmod.content_board.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int panelW = 380;
        int panelH = 250;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.content_board.button.refresh"), b -> rebuildLines())
                .bounds(x + 14, y + panelH - 28, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.content_board.button.keymap"), b -> this.minecraft.setScreen(new KeymapCenterScreen(this)))
                .bounds(x + 110, y + panelH - 28, 120, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.content_board.button.quest"), b -> this.minecraft.setScreen(new QuestBoardScreen(this)))
                .bounds(x + 236, y + panelH - 28, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.common.close"), b -> onClose())
                .bounds(x + panelW - 60 - 8, y + panelH - 28, 60, 18).build());
        rebuildLines();
    }

    private void rebuildLines() {
        lines.clear();
        lines.add(line("Quest DSL", exists("quests.json")));
        lines.add(line("Quest Links", exists("quest-links.json")));
        lines.add(line("Boss Chains", exists("boss-chains.json")));
        lines.add(line("Quest Zones", exists("quest-zones.json")));
        lines.add(line("Skill Mechanics", exists("skill-mechanics.json")));
        lines.add(line("XP Source Table", exists("proficiency-sources.json")));
        lines.add(line("NPC Dialogue", exists("npc-dialogues.json")));
        lines.add(line("Spell School", exists("spell-schools.json")));
        lines.add(line("Combat Profile", exists("combat-profiles.json")));
        lines.add(line("Permission Context Priority", exists("permission-context-priority.json")));

        PermissionClientState.Snapshot p = PermissionClientState.get();
        lines.add(" ");
        lines.add("Permission Snapshot");
        lines.add("- group: " + p.groupId() + ", weight: " + p.weight());
        lines.add("- classAdmin=" + p.classAdmin() + ", permAdmin=" + p.permissionAdmin());
        lines.add("- partyManage=" + p.partyManage() + ", debugAdmin=" + p.debugAdmin());

        lines.add(" ");
        lines.add("Quick Commands");
        lines.add("- /rpgdev doctor");
        lines.add("- /rpgquest list");
        lines.add("- /rpgquest status");
        lines.add("- /rpgspell affinity");
        lines.add("- /rpgskill trigger on_hit");
        lines.add("- /rpgnpc talk trainer_warrior");
    }

    private boolean exists(String file) {
        return Files.exists(Path.of("config", "rpgmod", file));
    }

    private String line(String label, boolean ok) {
        return "- " + label + ": " + (ok ? "READY" : "MISSING");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x70000000);
        int panelW = 380;
        int panelH = 250;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        guiGraphics.fill(x, y, x + panelW, y + panelH, 0xD0141B26);
        guiGraphics.fill(x + 8, y + 24, x + panelW - 8, y + panelH - 34, 0xB01D2A39);
        guiGraphics.renderOutline(x, y, panelW, panelH, 0x90CFE8FF);
        guiGraphics.drawString(this.font, this.title, x + 12, y + 8, 0xFFFFD166, false);

        int dy = y + 30;
        for (String line : lines) {
            guiGraphics.drawString(this.font, line, x + 14, dy, 0xFFE8F1FF, false);
            dy += 10;
            if (dy > y + panelH - 40) {
                break;
            }
        }
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
