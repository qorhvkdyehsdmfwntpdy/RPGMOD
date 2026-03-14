package qorhvkdy.qorhvkdy.rpgmod.combat;

/*
 * [RPGMOD 파일 설명]
 * 역할: 전투 이벤트 발생 시 데미지 계산과 크리티컬 처리 흐름을 실행합니다.
 * 수정 예시: 크리티컬 알림 조건을 바꾸려면 이벤트 핸들러 조건을 수정합니다.
 */


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassWeaponPolicyService;
import qorhvkdy.qorhvkdy.rpgmod.classes.DefaultWeaponFamilyResolver;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDataService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatEvents {
    private static final Map<UUID, Long> WEAPON_WARNING_COOLDOWN = new ConcurrentHashMap<>();

    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        PlayerStats stats = StatsUtil.get(attacker);
        if (!canUseCurrentWeapon(attacker, stats)) {
            float reduced = event.getAmount() * 0.2f;
            event.setAmount(Math.max(0.0f, reduced));
            warnIncompatibleWeapon(attacker);
            return;
        }

        DamageCalculator.DamageResult result = DamageCalculator.calculateResult(attacker, event.getEntity(), event.getAmount(), 0.0f);
        event.setAmount(result.damage());
        CombatTelemetryService.recordHit(attacker, result.damage(), result.critical());
        boolean killedByHit = event.getEntity().getHealth() - result.damage() <= 0.0f;
        ClassResourceService.onHit(attacker, stats, result.critical(), killedByHit);

        if (result.critical()) {
            double bonusPercent = (result.critMultiplier() - 1.0) * 100.0;
            attacker.sendSystemMessage(Component.literal(
                    "\uD06C\uB9AC\uD2F0\uCEEC! +" + round1(bonusPercent) + "% (\uD655\uB960 " + round1(result.critChancePercent()) + "%)"
            ));
        }
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static boolean canUseCurrentWeapon(ServerPlayer attacker, PlayerStats stats) {
        ItemStack mainHand = attacker.getMainHandItem();
        boolean byFamily = ClassWeaponPolicyService.canUseItem(stats, mainHand, DefaultWeaponFamilyResolver.INSTANCE);
        boolean byRequirement = WeaponDataService.find(mainHand)
                .map(descriptor -> ClassWeaponPolicyService.canUseDescriptor(stats, descriptor))
                .orElse(true);
        return byFamily && byRequirement;
    }

    private static void warnIncompatibleWeapon(ServerPlayer attacker) {
        long now = System.currentTimeMillis();
        long until = WEAPON_WARNING_COOLDOWN.getOrDefault(attacker.getUUID(), 0L);
        if (now < until) {
            return;
        }
        WEAPON_WARNING_COOLDOWN.put(attacker.getUUID(), now + 2000L);
        attacker.sendSystemMessage(Component.literal("Current weapon is not compatible with your class/advancement."));
    }
}
