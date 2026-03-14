package qorhvkdy.qorhvkdy.rpgmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import qorhvkdy.qorhvkdy.rpgmod.client.PermissionClientState;
import qorhvkdy.qorhvkdy.rpgmod.client.PermLayoutConfig;
import qorhvkdy.qorhvkdy.rpgmod.client.UiGridLayout;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.network.PermActionC2SPacket;
import qorhvkdy.qorhvkdy.rpgmod.network.PermissionSyncRequestC2SPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 권한/운영 허브 GUI.
 * 일반/관리자 UI는 동일한 레이아웃 기반으로, 권한에 따라 액션 노출만 달리한다.
 */
public class PermHubScreen extends Screen {
    private static final int BG_COLOR = 0xD0151D29;
    private static final int PANEL_COLOR = 0xB0213043;
    private static final int BORDER_COLOR = 0x90CFE8FF;
    private static final int TITLE_COLOR = 0xFFFFD166;
    private static final int TEXT_COLOR = 0xFFE8F1FF;

    private enum Mode {
        GENERAL,
        ADMIN_UUID
    }

    private record ActionEntry(
            int col,
            int row,
            String labelKey,
            BooleanSupplier visible,
            BooleanSupplier active,
            PermActionC2SPacket.Action action,
            Supplier<String> arg1,
            Supplier<String> arg2
    ) {
    }

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private PermLayoutConfig.LayoutData layout;
    private long appliedVersion = -1L;
    private long appliedStateHash = 0L;
    private Mode mode = Mode.GENERAL;

    private EditBox targetInput;
    private EditBox uuidInput;
    private EditBox groupInput;
    private EditBox nodeInput;
    private EditBox selectorInput;
    private EditBox tempSecondsInput;

    public PermHubScreen() {
        super(Component.translatable("screen.rpgmod.perm.title"));
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

        PermissionClientState.Snapshot state = PermissionClientState.get();
        guiGraphics.drawString(this.font, this.title, panelX + layout.panel.titleLeft, panelY + layout.panel.titleTop, TITLE_COLOR, false);
        guiGraphics.drawString(this.font,
                Component.translatable("screen.rpgmod.perm.group", state.groupId()).getString()
                        + "  " + Component.translatable("screen.rpgmod.perm.prefix", state.prefix()).getString()
                        + "  " + Component.translatable("screen.rpgmod.perm.weight", state.weight()).getString(),
                panelX + layout.buttons.left,
                panelY + 36,
                TEXT_COLOR,
                false);
        guiGraphics.drawString(this.font,
                Component.translatable(state.permissionAdmin()
                        ? "screen.rpgmod.perm.role.admin"
                        : "screen.rpgmod.perm.role.user").getString(),
                panelX + layout.buttons.left + 170,
                panelY + 36,
                state.permissionAdmin() ? 0xFFB7F59E : 0xFFB8C0CC,
                false);
        guiGraphics.drawString(this.font,
                Component.translatable(mode == Mode.GENERAL
                        ? "screen.rpgmod.perm.mode.general"
                        : "screen.rpgmod.perm.mode.admin_uuid").getString(),
                panelX + panelW - 110,
                panelY + 36,
                0xFFB8C0CC,
                false);

        if (targetInput != null && mode == Mode.GENERAL) {
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.rpgmod.perm.admin.target").getString(),
                    targetInput.getX(),
                    targetInput.getY() - 10,
                    TEXT_COLOR,
                    false);
        }
        if (uuidInput != null && mode == Mode.ADMIN_UUID) {
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.rpgmod.perm.admin.uuid").getString(),
                    uuidInput.getX(),
                    uuidInput.getY() - 10,
                    TEXT_COLOR,
                    false);
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.rpgmod.perm.admin.group").getString(),
                    groupInput.getX(),
                    groupInput.getY() - 10,
                    TEXT_COLOR,
                    false);
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.rpgmod.perm.admin.node").getString(),
                    nodeInput.getX(),
                    nodeInput.getY() - 10,
                    TEXT_COLOR,
                    false);
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.rpgmod.perm.admin.selector").getString(),
                    selectorInput.getX(),
                    selectorInput.getY() - 10,
                    TEXT_COLOR,
                    false);
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.rpgmod.perm.admin.temp_seconds").getString(),
                    tempSecondsInput.getX(),
                    tempSecondsInput.getY() - 10,
                    TEXT_COLOR,
                    false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyLayout(boolean forceRebuild) {
        PermLayoutConfig.Snapshot snapshot = PermLayoutConfig.snapshot();
        this.layout = snapshot.layout();
        long version = snapshot.version();
        long stateHash = PermissionClientState.get().hashCode();

        panelW = layout.panel.width;
        panelH = layout.panel.height;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        if (forceRebuild || version != appliedVersion || stateHash != appliedStateHash) {
            rebuildPermWidgets();
            appliedVersion = version;
            appliedStateHash = stateHash;
        }
    }

    private void rebuildPermWidgets() {
        clearWidgets();
        targetInput = null;
        uuidInput = null;
        groupInput = null;
        nodeInput = null;
        selectorInput = null;
        tempSecondsInput = null;

        PermissionClientState.Snapshot state = PermissionClientState.get();
        UiGridLayout grid = new UiGridLayout(
                panelX + layout.buttons.left,
                panelY + layout.buttons.top,
                layout.buttons.width,
                layout.buttons.height,
                layout.buttons.colGap,
                layout.buttons.rowGap
        );

        if (mode == Mode.GENERAL) {
            targetInput = new EditBox(
                    this.font,
                    panelX + layout.admin.targetInputLeft,
                    panelY + layout.admin.targetInputTop,
                    layout.admin.targetInputWidth,
                    layout.admin.targetInputHeight,
                    Component.translatable("screen.rpgmod.perm.admin.target")
            );
            targetInput.setHint(Component.translatable("screen.rpgmod.perm.admin.target_hint"));
            addRenderableWidget(targetInput);
            addClearButtonForTarget();
            buildGeneralActions(state).forEach(this::addActionButton);
        } else {
            uuidInput = new EditBox(this.font,
                    panelX + layout.admin.uuidInputLeft,
                    panelY + layout.admin.uuidInputTop,
                    layout.admin.uuidInputWidth,
                    layout.admin.targetInputHeight,
                    Component.translatable("screen.rpgmod.perm.admin.uuid"));
            uuidInput.setHint(Component.translatable("screen.rpgmod.perm.admin.uuid_hint"));
            addRenderableWidget(uuidInput);

            groupInput = new EditBox(this.font,
                    panelX + layout.admin.groupInputLeft,
                    panelY + layout.admin.groupInputTop,
                    layout.admin.groupInputWidth,
                    layout.admin.targetInputHeight,
                    Component.translatable("screen.rpgmod.perm.admin.group"));
            groupInput.setHint(Component.translatable("screen.rpgmod.perm.admin.group_hint"));
            addRenderableWidget(groupInput);

            nodeInput = new EditBox(this.font,
                    panelX + layout.admin.nodeInputLeft,
                    panelY + layout.admin.nodeInputTop,
                    layout.admin.nodeInputWidth,
                    layout.admin.targetInputHeight,
                    Component.translatable("screen.rpgmod.perm.admin.node"));
            nodeInput.setHint(Component.translatable("screen.rpgmod.perm.admin.node_hint"));
            addRenderableWidget(nodeInput);

            selectorInput = new EditBox(this.font,
                    panelX + layout.admin.selectorInputLeft,
                    panelY + layout.admin.selectorInputTop,
                    layout.admin.selectorInputWidth,
                    layout.admin.targetInputHeight,
                    Component.translatable("screen.rpgmod.perm.admin.selector"));
            selectorInput.setHint(Component.translatable("screen.rpgmod.perm.admin.selector_hint"));
            addRenderableWidget(selectorInput);

            tempSecondsInput = new EditBox(this.font,
                    panelX + layout.admin.tempSecondsInputLeft,
                    panelY + layout.admin.tempSecondsInputTop,
                    layout.admin.tempSecondsInputWidth,
                    layout.admin.targetInputHeight,
                    Component.translatable("screen.rpgmod.perm.admin.temp_seconds"));
            tempSecondsInput.setHint(Component.translatable("screen.rpgmod.perm.admin.temp_seconds_hint"));
            addRenderableWidget(tempSecondsInput);
            addClearButtonForAdminInputs();
            buildAdminUuidActions(state).forEach(this::addActionButton);
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.hub.button.refresh"),
                        b -> ModNetwork.sendToServer(new PermissionSyncRequestC2SPacket()))
                .bounds(grid.rect(0, 4).x(), grid.rect(0, 4).y(), grid.rect(0, 4).width(), grid.rect(0, 4).height())
                .build());

        if (state.permissionAdmin()) {
            addRenderableWidget(Button.builder(
                            Component.translatable(mode == Mode.GENERAL
                                    ? "screen.rpgmod.perm.button.open_uuid_tool"
                                    : "screen.rpgmod.perm.button.back_general"),
                            b -> {
                                mode = (mode == Mode.GENERAL) ? Mode.ADMIN_UUID : Mode.GENERAL;
                                rebuildPermWidgets();
                            })
                    .bounds(grid.rect(1, 4).x(), grid.rect(1, 4).y(), grid.rect(1, 4).width(), grid.rect(1, 4).height())
                    .build());
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.common.close"), b -> onClose())
                .bounds(
                        panelX + panelW - layout.buttons.closeRightPadding - layout.buttons.closeWidth,
                        panelY + panelH - layout.buttons.closeBottomPadding - layout.buttons.closeHeight,
                        layout.buttons.closeWidth,
                        layout.buttons.closeHeight
                ).build());
    }

    private List<ActionEntry> buildGeneralActions(PermissionClientState.Snapshot state) {
        List<ActionEntry> entries = new ArrayList<>();
        entries.add(new ActionEntry(0, 0, "screen.rpgmod.perm.button.party_create", state::partyManage, () -> true,
                PermActionC2SPacket.Action.PARTY_CREATE_SELF, () -> "", () -> ""));
        entries.add(new ActionEntry(1, 0, "screen.rpgmod.perm.button.party_invite", state::partyManage, this::hasTarget,
                PermActionC2SPacket.Action.PARTY_INVITE_TARGET, this::targetValue, () -> ""));

        entries.add(new ActionEntry(0, 1, "screen.rpgmod.perm.button.party_accept", state::partyManage, () -> true,
                PermActionC2SPacket.Action.PARTY_ACCEPT_INVITE, () -> "", () -> ""));
        entries.add(new ActionEntry(1, 1, "screen.rpgmod.perm.button.party_leave", state::partyManage, () -> true,
                PermActionC2SPacket.Action.PARTY_LEAVE_SELF, () -> "", () -> ""));

        entries.add(new ActionEntry(0, 2, "screen.rpgmod.perm.button.party_kick", state::partyManage, this::hasTarget,
                PermActionC2SPacket.Action.PARTY_KICK_TARGET, this::targetValue, () -> ""));
        entries.add(new ActionEntry(1, 2, "screen.rpgmod.perm.button.party_disband", state::partyManage, () -> true,
                PermActionC2SPacket.Action.PARTY_DISBAND_SELF, () -> "", () -> ""));

        entries.add(new ActionEntry(0, 3, "screen.rpgmod.perm.button.force_kick_party", state::partyForceKick, this::hasTarget,
                PermActionC2SPacket.Action.PARTY_FORCE_KICK, this::targetValue, () -> ""));
        entries.add(new ActionEntry(1, 3, "screen.rpgmod.perm.button.guild", () -> true, () -> false,
                null, () -> "", () -> ""));
        return entries;
    }

    private List<ActionEntry> buildAdminUuidActions(PermissionClientState.Snapshot state) {
        List<ActionEntry> entries = new ArrayList<>();
        entries.add(new ActionEntry(0, 0, "screen.rpgmod.perm.button.uuid_set_group", state::permissionAdmin, this::canSetGroup,
                PermActionC2SPacket.Action.PERM_SET_GROUP_UUID, this::uuidValue, this::groupValue));
        entries.add(new ActionEntry(1, 0, "screen.rpgmod.perm.button.uuid_add_node", state::permissionAdmin, this::canSetNode,
                PermActionC2SPacket.Action.PERM_ADD_NODE_UUID, this::uuidValue, this::nodeValue));
        entries.add(new ActionEntry(0, 1, "screen.rpgmod.perm.button.uuid_remove_node", state::permissionAdmin, this::canSetNode,
                PermActionC2SPacket.Action.PERM_REMOVE_NODE_UUID, this::uuidValue, this::nodeValue));
        entries.add(new ActionEntry(1, 1, "screen.rpgmod.perm.button.uuid_add_temp_node", state::permissionAdmin, this::canSetTempNode,
                PermActionC2SPacket.Action.PERM_ADD_TEMP_NODE_UUID, this::uuidValue, this::tempNodeSpec));
        entries.add(new ActionEntry(0, 2, "screen.rpgmod.perm.button.uuid_add_context_node", state::permissionAdmin, this::canSetContextNode,
                PermActionC2SPacket.Action.PERM_ADD_CONTEXT_NODE_UUID, this::uuidValue, this::contextNodeSpec));
        entries.add(new ActionEntry(1, 2, "screen.rpgmod.perm.button.uuid_remove_context_node", state::permissionAdmin, this::canSetContextNode,
                PermActionC2SPacket.Action.PERM_REMOVE_CONTEXT_NODE_UUID, this::uuidValue, this::contextNodeSpec));
        entries.add(new ActionEntry(0, 3, "screen.rpgmod.perm.button.group_set_weight", state::permissionAdmin, this::canSetGroupWeight,
                PermActionC2SPacket.Action.PERM_SET_GROUP_WEIGHT, this::groupValue, this::weightValue));
        entries.add(new ActionEntry(1, 3, "screen.rpgmod.perm.button.group_set_prefix", state::permissionAdmin, this::canSetGroupPrefix,
                PermActionC2SPacket.Action.PERM_SET_GROUP_PREFIX, this::groupValue, this::nodeValue));
        entries.add(new ActionEntry(0, 5, "screen.rpgmod.perm.button.group_set_meta", state::permissionAdmin, this::canSetGroupMeta,
                PermActionC2SPacket.Action.PERM_SET_GROUP_META, this::groupValue, this::groupMetaSpec));
        return entries;
    }

    private void addActionButton(ActionEntry entry) {
        if (!entry.visible.getAsBoolean()) {
            return;
        }
        UiGridLayout grid = new UiGridLayout(
                panelX + layout.buttons.left,
                panelY + layout.buttons.top,
                layout.buttons.width,
                layout.buttons.height,
                layout.buttons.colGap,
                layout.buttons.rowGap
        );
        UiGridLayout.Rect rect = grid.rect(entry.col, entry.row);
        Button button = Button.builder(Component.translatable(entry.labelKey), b -> {
                    if (entry.action == null) {
                        return;
                    }
                    ModNetwork.sendToServer(new PermActionC2SPacket(entry.action, entry.arg1.get(), entry.arg2.get()));
                })
                .bounds(rect.x(), rect.y(), rect.width(), rect.height())
                .build();
        button.active = entry.active.getAsBoolean();
        addRenderableWidget(button);
    }

    private boolean hasTarget() {
        return targetInput != null && !targetInput.getValue().trim().isBlank();
    }

    private String targetValue() {
        return targetInput == null ? "" : targetInput.getValue().trim();
    }

    private String uuidValue() {
        return uuidInput == null ? "" : uuidInput.getValue().trim();
    }

    private String groupValue() {
        return groupInput == null ? "" : groupInput.getValue().trim();
    }

    private String nodeValue() {
        return nodeInput == null ? "" : nodeInput.getValue().trim();
    }

    private String selectorValue() {
        return selectorInput == null ? "" : selectorInput.getValue().trim();
    }

    private boolean canSetGroup() {
        return !uuidValue().isBlank() && !groupValue().isBlank();
    }

    private boolean canSetNode() {
        return !uuidValue().isBlank() && !nodeValue().isBlank();
    }

    private boolean canSetTempNode() {
        return !uuidValue().isBlank() && !nodeValue().isBlank() && parsePositiveInt(tempSecondsValue()) > 0;
    }

    private boolean canSetContextNode() {
        return !uuidValue().isBlank() && !nodeValue().isBlank() && selectorValue().contains("=");
    }

    private boolean canSetGroupWeight() {
        return !groupValue().isBlank() && parseSignedInt(weightValue()) != null;
    }

    private boolean canSetGroupPrefix() {
        return !groupValue().isBlank() && !nodeValue().isBlank();
    }

    private boolean canSetGroupMeta() {
        return !groupValue().isBlank() && !selectorValue().isBlank() && !nodeValue().isBlank();
    }

    private String tempSecondsValue() {
        return tempSecondsInput == null ? "" : tempSecondsInput.getValue().trim();
    }

    private String weightValue() {
        return tempSecondsValue();
    }

    private String tempNodeSpec() {
        return tempSecondsValue() + "|" + nodeValue();
    }

    private String contextNodeSpec() {
        return selectorValue() + "|" + nodeValue();
    }

    private String groupMetaSpec() {
        return selectorValue() + "|" + nodeValue();
    }

    private void addClearButtonForTarget() {
        if (targetInput == null) {
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.perm.button.clear_inputs"), b -> targetInput.setValue(""))
                .bounds(
                        targetInput.getX() + targetInput.getWidth() + 6,
                        targetInput.getY(),
                        64,
                        targetInput.getHeight()
                )
                .build());
    }

    private void addClearButtonForAdminInputs() {
        if (uuidInput == null || groupInput == null || nodeInput == null || selectorInput == null || tempSecondsInput == null) {
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.perm.button.clear_inputs"), b -> {
                    uuidInput.setValue("");
                    groupInput.setValue("");
                    nodeInput.setValue("");
                    selectorInput.setValue("");
                    tempSecondsInput.setValue("");
                })
                .bounds(
                        nodeInput.getX() + nodeInput.getWidth() + 6,
                        nodeInput.getY(),
                        64,
                        nodeInput.getHeight()
                )
                .build());
    }

    private static int parsePositiveInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Integer parseSignedInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
