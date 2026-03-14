package qorhvkdy.qorhvkdy.rpgmod.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.combat.profile.CombatProfileService;

/**
 * 무기 데이터 기반 툴팁 렌더러.
 * 기본 목표: 디버깅/밸런싱 확인용으로 핵심 정보만 간단히 노출한다.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponTooltipEvents {
    private WeaponTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        WeaponDataService.find(event.getItemStack()).ifPresent(descriptor -> {
            // 한글 주석: 헤더 라인은 무기 데이터가 붙은 아이템인지 즉시 구분하기 위한 표식이다.
            event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.header").withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.grade", descriptor.grade()).withStyle(ChatFormatting.YELLOW));
            event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.rarity", descriptor.rarity()).withStyle(ChatFormatting.AQUA));

            if (!descriptor.requiredClass().isBlank()) {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.required_class", descriptor.requiredClass()).withStyle(ChatFormatting.GRAY));
            }
            if (!descriptor.requiredAdvancement().isBlank()) {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.required_adv", descriptor.requiredAdvancement()).withStyle(ChatFormatting.GRAY));
            }
            if (descriptor.socket().maxSockets() > 0) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.rpgmod.weapon.socket_template",
                        descriptor.socket().minSockets(),
                        descriptor.socket().maxSockets()
                ).withStyle(ChatFormatting.DARK_AQUA));
            }
            for (WeaponOptionDescriptor option : descriptor.options()) {
                String optionText = option.label();
                if (optionText == null || optionText.isBlank()) {
                    optionText = option.mode() + " " + option.stat() + " " + option.value();
                }
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.option", optionText).withStyle(ChatFormatting.GREEN));
            }
            WeaponDropService.RollInfo rollInfo = WeaponDropService.readRollInfo(event.getItemStack());
            if (!rollInfo.affixDisplay().isBlank()) {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.affix", rollInfo.affixDisplay()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (!rollInfo.optionText().isBlank()) {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.rolled_option", rollInfo.optionText()).withStyle(ChatFormatting.GREEN));
            }
            if (rollInfo.socketMax() > 0) {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.socket_roll", rollInfo.sockets(), rollInfo.socketMax()).withStyle(ChatFormatting.DARK_AQUA));
            }
            for (String method : descriptor.obtainMethods()) {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.obtain", method).withStyle(ChatFormatting.DARK_GRAY));
            }
            CombatProfileService.find(event.getItemStack()).ifPresent(profile -> {
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.combo_window", profile.comboWindowTicks()).withStyle(ChatFormatting.BLUE));
                event.getToolTip().add(Component.translatable("tooltip.rpgmod.weapon.recovery", profile.recoveryTicks()).withStyle(ChatFormatting.BLUE));
            });
        });
    }
}
