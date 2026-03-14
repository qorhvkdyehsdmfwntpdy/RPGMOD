package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

/**
 * Extension hook for class lifecycle events.
 */
public interface ClassLifecycleHook {
    default void onClassChanged(ServerPlayer player, PlayerStats stats, PlayerClassType before, PlayerClassType after, ClassAdvancement beforeAdv, ClassAdvancement afterAdv) {
    }

    default void onPromotion(ServerPlayer player, PlayerStats stats, ClassAdvancement before, ClassAdvancement after) {
    }
}

