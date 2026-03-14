package qorhvkdy.qorhvkdy.rpgmod.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;
import qorhvkdy.qorhvkdy.rpgmod.skill.passive.PassiveSkillProgramService;
import qorhvkdy.qorhvkdy.rpgmod.skill.runtime.SkillRuntimeService;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

/**
 * Skill/passive runtime events.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillMechanicEvents {
    private SkillMechanicEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            SkillRuntimeService.trigger(attacker, "on_hit");
            PassiveSkillProgramService.triggerHook(attacker, "on_hit", StatsUtil.get(attacker));
        }
        if (event.getEntity() instanceof ServerPlayer defender) {
            SkillRuntimeService.trigger(defender, "on_hurt");
            PassiveSkillProgramService.triggerHook(defender, "on_hurt", StatsUtil.get(defender));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        if (killed.getKillCredit() instanceof ServerPlayer killer) {
            SkillRuntimeService.trigger(killer, "on_kill");
            PassiveSkillProgramService.triggerHook(killer, "on_kill", StatsUtil.get(killer));
        }
    }

    @SubscribeEvent
    public static void onServerPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) {
            return;
        }
        SkillRuntimeService.tickPlayer(player, player.level().getGameTime());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PassiveSkillProgramService.invalidate(player);
        }
    }

    @SubscribeEvent
    public static void onEquipChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PassiveSkillProgramService.invalidate(player);
        }
    }
}
