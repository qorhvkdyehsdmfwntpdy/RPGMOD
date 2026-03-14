package qorhvkdy.qorhvkdy.rpgmod.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

/**
 * 몹 처치 시 무기 드랍 이벤트.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponDropEvents {
    private WeaponDropEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
        var level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }

        WeaponDropService.rollDrops(killer, event.getEntity()).forEach(stack -> {
            ItemEntity dropEntity = new ItemEntity(
                    level,
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    stack
            );
            event.getDrops().add(dropEntity);
        });
    }
}
